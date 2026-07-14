package ujfe.core;

import java.util.Objects;

/**
 * Declarative bridge for browser APIs that must run in an external CSP-safe
 * JavaScript module.
 */
public final class BrowserApiBridge implements Node {
    private final String name;
    private String moduleSrc;
    private String api;
    private String action;
    private Class<?> requestType;
    private Class<?> responseType;
    private String triggerSelector;
    private String resultTarget;
    private String errorTarget;
    private boolean arrayBufferBase64Url = true;

    BrowserApiBridge(String name) {
        this.name = safeName(name);
        this.moduleSrc = "/assets/ujfe/" + this.name + ".js";
    }

    public BrowserApiBridge moduleSrc(String moduleSrc) {
        this.moduleSrc = Objects.requireNonNull(moduleSrc, "moduleSrc");
        return this;
    }

    public BrowserApiBridge api(String api) {
        this.api = requireText(api, "api");
        return this;
    }

    public BrowserApiBridge action(String action, Class<?> requestType, Class<?> responseType) {
        this.action = safeName(action);
        this.requestType = Objects.requireNonNull(requestType, "requestType");
        this.responseType = Objects.requireNonNull(responseType, "responseType");
        return this;
    }

    public BrowserApiBridge trigger(String selector) {
        this.triggerSelector = requireText(selector, "selector");
        return this;
    }

    public BrowserApiBridge resultTarget(String selector) {
        this.resultTarget = requireText(selector, "selector");
        return this;
    }

    public BrowserApiBridge errorTarget(String selector) {
        this.errorTarget = requireText(selector, "selector");
        return this;
    }

    public BrowserApiBridge arrayBufferBase64Url(boolean enabled) {
        this.arrayBufferBase64Url = enabled;
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.div()
            .data("ujfe-browser-bridge", name)
            .data("ujfe-module-src", moduleSrc)
            .data("ujfe-arraybuffer-base64url", Boolean.toString(arrayBufferBase64Url));
        if (api != null) {
            root.data("ujfe-browser-api", api);
        }
        if (action != null) {
            root.data("ujfe-browser-action", action)
                .data("ujfe-request-type", requestType.getName())
                .data("ujfe-response-type", responseType.getName());
        }
        if (triggerSelector != null) {
            root.data("ujfe-trigger", triggerSelector);
        }
        if (resultTarget != null) {
            root.data("ujfe-result-target", resultTarget);
        }
        if (errorTarget != null) {
            root.data("ujfe-error-target", errorTarget);
        }
        root.child(UI.clientModule(name)
            .src(moduleSrc)
            .defer(true));
        return root.render(context);
    }

    static String safeName(String value) {
        String name = requireText(value, "name");
        for (int index = 0; index < name.length(); index++) {
            char current = name.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '-' || current == '_' || current == '.')) {
                throw new IllegalArgumentException("Names may contain letters, numbers, '.', '-' and '_'");
            }
        }
        return name;
    }

    static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
