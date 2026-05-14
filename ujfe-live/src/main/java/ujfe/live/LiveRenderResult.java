package ujfe.live;

import java.util.Objects;

public final class LiveRenderResult {
    private final String html;
    private final String css;

    public LiveRenderResult(String html, String css) {
        this.html = Objects.requireNonNull(html, "html");
        this.css = Objects.requireNonNull(css, "css");
    }

    public String html() {
        return html;
    }

    public String css() {
        return css;
    }
}
