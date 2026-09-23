package helpers.service;

import configuration.network.NetworkPool;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitContainerService {

    private static final Logger LOGGER = LogManager.getLogger(GitContainerService.class);

    private static final int HTTP_PORT = 3000;
    private static final String REPO_NAME = "design";
    private static final String FIXTURE_RESOURCE = "/git_daemon_repo";
    private static final String BRANCH = "master";
    private static final String OWNER = "openl";
    private static final String OWNER_PASSWORD = "openl-git-pass";

    private static final String IMAGE = "gitea/gitea:1.27.3@sha256:87a67ee09d3ae0d1df5fda5dcda3e2a1f9236a45b0a59025d6e00e46adc43bef";
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

    private final String alias;
    private final String repoName;
    private final String branch;
    private final Path fixtureDir;
    private final List<String> lfsPatterns = new ArrayList<>();
    private final Map<String, byte[]> replacedFiles = new LinkedHashMap<>();
    private String externalLfsUrl;
    private String ownerPassword = OWNER_PASSWORD;
    private GenericContainer<?> container;

    public GitContainerService(String alias) {
        this(alias, REPO_NAME, BRANCH, FIXTURE_RESOURCE);
    }

    public GitContainerService(String alias, String repoName, String branch, String fixtureResource) {
        this.alias = alias;
        this.repoName = repoName;
        this.branch = branch;
        this.fixtureDir = resolveFixtureDir(fixtureResource);
    }

    public GitContainerService withLfsTracking(String pattern) {
        lfsPatterns.add(pattern);
        return this;
    }

    public GitContainerService withExternalLfs(String lfsUrl) {
        externalLfsUrl = lfsUrl;
        return this;
    }

    public GitContainerService withOwnerPassword(String password) {
        ownerPassword = password;
        return this;
    }

    public GitContainerService withFile(String path, byte[] content) {
        replacedFiles.put(path, content);
        return this;
    }

    public void start() {
        Network network = NetworkPool.getNetwork();
        if (network == null) {
            network = Network.newNetwork();
            NetworkPool.setNetwork(network);
        }
        container = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withExposedPorts(HTTP_PORT)
                .withEnv("GITEA__security__INSTALL_LOCK", "true")
                .withEnv("GITEA__database__DB_TYPE", "sqlite3")
                .withEnv("GITEA__server__HTTP_PORT", String.valueOf(HTTP_PORT))
                .withEnv("GITEA__server__ROOT_URL", inNetworkBaseUrl() + "/")
                .withEnv("GITEA__server__DISABLE_SSH", "true")
                .withEnv("GITEA__server__LFS_START_SERVER", String.valueOf(externalLfsUrl == null))
                .withEnv("GITEA__service__DISABLE_REGISTRATION", "true")
                .withEnv("GITEA__service__REQUIRE_SIGNIN_VIEW", "false")
                .waitingFor(Wait.forHttp("/api/healthz").forPort(HTTP_PORT).forStatusCode(200)
                        .withStartupTimeout(STARTUP_TIMEOUT));
        LOGGER.info("Starting Gitea ({}) on network alias '{}'", IMAGE, alias);
        container.start();
        createOwner();
        if (!OWNER_PASSWORD.equals(ownerPassword)) {
            changeOwnerPassword();
        }
        createRepository();
        commitFixture();
        LOGGER.info("Gitea ready. Host URL: {} | in-network URL: {}", getHostUrl(), getInNetworkUrl());
    }

    public String getHostUrl() {
        return hostBaseUrl() + "/" + OWNER + "/" + repoName + ".git";
    }

    public String getInNetworkUrl() {
        return inNetworkBaseUrl() + "/" + OWNER + "/" + repoName + ".git";
    }

    public GitRemote asRemote() {
        return new GitRemote(getHostUrl(), OWNER, ownerPassword);
    }

    public byte[] readCommittedFile(String path) {
        return readFile("raw", path);
    }

    public byte[] readLfsContent(String path) {
        if (externalLfsUrl != null) {
            throw new IllegalStateException("LFS objects of " + repoName + " are stored at " + externalLfsUrl + ", not in Gitea");
        }
        return readFile("media", path);
    }

    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private void createOwner() {
        runGitea("create the Gitea user " + OWNER, "admin", "user", "create",
                "--username", OWNER, "--password=" + OWNER_PASSWORD, "--email", OWNER + "@example.com",
                "--admin", "--must-change-password=false");
    }

    private void changeOwnerPassword() {
        runGitea("change the password of the Gitea user " + OWNER, "admin", "user", "change-password",
                "--username", OWNER, "--password=" + ownerPassword, "--must-change-password=false");
    }

    private void runGitea(String action, String... arguments) {
        String[] command = new String[arguments.length + 1];
        command[0] = "gitea";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Container.ExecResult result;
        try {
            result = container.execInContainerWithUser("git", command);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot " + action, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while trying to " + action, e);
        }
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Cannot " + action + ": "
                    + (result.getStdout() + result.getStderr()).replace(ownerPassword, "***"));
        }
    }

    private void createRepository() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", repoName);
        body.put("default_branch", branch);
        body.put("private", false);
        Response response = ownerRequest().body(body).post(hostBaseUrl() + "/api/v1/user/repos");
        requireStatus(response, 201, "create the repository " + repoName);
    }

    private void commitFixture() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("branch", branch);
        body.put("message", "Initial commit");
        body.put("files", fixtureFiles());
        Response response = ownerRequest().body(body)
                .post(hostBaseUrl() + "/api/v1/repos/" + OWNER + "/" + repoName + "/contents");
        requireStatus(response, 201, "commit the fixture " + fixtureDir + " to " + repoName);
    }

    private List<Map<String, String>> fixtureFiles() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        if (!lfsPatterns.isEmpty()) {
            files.put(".gitattributes", lfsAttributes());
        }
        if (externalLfsUrl != null) {
            files.put(".lfsconfig", ("[lfs]\n\turl = " + externalLfsUrl + "\n").getBytes(StandardCharsets.UTF_8));
        }
        try (Stream<Path> paths = Files.walk(fixtureDir)) {
            paths.filter(Files::isRegularFile).sorted().forEach(file ->
                    files.put(fixtureDir.relativize(file).toString().replace('\\', '/'), readFixtureFile(file)));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the fixture " + fixtureDir, e);
        }
        files.putAll(replacedFiles);
        return files.entrySet().stream().map(file -> createFileOperation(file.getKey(), file.getValue())).toList();
    }

    private byte[] lfsAttributes() {
        return lfsPatterns.stream()
                .map(pattern -> pattern + " filter=lfs diff=lfs merge=lfs -text\n")
                .collect(Collectors.joining())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] readFixtureFile(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the fixture file " + file, e);
        }
    }

    private static Map<String, String> createFileOperation(String path, byte[] content) {
        Map<String, String> operation = new LinkedHashMap<>();
        operation.put("operation", "create");
        operation.put("path", path);
        operation.put("content", Base64.getEncoder().encodeToString(content));
        return operation;
    }

    private byte[] readFile(String endpoint, String path) {
        Response response = ownerRequest().queryParam("ref", branch)
                .get(hostBaseUrl() + "/api/v1/repos/" + OWNER + "/" + repoName + "/" + endpoint + "/" + path);
        requireStatus(response, 200, "read " + path + " from " + repoName);
        return response.asByteArray();
    }

    private RequestSpecification ownerRequest() {
        return RestAssured.given()
                .contentType("application/json")
                .auth().preemptive().basic(OWNER, ownerPassword);
    }

    private static void requireStatus(Response response, int expected, String action) {
        if (response.getStatusCode() != expected) {
            throw new IllegalStateException("Gitea could not " + action + ": HTTP " + response.getStatusCode()
                    + " " + response.asString());
        }
    }

    private String hostBaseUrl() {
        return "http://" + container.getHost() + ":" + container.getMappedPort(HTTP_PORT);
    }

    private String inNetworkBaseUrl() {
        return "http://" + alias + ":" + HTTP_PORT;
    }

    private static Path resolveFixtureDir(String fixtureResource) {
        URL url = GitContainerService.class.getResource(fixtureResource);
        if (url == null) {
            throw new IllegalStateException("Fixture resource not found on classpath: " + fixtureResource);
        }
        try {
            return Paths.get(url.toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve fixture directory for " + fixtureResource, e);
        }
    }
}
