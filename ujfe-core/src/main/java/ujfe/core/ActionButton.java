package ujfe.core;

import java.util.Locale;
import java.util.Objects;

/**
 * A server action control with built-in metadata for confirmation, loading
 * state, CSRF fields, permissions, and live event handlers.
 */
public final class ActionButton implements Node {
    private final String label;
    private final String actionUrl;
    private final Runnable liveHandler;
    private String id;
    private String method = "post";
    private String csrfName;
    private String csrfToken;
    private String confirmMessage;
    private String loadingText;
    private String resultTarget;
    private String refreshUrl;
    private String variant;
    private String cssClasses;
    private boolean disabled;
    private boolean permitted = true;

    ActionButton(String label, String actionUrl) {
        this.label = requireText(label, "label");
        this.actionUrl = Objects.requireNonNull(actionUrl, "actionUrl");
        this.liveHandler = null;
    }

    ActionButton(String label, Runnable liveHandler) {
        this.label = requireText(label, "label");
        this.actionUrl = null;
        this.liveHandler = Objects.requireNonNull(liveHandler, "liveHandler");
    }

    public ActionButton id(String id) {
        this.id = id;
        return this;
    }

    public ActionButton method(String method) {
        this.method = normalizeMethod(method);
        return this;
    }

    public ActionButton csrf(String token) {
        return csrf("_csrf", token);
    }

    public ActionButton csrf(String name, String token) {
        this.csrfName = requireText(name, "csrfName");
        this.csrfToken = Objects.requireNonNull(token, "csrfToken");
        return this;
    }

    public ActionButton confirm(String message) {
        this.confirmMessage = requireText(message, "confirmMessage");
        return this;
    }

    public ActionButton loadingText(String loadingText) {
        this.loadingText = requireText(loadingText, "loadingText");
        return this;
    }

    public ActionButton resultTarget(String selector) {
        this.resultTarget = requireText(selector, "resultTarget");
        return this;
    }

    public ActionButton refresh(String url) {
        this.refreshUrl = RouteBuilder.validateContinueUrl(url);
        return this;
    }

    public ActionButton variant(String variant) {
        this.variant = requireText(variant, "variant");
        return this;
    }

    public ActionButton css(String classes) {
        this.cssClasses = classes;
        return this;
    }

    public ActionButton disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public ActionButton permission(boolean permitted) {
        this.permitted = permitted;
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        if (liveHandler != null) {
            return liveButton().render(context);
        }
        return formButton().render(context);
    }

    private Element liveButton() {
        return baseButton()
            .type("button")
            .data("ujfe-action-kind", "live")
            .onClick(liveHandler);
    }

    private Element formButton() {
        String requestedMethod = normalizeMethod(method);
        String formMethod = "get".equals(requestedMethod) ? "get" : "post";
        Element form = UI.form()
            .method(formMethod)
            .action(actionUrl)
            .data("ujfe-action-form", "true")
            .data("ujfe-action-kind", "http")
            .data("ujfe-method", requestedMethod);
        applyActionMetadata(form);

        if (!"get".equals(requestedMethod) && csrfName != null) {
            form.child(UI.inputHidden()
                .name(csrfName)
                .value(csrfToken));
        }
        if (!"get".equals(requestedMethod) && !"post".equals(requestedMethod)) {
            form.child(UI.inputHidden()
                .name("_method")
                .value(requestedMethod.toUpperCase(Locale.ROOT)));
        }
        form.child(baseButton().type("submit"));
        return form;
    }

    private Element baseButton() {
        Element button = UI.button(label)
            .data("ujfe-action-button", "true")
            .disabled(disabled || !permitted)
            .aria("disabled", Boolean.toString(disabled || !permitted));
        if (id != null) {
            button.id(id);
        }
        if (variant != null) {
            button.data("ujfe-variant", variant);
        }
        if (cssClasses != null) {
            button.css(cssClasses);
        }
        if (!permitted) {
            button.data("ujfe-permission", "denied");
        }
        applyActionMetadata(button);
        return button;
    }

    private void applyActionMetadata(Element element) {
        if (confirmMessage != null) {
            element.data("ujfe-confirm", confirmMessage);
        }
        if (loadingText != null) {
            element.data("ujfe-loading-text", loadingText);
        }
        if (resultTarget != null) {
            element.data("ujfe-result-target", resultTarget);
        }
        if (refreshUrl != null) {
            element.data("ujfe-refresh-url", refreshUrl);
        }
    }

    private static String normalizeMethod(String method) {
        String normalized = requireText(method, "method").toLowerCase(Locale.ROOT);
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (!(current >= 'a' && current <= 'z')) {
                throw new IllegalArgumentException("HTTP method must contain only letters");
            }
        }
        return normalized;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
