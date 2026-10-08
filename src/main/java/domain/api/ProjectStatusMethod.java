package domain.api;

import io.restassured.http.Method;
import io.restassured.response.Response;

public class ProjectStatusMethod extends AuthorizedApiMethod {

    public ProjectStatusMethod() {
        super("/rest/projects");
    }

    public Response getStatus(String projectId) {
        return getStatus(projectId, true);
    }

    public Response getStatus(String projectId, boolean withLogs) {
        return callApi(Method.GET, authorizedRequest().queryParam("include", "status"), fullApiUrl + "/" + projectId, withLogs);
    }
}
