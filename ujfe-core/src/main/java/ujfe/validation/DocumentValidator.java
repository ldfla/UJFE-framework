package ujfe.validation;

import ujfe.core.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DocumentValidator {
    private final ValidationOptions options;

    private DocumentValidator(ValidationOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    public static DocumentValidator of(ValidationOptions options) {
        return new DocumentValidator(options);
    }

    public static DocumentValidator off() {
        return of(ValidationOptions.off());
    }

    public static DocumentValidator accessibility() {
        return of(ValidationOptions.accessibility());
    }

    public static DocumentValidator seo() {
        return of(ValidationOptions.seo());
    }

    public static DocumentValidator recommended() {
        return of(ValidationOptions.recommended());
    }

    public static DocumentValidator strict() {
        return of(ValidationOptions.strict());
    }

    public ValidationOptions options() {
        return options;
    }

    public ValidationResult validate(Node node) {
        Objects.requireNonNull(node, "node");
        return validate(node.render());
    }

    public ValidationResult validate(String html) {
        Objects.requireNonNull(html, "html");
        if (options.mode() == ValidationMode.OFF) {
            return ValidationResult.empty();
        }

        ValidationDocument document = HtmlValidationParser.parse(html);
        List<ValidationFinding> findings = new ArrayList<>();
        if (options.accessibilityValidationEnabled()) {
            new AccessibilityValidator(options).validate(document, findings);
        }
        if (options.seoValidationEnabled()) {
            new SeoValidator(options).validate(document, findings);
        }
        return ValidationResult.of(findings);
    }

    public void validateOrThrow(Node node) {
        Objects.requireNonNull(node, "node");
        validateOrThrow(node.render());
    }

    public void validateOrThrow(String html) {
        ValidationResult result = validate(html);
        if (options.mode() == ValidationMode.STRICT) {
            result.throwIfInvalid();
        } else {
            result.throwIfErrors();
        }
    }
}
