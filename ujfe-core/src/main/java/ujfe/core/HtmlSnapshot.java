package ujfe.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

public final class HtmlSnapshot {
    private final String html;

    private HtmlSnapshot(String html) {
        this.html = Objects.requireNonNull(html, "html");
    }

    public static HtmlSnapshot capture(Node node) {
        Objects.requireNonNull(node, "node");
        return new HtmlSnapshot(node.render());
    }

    public String html() {
        return html;
    }

    public String normalizedHtml() {
        return html.replaceAll(">\\s+<", "><")
            .trim();
    }

    public String fingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalizedHtml().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public boolean containsInlineExecutableScript() {
        return normalizedHtml().matches("(?is).*<script(?![^>]*\\bsrc=)(?![^>]*\\btype=\"application/json\")[^>]*>.*");
    }
}
