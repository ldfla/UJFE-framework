package ujfe.validation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class ValidationOptions {
    private final ValidationMode mode;
    private final boolean accessibilityValidationEnabled;
    private final boolean seoValidationEnabled;
    private final boolean canonicalLinkValidationEnabled;
    private final boolean openGraphValidationEnabled;
    private final boolean htmlLangValidationEnabled;
    private final Set<String> disabledRuleIds;
    private final Set<String> strictRuleIds;

    private ValidationOptions(Builder builder) {
        this.mode = Objects.requireNonNull(builder.mode, "mode");
        this.accessibilityValidationEnabled = builder.accessibilityValidationEnabled;
        this.seoValidationEnabled = builder.seoValidationEnabled;
        this.canonicalLinkValidationEnabled = builder.canonicalLinkValidationEnabled;
        this.openGraphValidationEnabled = builder.openGraphValidationEnabled;
        this.htmlLangValidationEnabled = builder.htmlLangValidationEnabled;
        this.disabledRuleIds = Set.copyOf(builder.disabledRuleIds);
        this.strictRuleIds = Set.copyOf(builder.strictRuleIds);
    }

    public static ValidationOptions off() {
        return builder()
            .mode(ValidationMode.OFF)
            .accessibilityValidationEnabled(false)
            .seoValidationEnabled(false)
            .build();
    }

    public static ValidationOptions accessibility() {
        return builder()
            .accessibilityValidationEnabled(true)
            .seoValidationEnabled(false)
            .build();
    }

    public static ValidationOptions seo() {
        return builder()
            .accessibilityValidationEnabled(false)
            .seoValidationEnabled(true)
            .build();
    }

    public static ValidationOptions recommended() {
        return builder()
            .accessibilityValidationEnabled(true)
            .seoValidationEnabled(true)
            .build();
    }

    public static ValidationOptions strict() {
        return builder()
            .mode(ValidationMode.STRICT)
            .accessibilityValidationEnabled(true)
            .seoValidationEnabled(true)
            .canonicalLinkValidationEnabled(true)
            .openGraphValidationEnabled(true)
            .htmlLangValidationEnabled(true)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder()
            .mode(mode)
            .accessibilityValidationEnabled(accessibilityValidationEnabled)
            .seoValidationEnabled(seoValidationEnabled)
            .canonicalLinkValidationEnabled(canonicalLinkValidationEnabled)
            .openGraphValidationEnabled(openGraphValidationEnabled)
            .htmlLangValidationEnabled(htmlLangValidationEnabled)
            .disabledRules(disabledRuleIds)
            .strictRules(strictRuleIds);
    }

    public ValidationMode mode() {
        return mode;
    }

    public boolean accessibilityValidationEnabled() {
        return accessibilityValidationEnabled;
    }

    public boolean seoValidationEnabled() {
        return seoValidationEnabled;
    }

    public boolean canonicalLinkValidationEnabled() {
        return canonicalLinkValidationEnabled;
    }

    public boolean openGraphValidationEnabled() {
        return openGraphValidationEnabled;
    }

    public boolean htmlLangValidationEnabled() {
        return htmlLangValidationEnabled;
    }

    public Set<String> disabledRuleIds() {
        return disabledRuleIds;
    }

    public Set<String> strictRuleIds() {
        return strictRuleIds;
    }

    public boolean isRuleEnabled(String ruleId) {
        return !disabledRuleIds.contains(Objects.requireNonNull(ruleId, "ruleId"));
    }

    public ValidationSeverity severityFor(String ruleId) {
        Objects.requireNonNull(ruleId, "ruleId");
        if (mode == ValidationMode.STRICT || strictRuleIds.contains(ruleId)) {
            return ValidationSeverity.ERROR;
        }
        return ValidationSeverity.WARN;
    }

    public static final class Builder {
        private ValidationMode mode = ValidationMode.WARN;
        private boolean accessibilityValidationEnabled;
        private boolean seoValidationEnabled;
        private boolean canonicalLinkValidationEnabled;
        private boolean openGraphValidationEnabled;
        private boolean htmlLangValidationEnabled;
        private final Set<String> disabledRuleIds = new LinkedHashSet<>();
        private final Set<String> strictRuleIds = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder mode(ValidationMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder accessibilityValidationEnabled(boolean enabled) {
            this.accessibilityValidationEnabled = enabled;
            return this;
        }

        public Builder seoValidationEnabled(boolean enabled) {
            this.seoValidationEnabled = enabled;
            return this;
        }

        public Builder canonicalLinkValidationEnabled(boolean enabled) {
            this.canonicalLinkValidationEnabled = enabled;
            return this;
        }

        public Builder openGraphValidationEnabled(boolean enabled) {
            this.openGraphValidationEnabled = enabled;
            return this;
        }

        public Builder htmlLangValidationEnabled(boolean enabled) {
            this.htmlLangValidationEnabled = enabled;
            return this;
        }

        public Builder disableRule(String ruleId) {
            this.disabledRuleIds.add(requireRuleId(ruleId));
            return this;
        }

        public Builder disabledRules(Collection<String> ruleIds) {
            Objects.requireNonNull(ruleIds, "ruleIds");
            ruleIds.forEach(this::disableRule);
            return this;
        }

        public Builder strictRule(String ruleId) {
            this.strictRuleIds.add(requireRuleId(ruleId));
            return this;
        }

        public Builder strictRules(Collection<String> ruleIds) {
            Objects.requireNonNull(ruleIds, "ruleIds");
            ruleIds.forEach(this::strictRule);
            return this;
        }

        public ValidationOptions build() {
            return new ValidationOptions(this);
        }

        private static String requireRuleId(String ruleId) {
            Objects.requireNonNull(ruleId, "ruleId");
            if (ruleId.isBlank()) {
                throw new IllegalArgumentException("ruleId must not be blank");
            }
            return ruleId;
        }
    }
}
