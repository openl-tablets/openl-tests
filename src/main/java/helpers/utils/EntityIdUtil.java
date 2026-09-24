package helpers.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class EntityIdUtil {

    public static final String URL_SAFE_BASE64_PATTERN = "[A-Za-z0-9_=-]+";

    private static final String PROJECT_ID_SEPARATOR = ":";

    private EntityIdUtil() {
    }

    public static String lastUrlSegment(String url) {
        String withoutQuery = url.split("\\?")[0];
        return withoutQuery.substring(withoutQuery.lastIndexOf('/') + 1);
    }

    public static String decodeUrlSafeId(String idSegment) {
        String padded = idSegment + "=".repeat((4 - idSegment.length() % 4) % 4);
        return new String(Base64.getUrlDecoder().decode(padded), StandardCharsets.UTF_8);
    }

    public static String encodeProjectId(String repositoryId, String projectName) {
        byte[] source = (repositoryId + PROJECT_ID_SEPARATOR + projectName).getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().encodeToString(source);
    }

    public static String repositoryOf(String projectId) {
        String decoded = decodeUrlSafeId(projectId);
        int separator = decoded.indexOf(PROJECT_ID_SEPARATOR);
        if (separator == -1) {
            throw new IllegalArgumentException("Not a project id: " + projectId);
        }
        return decoded.substring(0, separator);
    }
}
