package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HealthPanel implements Node {
    private final List<Item> items = new ArrayList<>();
    private String title = "Health";

    HealthPanel() {
    }

    public HealthPanel title(String title) {
        this.title = BrowserApiBridge.requireText(title, "title");
        return this;
    }

    public HealthPanel item(String label, String status) {
        return item(label, status, null);
    }

    public HealthPanel item(String label, String status, String detail) {
        items.add(new Item(label, status, detail));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element section = UI.section()
            .data("ujfe-health-panel", "true")
            .child(UI.h2(title));
        Element list = UI.dl();
        for (Item item : items) {
            list.child(UI.dt(item.label))
                .child(UI.dd()
                    .data("ujfe-health-status", item.status)
                    .child(UI.statusIndicator(item.status, item.status))
                    .child(item.detail == null ? UI.text("") : UI.small(item.detail)));
        }
        return section.child(list)
            .render(context);
    }

    private static final class Item {
        private final String label;
        private final String status;
        private final String detail;

        private Item(String label, String status, String detail) {
            this.label = BrowserApiBridge.requireText(label, "label");
            this.status = BrowserApiBridge.requireText(status, "status");
            this.detail = detail == null || detail.isBlank() ? null : detail.trim();
        }
    }
}
