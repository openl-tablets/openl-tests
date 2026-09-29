package domain.api;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.testcontainers.containers.GenericContainer;

public class ServiceOpenApiMethod extends ApiBaseMethod {

    public ServiceOpenApiMethod(GenericContainer<?> wsContainer, int port, String restfulUrl, String format) {
        super("http://" + wsContainer.getHost() + ":" + wsContainer.getMappedPort(port), "/" + restfulUrl + "/openapi." + format);
    }

    public Response get() {
        return callApi(Method.GET, RestAssured.given().urlEncodingEnabled(false).redirects().follow(false), false);
    }
}
