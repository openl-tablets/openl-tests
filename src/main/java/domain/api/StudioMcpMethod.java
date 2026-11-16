package domain.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.List;
import java.util.Map;

public class StudioMcpMethod extends ApiBaseMethod {

    private static final String PROTOCOL_VERSION = "2026-07-28";
    private static final String META_PREFIX = "io.modelcontextprotocol/";
    private static final String DISCOVER = "server/discover";
    private static final Map<String, Object> META = Map.of(
            META_PREFIX + "protocolVersion", PROTOCOL_VERSION,
            META_PREFIX + "clientCapabilities", Map.of(),
            META_PREFIX + "clientInfo", Map.of("name", "openl-tests", "version", "1.0"));

    public StudioMcpMethod() {
        super("/mcp");
    }

    public int getDiscoverStatusWithoutAuthorization() {
        return call(RestAssured.given(), DISCOVER).getStatusCode();
    }

    public Map<String, Object> getServerInfo(String personalAccessToken) {
        return call(withToken(personalAccessToken), DISCOVER).then().statusCode(200).extract()
                .jsonPath().getMap("result._meta.'" + META_PREFIX + "serverInfo'");
    }

    public List<String> getToolNames(String personalAccessToken) {
        return call(withToken(personalAccessToken), "tools/list").then().statusCode(200).extract()
                .jsonPath().getList("result.tools.name");
    }

    private RequestSpecification withToken(String personalAccessToken) {
        return RestAssured.given().header("Authorization", "Token " + personalAccessToken);
    }

    private Response call(RequestSpecification specification, String method) {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", method,
                "params", Map.of("_meta", META));
        specification.contentType(ContentType.JSON)
                .accept("application/json, text/event-stream")
                .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                .header("Mcp-Method", method)
                .body(body);
        return callApi(Method.POST, specification, false);
    }
}
