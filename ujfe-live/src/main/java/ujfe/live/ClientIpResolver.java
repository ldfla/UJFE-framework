package ujfe.live;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ClientIpResolver {
    private static final String UNKNOWN = "unknown";

    private ClientIpResolver() {
    }

    public static String resolve(LiveHttpRequestMetadata metadata, LiveSessionConfig config) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(config, "config");

        String remoteAddress = normalizeIp(metadata.remoteAddress()
            .orElse(UNKNOWN));
        if (config.trustedProxyAddresses()
            .contains(remoteAddress)) {
            Optional<String> forwarded = firstForwardedFor(metadata.forwarded()
                .orElse(null));
            if (forwarded.isPresent()) {
                return forwarded.get();
            }

            Optional<String> xForwardedFor = firstHeaderIp(metadata.xForwardedFor()
                .orElse(null));
            if (xForwardedFor.isPresent()) {
                return xForwardedFor.get();
            }

            Optional<String> realIp = firstHeaderIp(metadata.xRealIp()
                .orElse(null));
            if (realIp.isPresent()) {
                return realIp.get();
            }
        }
        return remoteAddress;
    }

    private static Optional<String> firstForwardedFor(String header) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        for (String part : header.split(";|,")) {
            String trimmed = part.trim();
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String name = trimmed.substring(0, separator)
                .trim()
                .toLowerCase(Locale.ROOT);
            if (!"for".equals(name)) {
                continue;
            }
            String value = unquote(trimmed.substring(separator + 1)
                .trim());
            if (!value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return Optional.of(normalizeIp(value));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> firstHeaderIp(String header) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        for (String value : header.split(",")) {
            String normalized = normalizeIp(value);
            if (!normalized.isBlank() && !"unknown".equalsIgnoreCase(normalized)) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private static String normalizeIp(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return UNKNOWN;
        }
        normalized = unquote(normalized);
        if (normalized.startsWith("[")) {
            int end = normalized.indexOf(']');
            return end > 0 ? normalized.substring(1, end) : UNKNOWN;
        }
        int colon = normalized.lastIndexOf(':');
        if (colon > 0 && normalized.indexOf(':') == colon && normalized.substring(colon + 1)
            .matches("\\d+")) {
            normalized = normalized.substring(0, colon);
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? UNKNOWN : normalized;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
