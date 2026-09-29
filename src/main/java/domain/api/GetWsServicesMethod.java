package domain.api;

import io.restassured.http.Method;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;

public class GetWsServicesMethod extends ApiBaseMethod {

    public record WsService(String name, String deploymentName, String status, String restfulUrl) {
    }

    public GetWsServicesMethod(GenericContainer<?> wsContainer, int port) {
        super("http://" + wsContainer.getHost() + ":" + wsContainer.getMappedPort(port), "/admin/services");
    }

    public List<String> getServiceNames() {
        return readServices().jsonPath().getList("name");
    }

    public List<WsService> getServices() {
        List<Map<String, Object>> services = readServices().jsonPath().getList("$");
        return services.stream()
                .map(service -> new WsService((String) service.get("name"), (String) service.get("deploymentName"),
                        (String) service.get("status"),
                        service.get("urls") instanceof Map<?, ?> urls ? (String) urls.get("RESTFUL") : null))
                .toList();
    }

    private Response readServices() {
        RequestSpecification request = io.restassured.RestAssured.given()
                .redirects().follow(false)
                .accept(ContentType.JSON);
        Response response = callApi(Method.GET, request, true);
        String body = response.getBody().asString();
        String trimmedBody = body.stripLeading();
        if (response.statusCode() != 200 || (!trimmedBody.startsWith("[") && !trimmedBody.startsWith("{"))) {
            String preview = body.length() > 500 ? body.substring(0, 500) + "..." : body;
            throw new IllegalStateException(String.format(
                    "GET /admin/services expected JSON from Rule Services but got HTTP %s, content-type '%s', body: %s",
                    response.statusCode(), response.contentType(), preview));
        }
        LOGGER.debug("GET /admin/services → HTTP {} body: {}", response.statusCode(), body);
        return response;
    }
}
