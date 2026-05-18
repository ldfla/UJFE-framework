package ujfe.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ujfe.live.LiveSessionConfig;
import ujfe.validation.ValidationMode;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("ujfe.validation")
public final class UjfeSpringValidationProperties {
    private ValidationMode mode = ValidationMode.OFF;
    private boolean accessibilityEnabled;
    private boolean seoEnabled;
    private boolean canonicalEnabled;
    private boolean openGraphEnabled;
    private boolean htmlLangEnabled;
    private List<String> disabledRules = new ArrayList<>();

    public ValidationMode getMode() {
        return mode;
    }

    public void setMode(ValidationMode mode) {
        this.mode = mode == null ? ValidationMode.OFF : mode;
    }

    public boolean isAccessibilityEnabled() {
        return accessibilityEnabled;
    }

    public void setAccessibilityEnabled(boolean accessibilityEnabled) {
        this.accessibilityEnabled = accessibilityEnabled;
    }

    public boolean isSeoEnabled() {
        return seoEnabled;
    }

    public void setSeoEnabled(boolean seoEnabled) {
        this.seoEnabled = seoEnabled;
    }

    public boolean isCanonicalEnabled() {
        return canonicalEnabled;
    }

    public void setCanonicalEnabled(boolean canonicalEnabled) {
        this.canonicalEnabled = canonicalEnabled;
    }

    public boolean isOpenGraphEnabled() {
        return openGraphEnabled;
    }

    public void setOpenGraphEnabled(boolean openGraphEnabled) {
        this.openGraphEnabled = openGraphEnabled;
    }

    public boolean isHtmlLangEnabled() {
        return htmlLangEnabled;
    }

    public void setHtmlLangEnabled(boolean htmlLangEnabled) {
        this.htmlLangEnabled = htmlLangEnabled;
    }

    public List<String> getDisabledRules() {
        return disabledRules;
    }

    public void setDisabledRules(List<String> disabledRules) {
        this.disabledRules = disabledRules == null ? new ArrayList<>() : new ArrayList<>(disabledRules);
    }

    void applyTo(LiveSessionConfig.Builder builder) {
        builder.validationMode(mode)
            .accessibilityValidationEnabled(accessibilityEnabled)
            .seoValidationEnabled(seoEnabled)
            .canonicalLinkValidationEnabled(canonicalEnabled)
            .openGraphValidationEnabled(openGraphEnabled)
            .htmlLangValidationEnabled(htmlLangEnabled);
        disabledRules.stream()
            .filter(ruleId -> ruleId != null && !ruleId.isBlank())
            .forEach(builder::disableValidationRule);
    }
}
