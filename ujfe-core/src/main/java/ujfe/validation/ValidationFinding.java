package ujfe.validation;

import java.util.Objects;

public final class ValidationFinding {
    private final String ruleId;
    private final ValidationSeverity severity;
    private final ValidationCategory category;
    private final String message;
    private final String suggestion;
    private final String element;
    private final String attribute;
    private final String location;

    private ValidationFinding(Builder builder) {
        this.ruleId = requireText(builder.ruleId, "ruleId");
        this.severity = Objects.requireNonNull(builder.severity, "severity");
        this.category = Objects.requireNonNull(builder.category, "category");
        this.message = requireText(builder.message, "message");
        this.suggestion = clean(builder.suggestion);
        this.element = clean(builder.element);
        this.attribute = clean(builder.attribute);
        this.location = clean(builder.location);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String ruleId() {
        return ruleId;
    }

    public ValidationSeverity severity() {
        return severity;
    }

    public ValidationCategory category() {
        return category;
    }

    public String message() {
        return message;
    }

    public String suggestion() {
        return suggestion;
    }

    public String element() {
        return element;
    }

    public String attribute() {
        return attribute;
    }

    public String location() {
        return location;
    }

    public String format() {
        StringBuilder formatted = new StringBuilder()
            .append("rule=")
            .append(ruleId)
            .append(" severity=")
            .append(severity)
            .append(" category=")
            .append(category.value())
            .append(" message=")
            .append(message);
        append(formatted, "suggestion", suggestion);
        append(formatted, "element", element);
        append(formatted, "attribute", attribute);
        append(formatted, "location", location);
        return formatted.toString();
    }

    @Override
    public String toString() {
        return format();
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof ValidationFinding)) {
            return false;
        }
        ValidationFinding that = (ValidationFinding) candidate;
        return Objects.equals(ruleId, that.ruleId)
            && severity == that.severity
            && category == that.category
            && Objects.equals(message, that.message)
            && Objects.equals(suggestion, that.suggestion)
            && Objects.equals(element, that.element)
            && Objects.equals(attribute, that.attribute)
            && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, severity, category, message, suggestion, element, attribute, location);
    }

    private static void append(StringBuilder formatted, String name, String value) {
        if (value != null) {
            formatted.append(' ')
                .append(name)
                .append('=')
                .append(value);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public static final class Builder {
        private String ruleId;
        private ValidationSeverity severity;
        private ValidationCategory category;
        private String message;
        private String suggestion;
        private String element;
        private String attribute;
        private String location;

        private Builder() {
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder severity(ValidationSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder category(ValidationCategory category) {
            this.category = category;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        public Builder element(String element) {
            this.element = element;
            return this;
        }

        public Builder attribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public ValidationFinding build() {
            return new ValidationFinding(this);
        }
    }
}
