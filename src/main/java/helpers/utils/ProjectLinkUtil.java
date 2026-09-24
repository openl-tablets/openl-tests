package helpers.utils;

import configuration.driver.DriverPool;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public final class ProjectLinkUtil {

    private static final String PROJECTS_PATH = "/projects/";

    private ProjectLinkUtil() {
    }

    public static String projectLink(String projectId) {
        return DriverPool.getAppUrl() + PROJECTS_PATH + escape(projectId);
    }

    public static String ruleLink(String projectId, String moduleName, String tableId) {
        return projectLink(projectId) + "/modules/" + escape(moduleName) + "?table=" + escape(tableId);
    }

    public static String ruleLink(String projectId, String moduleName, String tableId, String errorCell) {
        return ruleLink(projectId, moduleName, tableId) + "&errorCell=" + escape(errorCell);
    }

    public static String downloadLink(String projectId) {
        return DriverPool.getAppUrl() + "/web" + PROJECTS_PATH + escape(projectId) + "/files/?download=true";
    }

    public static String downloadLink(String projectId, String revision) {
        return downloadLink(projectId) + "&version=" + escape(revision);
    }

    public static String projectIdOf(String url) {
        String path = URI.create(url).getRawPath();
        int start = path.indexOf(PROJECTS_PATH);
        if (start == -1) {
            throw new IllegalArgumentException("Not a project address: " + url);
        }
        String idAndRest = path.substring(start + PROJECTS_PATH.length());
        return URLDecoder.decode(idAndRest.split("/")[0], StandardCharsets.UTF_8);
    }

    public static Optional<String> tableIdOf(String url) {
        String query = URI.create(url).getRawQuery();
        return query == null ? Optional.empty() : Arrays.stream(query.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parameter -> parameter.length == 2 && parameter[0].equals("table"))
                .map(parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8))
                .findFirst();
    }

    private static String escape(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
