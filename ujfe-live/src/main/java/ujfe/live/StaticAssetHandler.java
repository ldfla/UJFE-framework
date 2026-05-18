package ujfe.live;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StaticAssetHandler {
    private static final Logger LOGGER = Logger.getLogger(StaticAssetHandler.class.getName());
    private static final Set<String> STATIC_PREFIXES = Set.of("/assets/", "/static/");
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
        Map.entry("png", "image/png"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("gif", "image/gif"),
        Map.entry("webp", "image/webp"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("ico", "image/x-icon"),
        Map.entry("mp4", "video/mp4"),
        Map.entry("webm", "video/webm"),
        Map.entry("ogg", "application/ogg"),
        Map.entry("mp3", "audio/mpeg"),
        Map.entry("wav", "audio/wav"),
        Map.entry("m4a", "audio/mp4"),
        Map.entry("woff", "font/woff"),
        Map.entry("woff2", "font/woff2"),
        Map.entry("ttf", "font/ttf"),
        Map.entry("otf", "font/otf"),
        Map.entry("eot", "application/vnd.ms-fontobject"),
        Map.entry("css", "text/css; charset=utf-8"),
        Map.entry("js", "application/javascript; charset=utf-8"),
        Map.entry("mjs", "application/javascript; charset=utf-8"),
        Map.entry("map", "application/json; charset=utf-8"),
        Map.entry("txt", "text/plain; charset=utf-8"),
        Map.entry("json", "application/json; charset=utf-8"),
        Map.entry("xml", "application/xml; charset=utf-8"),
        Map.entry("csv", "text/csv; charset=utf-8")
    );

    private StaticAssetHandler() {
    }

    public static boolean isStaticAssetPath(String path) {
        String normalized = normalize(path);
        if (LiveHttpPaths.isInternalPath(normalized)) {
            return false;
        }
        if ("/favicon.ico".equals(normalized)) {
            return true;
        }
        for (String prefix : STATIC_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return extension(normalized).isPresent();
    }

    public static boolean isUnsafePath(String path) {
        String normalized = normalize(path);
        if (normalized.indexOf('\0') >= 0 || normalized.indexOf('\\') >= 0) {
            return true;
        }
        String decoded = decodeTwice(normalized);
        String lower = decoded.toLowerCase(Locale.ROOT);
        return lower.indexOf('\0') >= 0
            || lower.contains("\\")
            || lower.contains("/../")
            || lower.startsWith("../")
            || lower.endsWith("/..")
            || lower.equals("..")
            || lower.contains("%2e")
            || lower.contains("%2f")
            || lower.contains("%5c");
    }

    public static String contentType(String path) {
        return extension(path)
            .map(CONTENT_TYPES::get)
            .orElse("application/octet-stream");
    }

    public static String missingAssetBody() {
        return "Static asset not found.";
    }

    public static String rejectedAssetBody() {
        return "Invalid static asset path.";
    }

    public static void logNotFound(String adapter, String path) {
        LOGGER.log(Level.FINE, () -> "event=ujfe.static_asset_not_found adapter="
            + safe(adapter) + " path=" + safe(path));
    }

    public static void logRejected(String adapter, String path) {
        LOGGER.log(Level.WARNING, () -> "event=ujfe.static_asset_rejected adapter="
            + safe(adapter) + " path=" + safe(path) + " reason=unsafe_path");
    }

    private static Optional<String> extension(String path) {
        String normalized = normalize(path);
        int slash = normalized.lastIndexOf('/');
        int dot = normalized.lastIndexOf('.');
        if (dot <= slash + 1 || dot == normalized.length() - 1) {
            return Optional.empty();
        }
        String ext = normalized.substring(dot + 1)
            .toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.containsKey(ext) ? Optional.of(ext) : Optional.empty();
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String decodeTwice(String value) {
        String once = decode(value);
        return decode(once);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException exception) {
            return value;
        } catch (Exception exception) {
            return value;
        }
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._:/@-]", "_");
    }
}
