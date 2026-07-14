package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Form helper for administrative actions with CSRF, confirmation, status
 * targets, loading metadata, and live submit support.
 */
public final class ActionForm implements Node {
    private final String actionUrl;
    private final List<Node> children = new ArrayList<>();
    private String method = "post";
    private String csrfName;
    private String csrfToken;
    private String confirmMessage;
    private String loadingText;
    private String resultTarget;
    private String statusTarget;
    private String refreshUrl;
    private String cssClasses;
    private Runnable submitHandler;
    private boolean disabled;

    ActionForm(String actionUrl) {
        this.actionUrl = Objects.requireNonNull(actionUrl, "actionUrl");
    }

    public ActionForm method(String method) {
        this.method = normalizeMethod(method);
        return this;
    }

    public ActionForm csrf(String token) {
        return csrf("_csrf", token);
    }

    public ActionForm csrf(String name, String token) {
        this.csrfName = requireText(name, "csrfName");
        this.csrfToken = Objects.requireNonNull(token, "csrfToken");
        return this;
    }

    public ActionForm confirm(String message) {
        this.confirmMessage = requireText(message, "confirmMessage");
        return this;
    }

    public ActionForm loadingText(String loadingText) {
        this.loadingText = requireText(loadingText, "loadingText");
        return this;
    }

    public ActionForm resultTarget(String selector) {
        this.resultTarget = requireText(selector, "resultTarget");
        return this;
    }

    public ActionForm statusTarget(String selector) {
        this.statusTarget = requireText(selector, "statusTarget");
        return this;
    }

    public ActionForm refresh(String url) {
        this.refreshUrl = RouteBuilder.validateContinueUrl(url);
        return this;
    }

    public ActionForm css(String classes) {
        this.cssClasses = classes;
        return this;
    }

    public ActionForm disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public ActionForm onSubmit(Runnable handler) {
        this.submitHandler = Objects.requireNonNull(handler, "handler");
        return this;
    }

    public ActionForm child(Node child) {
        children.add(Objects.requireNonNull(child, "child"));
        return this;
    }

    public ActionForm children(Node... nodes) {
        Objects.requireNonNull(nodes, "nodes");
        for (Node node : nodes) {
            child(node);
        }
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        String requestedMethod = normalizeMethod(method);
        String formMethod = "get".equals(requestedMethod) ? "get" : "post";
        Element form = UI.form()
            .method(formMethod)
            .action(actionUrl)
            .data("ujfe-action-form", "true")
            .data("ujfe-method", requestedMethod)
            .aria("disabled", Boolean.toString(disabled));
        if (cssClasses != null) {
            form.css(cssClasses);
        }
        if (submitHandler != null) {
            form.onSubmit(submitHandler);
        }
        if (confirmMessage != null) {
            form.data("ujfe-confirm", confirmMessage);
        }
        if (loadingText != null) {
            form.data("ujfe-loading-text", loadingText);
        }
        if (resultTarget != null) {
            form.data("ujfe-result-target", resultTarget);
        }
        if (statusTarget != null) {
            form.data("ujfe-status-target", statusTarget);
        }
        if (refreshUrl != null) {
            form.data("ujfe-refresh-url", refreshUrl);
        }
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
        for (Node child : children) {
            form.child(child);
        }
        return form.render(context);
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
