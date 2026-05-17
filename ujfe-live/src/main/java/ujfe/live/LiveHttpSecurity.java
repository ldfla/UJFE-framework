package ujfe.live;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LiveHttpSecurity {
    private static final Map<String, String> SECURITY_HEADERS = createSecurityHeaders();

    private LiveHttpSecurity() {
    }

    public static Map<String, String> securityHeaders() {
        return new LinkedHashMap<>(SECURITY_HEADERS);
    }

    private static Map<String, String> createSecurityHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Content-Type-Options", "nosniff");
        headers.put("X-Frame-Options", "DENY");
        headers.put("Referrer-Policy", "no-referrer");
        headers.put("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        headers.put("Content-Security-Policy",
                "default-src 'self'; "
                        + "script-src 'self'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: https:; "
                        + "media-src 'self' data: https:; "
                        + "object-src 'none'; "
                        + "base-uri 'none'; "
                        + "frame-ancestors 'none'; "
                        + "form-action 'self'");
        return Map.copyOf(headers);
    }
}
