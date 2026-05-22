package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Typed UI contract metadata for payloads, actions, and realtime events.
 */
public final class UiContract implements Node {
    private final String name;
    private final List<Entry> entries = new ArrayList<>();

    UiContract(String name) {
        this.name = BrowserApiBridge.safeName(name);
    }

    public UiContract payload(Class<?> type) {
        entries.add(new Entry("payload", type));
        return this;
    }

    public UiContract action(String name, Class<?> type) {
        entries.add(new Entry("action:" + BrowserApiBridge.safeName(name), type));
        return this;
    }

    public UiContract event(String name, Class<?> type) {
        entries.add(new Entry("event:" + BrowserApiBridge.safeName(name), type));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        return UI.template()
            .data("ujfe-contract", name)
            .child(json())
            .render(context);
    }

    private String json() {
        StringBuilder json = new StringBuilder("{\"name\":\"")
            .append(escape(name))
            .append("\",\"entries\":[");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            Entry entry = entries.get(index);
            json.append("{\"kind\":\"")
                .append(escape(entry.kind))
                .append("\",\"schema\":")
                .append(TypeSchema.of(entry.type).json())
                .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static final class Entry {
        private final String kind;
        private final Class<?> type;

        private Entry(String kind, Class<?> type) {
            this.kind = kind;
            this.type = Objects.requireNonNull(type, "type");
        }
    }
}
