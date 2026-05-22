package ujfe.core;

import java.time.Duration;
import java.util.Objects;

/**
 * WebAuthn/passkey bridge metadata for external browser modules.
 */
public final class WebAuthnBridge implements Node {
    public enum Mode {
        AUTHENTICATE("authenticate"),
        REGISTER("register");

        private final String value;

        Mode(String value) {
            this.value = value;
        }
    }

    private final Mode mode;
    private final String challengeUrl;
    private final String responseUrl;
    private String moduleSrc = "/assets/ujfe/webauthn.js";
    private String triggerSelector;
    private String errorTarget;
    private String statusTarget;
    private String userVerification = "preferred";
    private Duration timeout = Duration.ofMinutes(2);

    WebAuthnBridge(Mode mode, String challengeUrl, String responseUrl) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.challengeUrl = RouteBuilder.validateContinueUrl(challengeUrl);
        this.responseUrl = RouteBuilder.validateContinueUrl(responseUrl);
    }

    public WebAuthnBridge moduleSrc(String moduleSrc) {
        this.moduleSrc = Objects.requireNonNull(moduleSrc, "moduleSrc");
        return this;
    }

    public WebAuthnBridge trigger(String selector) {
        this.triggerSelector = BrowserApiBridge.requireText(selector, "selector");
        return this;
    }

    public WebAuthnBridge errorTarget(String selector) {
        this.errorTarget = BrowserApiBridge.requireText(selector, "selector");
        return this;
    }

    public WebAuthnBridge statusTarget(String selector) {
        this.statusTarget = BrowserApiBridge.requireText(selector, "selector");
        return this;
    }

    public WebAuthnBridge userVerification(String userVerification) {
        this.userVerification = BrowserApiBridge.requireText(userVerification, "userVerification");
        return this;
    }

    public WebAuthnBridge timeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.div()
            .data("ujfe-webauthn", mode.value)
            .data("ujfe-challenge-url", challengeUrl)
            .data("ujfe-response-url", responseUrl)
            .data("ujfe-user-verification", userVerification)
            .data("ujfe-timeout-ms", Long.toString(timeout.toMillis()))
            .data("ujfe-arraybuffer-base64url", "true");
        if (triggerSelector != null) {
            root.data("ujfe-trigger", triggerSelector);
        }
        if (errorTarget != null) {
            root.data("ujfe-error-target", errorTarget);
        }
        if (statusTarget != null) {
            root.data("ujfe-status-target", statusTarget);
        }
        root.child(UI.clientModule("webauthn")
            .src(moduleSrc)
            .action(mode.value, PublicKeyCredentialRequest.class));
        return root.render(context);
    }

    public static final class PublicKeyCredentialRequest {
        private PublicKeyCredentialRequest() {
        }
    }
}
