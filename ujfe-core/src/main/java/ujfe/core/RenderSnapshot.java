package ujfe.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RenderSnapshot {
    private final String route;
    private final String html;
    private final RenderMode renderMode;
    private final Instant createdAt;
    private final String etag;

    private RenderSnapshot(String route, String html, RenderMode renderMode, Instant createdAt) {
        this.route = RouteBuilder.validateContinueUrl(route);
        this.html = Objects.requireNonNull(html, "html");
        this.renderMode = Objects.requireNonNull(renderMode, "renderMode");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.etag = sha256(route + "\n" + html + "\n" + renderMode.cacheControlHeader());
    }

    public static RenderSnapshot of(String route, Node node, RenderMode renderMode) {
        Objects.requireNonNull(node, "node");
        return new RenderSnapshot(route, node.render(), renderMode, Instant.now());
    }

    public String route() {
        return route;
    }

    public String html() {
        return html;
    }

    public RenderMode renderMode() {
        return renderMode;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String etag() {
        return etag;
    }

    public Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("ETag", "\"" + etag + "\"");
        headers.put("Cache-Control", renderMode.cacheControlHeader());
        renderMode.revalidateEvent()
            .ifPresent(event -> headers.put("X-UJFE-Revalidate-On", event));
        return headers;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
