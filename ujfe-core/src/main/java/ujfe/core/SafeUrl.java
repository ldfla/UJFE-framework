package ujfe.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Sanitizes URL values for HTML URL-bearing attributes.
 *
 * <p>All URL-bearing attributes ({@code href}, {@code src}, {@code action},
 * {@code poster}, {@code formaction}, {@code cite}, {@code data},
 * {@code background}) are routed through this sanitizer before rendering.</p>
 *
 * <p>The sanitizer delegates scheme decisions to the active {@link UrlPolicy}.
 * Relative references, fragment-only references, and scheme-less paths are
 * always allowed.</p>
 */
public final class SafeUrl {
    private static final Set<String> BLOCKED_SCHEMES = Set.of("javascript", "vbscript");
    private static final Set<String> ALLOWED_DATA_IMAGE_PREFIXES = Set.of(
            "data:image/gif;",
            "data:image/png;",
            "data:image/jpeg;",
            "data:image/webp;",
            "data:image/svg+xml;",
            "data:image/avif;",
            "data:image/bmp;"
    );

    private SafeUrl() {
    }

    /**
     * Sanitizes a URL value using the current default {@link UrlPolicy}.
     *
     * @throws IllegalArgumentException if the URL uses a blocked or disallowed scheme.
     */
    public static String sanitize(String value) {
        return sanitize(value, UrlPolicy.getDefault());
    }

    /**
     * Sanitizes a URL value using the given {@link UrlPolicy}.
     *
     * @throws IllegalArgumentException if the URL uses a blocked or disallowed scheme.
     */
    public static String sanitize(String value, UrlPolicy policy) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(policy, "policy");
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Empty, relative, and fragment-only references are always safe.
        if (trimmed.isEmpty()
                || trimmed.startsWith("/")
                || trimmed.startsWith("./")
                || trimmed.startsWith("../")
                || trimmed.startsWith("#")) {
            return trimmed;
        }

        int colonIndex = lower.indexOf(':');

        // No colon means no scheme — treat as a relative reference unless
        // the value contains characters that would be dangerous in an
        // unquoted attribute context.
        if (colonIndex < 0) {
            return trimmed;
        }

        String scheme = lower.substring(0, colonIndex);

        // Always block dangerous schemes, regardless of policy.
        if (BLOCKED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Blocked URL scheme: " + scheme);
        }

        // Handle data: URLs separately.
        if ("data".equals(scheme)) {
            if (!policy.allowDataImageUrls()) {
                throw new IllegalArgumentException(
                        "data: URLs are blocked by the current URL policy");
            }
            boolean allowed = ALLOWED_DATA_IMAGE_PREFIXES.stream().anyMatch(lower::startsWith);
            if (!allowed) {
                throw new IllegalArgumentException(
                        "Only image data URLs are allowed (data:image/*). "
                                + "Note: MIME prefix validation does not prove that "
                                + "decoded bytes are a valid image.");
            }
            return trimmed;
        }

        // Check if the scheme is allowed by the active policy.
        if (!policy.allowedSchemes().contains(scheme)) {
            throw new IllegalArgumentException(
                    "URL scheme not allowed by the current policy: " + scheme
                            + ". Allowed schemes: " + policy.allowedSchemes());
        }

        // Validate URI syntax for non-data URLs.
        try {
            new URI(trimmed);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid URL: " + value, exception);
        }

        return trimmed;
    }
}
