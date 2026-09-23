package helpers.utils;

import configuration.driver.DriverPool;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ProjectLinkUtil {

    private static final String ID_SEPARATOR = ":";

    private ProjectLinkUtil() {
    }

    public static String projectLink(String projectId) {
        return DriverPool.getAppUrl() + "/projects/" + escape(projectId);
    }

    public static String moduleLink(String projectId, String moduleName) {
        return projectLink(projectId) + "/modules/" + escape(moduleName);
    }

    public static String ruleLink(String projectId, String moduleName, String tableId) {
        return moduleLink(projectId, moduleName) + "?table=" + escape(tableId);
    }

    public static String ruleLink(String projectId, String moduleName, String tableId, String errorCell) {
        return ruleLink(projectId, moduleName, tableId) + "&errorCell=" + escape(errorCell);
    }

    public static String downloadLink(String projectId) {
        return DriverPool.getAppUrl() + "/web/projects/" + escape(projectId) + "/files/?download=true";
    }

    public static String downloadLink(String projectId, String revision) {
        return downloadLink(projectId) + "&version=" + escape(revision);
    }

    public static String encodeProjectId(String repositoryId, String projectName) {
        byte[] source = (repositoryId + ID_SEPARATOR + projectName).getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().encodeToString(source);
    }

    public static String repositoryOf(String projectId) {
        String decoded = EntityIdUtil.decodeUrlSafeId(projectId);
        int separator = decoded.indexOf(ID_SEPARATOR);
        if (separator == -1) {
            throw new IllegalArgumentException("Not a project id: " + projectId);
        }
        return decoded.substring(0, separator);
    }

    private static String escape(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
