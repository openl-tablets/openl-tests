package helpers.service;

import helpers.utils.LfsPointer;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GitHubLfsService {

    private static final Logger LOGGER = LogManager.getLogger(GitHubLfsService.class);

    private static final String DEFAULT_REPOSITORY = "openl-tablets/openl-tests";
    private static final String LFS_MEDIA_TYPE = "application/vnd.git-lfs+json";
    private static final String TOKEN_USER = "x-access-token";
    private static final RestAssuredConfig TIMEOUTS = RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
            .setParam("http.connection.timeout", 30_000)
            .setParam("http.socket.timeout", 120_000));

    private final String repository;
    private final String token;

    public GitHubLfsService() {
        String fromEnvironment = System.getenv("GITHUB_REPOSITORY");
        repository = fromEnvironment == null || fromEnvironment.isBlank() ? DEFAULT_REPOSITORY : fromEnvironment;
        token = resolveToken();
    }

    public String lfsUrl() {
        return "https://github.com/" + repository + ".git/info/lfs";
    }

    public String token() {
        return token;
    }

    public LfsPointer upload(byte[] content) {
        LfsPointer pointer = LfsPointer.of(content);
        JsonPath object = batch("upload", pointer);
        requireNoError(object, "upload", pointer);
        Map<String, Object> upload = object.getMap("actions.upload");
        if (upload == null) {
            LOGGER.info("GitHub LFS of {} already holds object {}", repository, pointer.oid());
            return pointer;
        }
        Response uploaded = RestAssured.given().config(TIMEOUTS)
                .urlEncodingEnabled(false)
                .headers(headersOf(upload))
                .header("Content-Type", "application/octet-stream")
                .body(content)
                .put((String) upload.get("href"));
        requireSuccess(uploaded, "upload object " + pointer.oid());
        Map<String, Object> verify = object.getMap("actions.verify");
        if (verify != null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("oid", pointer.oid());
            body.put("size", pointer.size());
            Response verified = RestAssured.given().config(TIMEOUTS)
                    .urlEncodingEnabled(false)
                    .headers(headersOf(verify))
                    .contentType(LFS_MEDIA_TYPE)
                    .accept(LFS_MEDIA_TYPE)
                    .body(body)
                    .post((String) verify.get("href"));
            requireSuccess(verified, "verify object " + pointer.oid());
        }
        LOGGER.info("Uploaded object {} ({} bytes) to GitHub LFS of {}", pointer.oid(), pointer.size(), repository);
        return pointer;
    }

    public byte[] download(LfsPointer pointer) {
        JsonPath object = batch("download", pointer);
        requireNoError(object, "download", pointer);
        Map<String, Object> download = object.getMap("actions.download");
        if (download == null) {
            throw new IllegalStateException("GitHub LFS of " + repository + " offers no download of " + pointer.oid());
        }
        Response downloaded = RestAssured.given().config(TIMEOUTS)
                .urlEncodingEnabled(false)
                .headers(headersOf(download))
                .get((String) download.get("href"));
        requireSuccess(downloaded, "download object " + pointer.oid());
        return downloaded.asByteArray();
    }

    private JsonPath batch(String operation, LfsPointer pointer) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("oid", pointer.oid());
        object.put("size", pointer.size());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operation", operation);
        body.put("transfers", List.of("basic"));
        body.put("objects", List.of(object));
        Response response = authorized()
                .contentType(LFS_MEDIA_TYPE)
                .accept(LFS_MEDIA_TYPE)
                .body(body)
                .post(lfsUrl() + "/objects/batch");
        requireSuccess(response, operation + " batch for " + pointer.oid());
        return new JsonPath(response.asString()).setRootPath("objects[0]");
    }

    private RequestSpecification authorized() {
        return RestAssured.given().config(TIMEOUTS).auth().preemptive().basic(TOKEN_USER, token);
    }

    private void requireNoError(JsonPath object, String operation, LfsPointer pointer) {
        Map<String, Object> error = object.getMap("error");
        if (error != null) {
            throw new IllegalStateException("GitHub LFS of " + repository + " refused to " + operation + " "
                    + pointer.oid() + ": " + error);
        }
    }

    private void requireSuccess(Response response, String action) {
        if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
            throw new IllegalStateException("GitHub LFS of " + repository + " could not " + action + ": HTTP "
                    + response.getStatusCode() + " " + response.asString());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> headersOf(Map<String, Object> action) {
        Object headers = action.get("header");
        return headers == null ? Map.of() : (Map<String, String>) headers;
    }

    private static String resolveToken() {
        String fromEnvironment = System.getenv("GITHUB_TOKEN");
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        try {
            Process gh = new ProcessBuilder("gh", "auth", "token")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!gh.waitFor(30, TimeUnit.SECONDS)) {
                gh.destroyForcibly();
            } else if (gh.exitValue() == 0) {
                String output = new String(gh.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!output.isEmpty()) {
                    return output;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot run 'gh auth token': {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new IllegalStateException("A GitHub token is required: set GITHUB_TOKEN or log in with 'gh auth login'");
    }
}
