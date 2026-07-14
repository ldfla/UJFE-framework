package ujfe.live;

import ujfe.core.RenderMode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LiveHttpCache {
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String ETAG = "ETag";
    public static final String IF_NONE_MATCH = "If-None-Match";
    public static final String REVALIDATE_ON = "X-UJFE-Revalidate-On";

    private static final CacheHeaders CLIENT_SCRIPT_HEADERS = new CacheHeaders(
        strongEtag(LiveClientScript.script()),
        "public, max-age=300, must-revalidate"
    );
    private static final CacheHeaders DEV_SCRIPT_HEADERS = new CacheHeaders(
        strongEtag(LiveDevToolsScript.script()),
        "no-cache"
    );

    private LiveHttpCache() {
    }

    public static CacheHeaders clientScriptHeaders() {
        return CLIENT_SCRIPT_HEADERS;
    }

    public static CacheHeaders devScriptHeaders() {
        return DEV_SCRIPT_HEADERS;
    }

    public static CacheHeaders cssHeaders(String css) {
        return new CacheHeaders(strongEtag(css == null ? "" : css), "private, no-cache");
    }

    public static Map<String, String> routeHeaders(RenderMode renderMode) {
        Objects.requireNonNull(renderMode, "renderMode");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(CACHE_CONTROL, renderMode.cacheControlHeader());
        renderMode.revalidateEvent()
            .ifPresent(event -> headers.put(REVALIDATE_ON, event));
        return Collections.unmodifiableMap(headers);
    }

    public static boolean matchesEtag(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank() || etag == null || etag.isBlank()) {
            return false;
        }

        int start = 0;
        while (start < ifNoneMatch.length()) {
            int comma = ifNoneMatch.indexOf(',', start);
            int end = comma < 0 ? ifNoneMatch.length() : comma;
            if (etagTokenMatches(ifNoneMatch.substring(start, end).trim(), etag)) {
                return true;
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return false;
    }

    static String strongEtag(String content) {
        byte[] bytes = Objects.requireNonNull(content, "content")
            .getBytes(StandardCharsets.UTF_8);
        byte[] digest = sha256(bytes);
        StringBuilder hex = new StringBuilder(digest.length * 2 + 2);
        hex.append('"');
        for (byte value : digest) {
            int current = value & 0xff;
            if (current < 16) {
                hex.append('0');
            }
            hex.append(Integer.toHexString(current));
        }
        hex.append('"');
        return hex.toString();
    }

    private static boolean etagTokenMatches(String token, String etag) {
        if (token.isEmpty()) {
            return false;
        }
        if ("*".equals(token)) {
            return true;
        }
        return stripWeakPrefix(token).equals(stripWeakPrefix(etag));
    }

    private static String stripWeakPrefix(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 2
            && trimmed.charAt(0) == 'W'
            && trimmed.charAt(1) == '/') {
            return trimmed.substring(2)
                .trim();
        }
        return trimmed;
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256")
                .digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static final class CacheHeaders {
        private final String etag;
        private final String cacheControl;
        private final Map<String, String> headers;

        private CacheHeaders(String etag, String cacheControl) {
            this.etag = Objects.requireNonNull(etag, "etag");
            this.cacheControl = Objects.requireNonNull(cacheControl, "cacheControl");
            Map<String, String> values = new LinkedHashMap<>();
            values.put(CACHE_CONTROL, cacheControl);
            values.put(ETAG, etag);
            this.headers = Collections.unmodifiableMap(values);
        }

        public String etag() {
            return etag;
        }

        public String cacheControl() {
            return cacheControl;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public boolean matches(String ifNoneMatch) {
            return matchesEtag(ifNoneMatch, etag);
        }
    }
}
