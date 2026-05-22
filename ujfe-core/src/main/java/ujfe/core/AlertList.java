package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AlertList implements Node {
    private final List<Alert> alerts = new ArrayList<>();
    private String title = "Alerts";

    AlertList() {
    }

    public AlertList title(String title) {
        this.title = BrowserApiBridge.requireText(title, "title");
        return this;
    }

    public AlertList alert(String tone, String message) {
        return alert(tone, message, null);
    }

    public AlertList alert(String tone, String message, String href) {
        alerts.add(new Alert(tone, message, href));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element section = UI.section()
            .data("ujfe-alert-list", "true")
            .child(UI.h2(title));
        if (alerts.isEmpty()) {
            section.child(UI.div()
                .role("status")
                .data("ujfe-empty-state", "true")
                .child("No alerts"));
            return section.render(context);
        }
        Element list = UI.ul();
        for (Alert alert : alerts) {
            Element item = UI.li()
                .data("ujfe-alert-tone", alert.tone)
                .child(UI.span(alert.message));
            if (alert.href != null) {
                item.child(UI.a("Open")
                    .href(alert.href));
            }
            list.child(item);
        }
        return section.child(list)
            .render(context);
    }

    private static final class Alert {
        private final String tone;
        private final String message;
        private final String href;

        private Alert(String tone, String message, String href) {
            this.tone = BrowserApiBridge.requireText(tone, "tone");
            this.message = BrowserApiBridge.requireText(message, "message");
            this.href = href == null || href.isBlank() ? null : RouteBuilder.validateContinueUrl(href);
        }
    }
}
