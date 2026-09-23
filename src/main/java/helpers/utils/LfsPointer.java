package helpers.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record LfsPointer(String oid, long size) {

    private static final Pattern FORMAT = Pattern.compile(
            "\\Aversion https://git-lfs\\.github\\.com/spec/v1\\noid sha256:([0-9a-f]{64})\\nsize (\\d+)\\n\\z");

    public static LfsPointer of(byte[] content) {
        return new LfsPointer(sha256(content), content.length);
    }

    public static Optional<LfsPointer> parse(byte[] committed) {
        Matcher pointer = FORMAT.matcher(new String(committed, StandardCharsets.UTF_8));
        return pointer.matches()
                ? Optional.of(new LfsPointer(pointer.group(1), Long.parseLong(pointer.group(2))))
                : Optional.empty();
    }

    public static String describe(byte[] committed) {
        String text = new String(committed, StandardCharsets.UTF_8);
        return text.startsWith("PK")
                ? "a workbook of " + committed.length + " bytes"
                : text.substring(0, Math.min(text.length(), 200));
    }

    public byte[] encode() {
        return ("version https://git-lfs.github.com/spec/v1\noid sha256:" + oid + "\nsize " + size + "\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
