# Testing And CI

Use UJFE validation helpers in tests when rendered HTML structure is part of the contract. Runtime validation remains off by default, so existing applications do not fail unless validation is explicitly enabled.

## Strict Validation

Strict validation is the recommended CI profile for pages that should meet accessibility or SEO structure rules:

```java
@Test
void pageMarkupPassesValidation() {
    String documentHtml = renderPage();

    DocumentValidator.of(ValidationOptions.builder()
            .mode(ValidationMode.STRICT)
            .accessibilityValidationEnabled(true)
            .seoValidationEnabled(true)
            .build())
            .validateOrThrow(documentHtml);
}
```

`ValidationException` includes the validation findings, rule ids, messages, suggestions, element names, attributes, and selector-like locations.

## Warning Mode

Use warning mode during development or example rendering:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .validationMode(ValidationMode.WARN)
        .accessibilityValidationEnabled(true)
        .seoValidationEnabled(true)
        .build();
```

Warning mode logs findings and continues rendering.

## Disabling Rules

Disable individual rules only for documented exceptions:

```java
ValidationOptions options = ValidationOptions.builder()
        .mode(ValidationMode.STRICT)
        .accessibilityValidationEnabled(true)
        .disableRule(AccessibilityValidator.INTERACTIVE_NESTED)
        .build();
```

Prefer fixing markup over disabling rules. For CI, keep the disabled-rule list small and local to the test or route that needs it.
