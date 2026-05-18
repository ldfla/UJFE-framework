package ujfe.core;

import java.util.Objects;

/**
 * Intentionally unsafe raw HTML node.
 *
 * <p>This API bypasses UJFE's normal text escaping. Rendering untrusted or
 * user-controlled input through this type can create cross-site scripting
 * (XSS) vulnerabilities. Use it only for HTML that has already been produced
 * by a trusted system and reviewed for the current security context.</p>
 */
public final class UnsafeHtml implements Node {
    private final String html;

    private UnsafeHtml(String html) {
        this.html = Objects.requireNonNull(html, "html");
    }

    /**
     * Creates an intentionally unsafe raw HTML node.
     *
     * <p>The provided HTML is rendered exactly as supplied. It is not escaped,
     * sanitized, or validated by UJFE, so unsafe input can create XSS
     * vulnerabilities.</p>
     */
    public static UnsafeHtml of(String html) {
        return new UnsafeHtml(html);
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        return html;
    }
}
