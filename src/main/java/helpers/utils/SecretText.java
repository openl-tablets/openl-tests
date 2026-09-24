package helpers.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecretText {

    private static final List<String> TOKEN_VARIABLES = List.of("GITHUB_TOKEN", "LFS_GITLAB_TOKEN", "LFS_BITBUCKET_TOKEN");
    private static final List<String> URL_VARIABLES = List.of("LFS_GITLAB_REPO", "LFS_BITBUCKET_REPO");

    private SecretText() {
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String redacted = text;
        for (Map.Entry<String, String> secret : secretForms().entrySet()) {
            int flags = isToken(secret.getValue()) ? 0 : Pattern.CASE_INSENSITIVE;
            redacted = Pattern.compile(Pattern.quote(secret.getKey()), flags)
                    .matcher(redacted)
                    .replaceAll(Matcher.quoteReplacement(secret.getValue()));
        }
        return redacted;
    }

    private static boolean isToken(String placeholder) {
        return TOKEN_VARIABLES.stream().anyMatch(variable -> placeholder.equals("${" + variable + "}"));
    }

    private static Map<String, String> secretForms() {
        Map<String, String> forms = new LinkedHashMap<>();
        for (String variable : TOKEN_VARIABLES) {
            String value = System.getenv(variable);
            if (value != null && value.trim().length() >= 16) {
                forms.put(value.trim(), "${" + variable + "}");
            }
        }
        for (String variable : URL_VARIABLES) {
            String value = System.getenv(variable);
            if (value != null && !value.isBlank()) {
                urlForms(value).forEach(form -> forms.putIfAbsent(form, "${" + variable + "}"));
            }
        }
        List<Map.Entry<String, String>> longestFirst = new ArrayList<>(forms.entrySet());
        longestFirst.sort(Comparator.comparingInt((Map.Entry<String, String> form) -> form.getKey().length()).reversed());
        Map<String, String> ordered = new LinkedHashMap<>();
        longestFirst.forEach(form -> ordered.put(form.getKey(), form.getValue()));
        return ordered;
    }

    static List<String> urlForms(String url) {
        String trimmed = url.trim();
        String withoutScheme = trimmed.replaceFirst("^[a-zA-Z]+://", "").replaceFirst("^[^@/]*@", "");
        String bare = withoutScheme.replaceFirst("/+$", "").replaceFirst("\\.git$", "");
        String path = bare.contains("/") ? bare.substring(bare.indexOf('/') + 1) : bare;
        List<String> forms = new ArrayList<>(List.of(trimmed, withoutScheme, bare + ".git", bare, path + ".git", path));
        forms.add(path.replace("/", "%2F"));
        forms.removeIf(form -> form.length() < 6 || !form.contains("/") && !form.contains("%2F"));
        return forms;
    }
}
