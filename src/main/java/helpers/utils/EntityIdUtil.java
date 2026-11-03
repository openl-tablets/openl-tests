package helpers.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class EntityIdUtil {

    public static final String URL_SAFE_BASE64_PATTERN = "[A-Za-z0-9_=-]+";
    public static final String PROJECT_ID_PATTERN = "[0-9A-HJKMNP-TV-Z]{13}";

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
}
