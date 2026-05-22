package ujfe.core;

import java.util.Base64;
import java.util.Objects;

/**
 * Base64url helpers for browser APIs such as WebAuthn that exchange binary
 * values through URL-safe strings without padding.
 */
public final class Base64Url {
    private Base64Url() {
    }

    public static String encode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    public static byte[] decode(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            return new byte[0];
        }
        return Base64.getUrlDecoder()
            .decode(padded(value.trim()));
    }

    private static String padded(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        int padding = 4 - remainder;
        return value +
            "=".repeat(padding);
    }
}
