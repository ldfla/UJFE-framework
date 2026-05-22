package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CSP-safe browser module declaration. The rendered output references an
 * external module script and annotates the expected typed actions.
 */
public final class ClientModule implements Node {
    private final String name;
    private final List<Action> actions = new ArrayList<>();
    private String src;
    private String target;
    private String errorTarget;
    private boolean defer = true;

    ClientModule(String name) {
        this.name = safeName(name);
        this.src = "/assets/ujfe/" + this.name + ".js";
    }

    public ClientModule src(String src) {
        this.src = Objects.requireNonNull(src, "src");
        return this;
    }

    public ClientModule action(String name, Class<?> requestType) {
        actions.add(new Action(safeName(name), Objects.requireNonNull(requestType, "requestType")));
        return this;
    }

    public ClientModule target(String selector) {
        this.target = requireText(selector, "selector");
        return this;
    }

    public ClientModule errorTarget(String selector) {
        this.errorTarget = requireText(selector, "selector");
        return this;
    }

    public ClientModule defer(boolean defer) {
        this.defer = defer;
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element script = UI.script()
            .type("module")
            .src(src)
            .data("ujfe-client-module", name)
            .data("ujfe-client-actions", actionsJson());
        if (target != null) {
            script.data("ujfe-target", target);
        }
        if (errorTarget != null) {
            script.data("ujfe-error-target", errorTarget);
        }
        if (defer) {
            script.attr("defer", true);
        }
        return script.render(context);
    }

    private String actionsJson() {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < actions.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            Action action = actions.get(index);
            json.append("{\"name\":\"")
                .append(escapeJson(action.name))
                .append("\",\"requestType\":\"")
                .append(escapeJson(action.requestType.getName()))
                .append("\"}");
        }
        json.append(']');
        return json.toString();
    }

    private static String safeName(String value) {
        String name = requireText(value, "name");
        for (int index = 0; index < name.length(); index++) {
            char current = name.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '-' || current == '_' || current == '.')) {
                throw new IllegalArgumentException("Client module names may contain letters, numbers, '.', '-' and '_'");
            }
        }
        return name;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }

    private static final class Action {
        private final String name;
        private final Class<?> requestType;

        private Action(String name, Class<?> requestType) {
            this.name = name;
            this.requestType = requestType;
        }
    }
}
