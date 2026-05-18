package ujfe.live;

import java.util.Map;

public final class LiveHttpSecurity {
    private LiveHttpSecurity() {
    }

    public static Map<String, String> securityHeaders() {
        return securityHeaders(SecurityHeadersConfig.defaults());
    }

    public static Map<String, String> securityHeaders(SecurityHeadersConfig config) {
        return Map.copyOf(java.util.Objects.requireNonNull(config, "config")
            .headers());
    }

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    public static String generateCsrfToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    public static void validateCsrf(LiveSessionConfig config, String sessionToken, LiveHttpRequestMetadata metadata) {
        if (config.isCsrfProtectionDisabled()) {
            return;
        }

        String requestToken = metadata.csrfToken()
            .orElse(null);
        boolean hasHeader = requestToken != null && !requestToken.isBlank();
        if (!hasHeader) {
            throw new LiveCsrfException(LiveHttpFailureCategory.MISSING_CSRF_TOKEN, "Missing CSRF token.", false);
        }

        if (!java.security.MessageDigest.isEqual(sessionToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            requestToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new LiveCsrfException(LiveHttpFailureCategory.INVALID_CSRF_TOKEN, "Invalid CSRF token.", true);
        }

        if (metadata.hasOrigin()) {
            validateSameOrigin(metadata.origin()
                .get(), metadata);
        } else if (metadata.hasReferer()) {
            validateSameOrigin(metadata.referer()
                .get(), metadata);
        } else {
            throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Missing Origin and Referer headers.", true);
        }
    }

    private static void validateSameOrigin(String originOrReferer, LiveHttpRequestMetadata metadata) {
        String hostHeader = metadata.host()
            .orElse("");
        String requestScheme = metadata.scheme()
            .orElse("");
        if (hostHeader.isBlank()) {
            throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Missing Host header for origin validation.", true);
        }
        if (requestScheme.isBlank()) {
            throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Missing request scheme for origin validation.", true);
        }

        try {
            java.net.URI uri = new java.net.URI(originOrReferer);
            String uriScheme = uri.getScheme();
            String uriHost = uri.getHost();
            if (uriScheme == null || !uriScheme.equalsIgnoreCase(requestScheme)) {
                throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Cross-origin scheme mismatch.", true);
            }
            if (uriHost == null) {
                throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Invalid Origin or Referer.", true);
            }

            HostPort requestHost = parseHostHeader(hostHeader, requestScheme);
            int uriPort = effectivePort(uri.getPort(), uriScheme);

            if (!uriHost.equalsIgnoreCase(requestHost.host())) {
                throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Cross-origin request rejected.", true);
            }

            if (uriPort != requestHost.port()) {
                throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Cross-origin port mismatch.", true);
            }
        } catch (LiveCsrfException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Invalid Origin or Referer.", true);
        }
    }

    private static HostPort parseHostHeader(String hostHeader, String scheme) {
        String host = hostHeader.trim();
        int port = -1;

        if (host.startsWith("[")) {
            int bracketEnd = host.indexOf(']');
            if (bracketEnd < 0) {
                throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Invalid Host header.", true);
            }
            String hostName = host.substring(1, bracketEnd);
            if (bracketEnd + 1 < host.length()) {
                if (host.charAt(bracketEnd + 1) != ':') {
                    throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Invalid Host header.", true);
                }
                port = parsePort(host.substring(bracketEnd + 2));
            }
            return new HostPort(hostName, effectivePort(port, scheme));
        }

        int colonIndex = host.lastIndexOf(':');
        if (colonIndex > 0 && host.indexOf(':') == colonIndex) {
            String potentialPort = host.substring(colonIndex + 1);
            if (potentialPort.matches("\\d+")) {
                port = parsePort(potentialPort);
                host = host.substring(0, colonIndex);
            }
        }

        if (host.isBlank()) {
            throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Invalid Host header.", true);
        }
        return new HostPort(host, effectivePort(port, scheme));
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw new NumberFormatException("port out of range");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Invalid Host header.", true);
        }
    }

    private static int effectivePort(int port, String scheme) {
        if (port != -1) {
            return port;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        throw new LiveCsrfException(LiveHttpFailureCategory.CROSS_ORIGIN_REQUEST, "Unsupported request scheme.", true);
    }

    private static final class HostPort {
        private final String host;
        private final int port;

        private HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private String host() {
            return host;
        }

        private int port() {
            return port;
        }
    }
}
