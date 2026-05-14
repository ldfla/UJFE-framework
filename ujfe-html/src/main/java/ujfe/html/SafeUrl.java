package ujfe.html;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class SafeUrl {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "mailto", "tel", "data");
    private static final Set<String> ALLOWED_DATA_IMAGE_PREFIXES = Set.of(
            "data:image/gif;",
            "data:image/png;",
            "data:image/jpeg;",
            "data:image/webp;"
    );

    private SafeUrl() {
    }

    public static String sanitize(String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (trimmed.isEmpty()
                || trimmed.startsWith("/")
                || trimmed.startsWith("./")
                || trimmed.startsWith("../")
                || trimmed.startsWith("#")) {
            return trimmed;
        }

        int colonIndex = lower.indexOf(':');
        if (colonIndex < 0) {
            return trimmed;
        }

        String scheme = lower.substring(0, colonIndex);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Unsafe URL scheme: " + scheme);
        }

        if ("data".equals(scheme)) {
            boolean allowed = ALLOWED_DATA_IMAGE_PREFIXES.stream().anyMatch(lower::startsWith);
            if (!allowed) {
                throw new IllegalArgumentException("Only image data URLs are allowed");
            }
        } else {
            try {
                new URI(trimmed);
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("Invalid URL: " + value, exception);
            }
        }

        return trimmed;
    }
}
