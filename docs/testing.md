# Testing And CI

Use UJFE validation helpers in tests when rendered HTML structure is part of the contract. Runtime validation remains off by default, so existing applications do not fail unless validation is explicitly enabled.

## Coverage

`mvn clean verify` runs the full test suite, generates JaCoCo reports, and enforces the module-level coverage gates used by CI. The GitHub workflow also runs `./mvnw -B verify --file pom.xml`, so a local coverage failure is expected to fail pull requests in the same way.

Module reports are written to:

```text
<module>/target/site/jacoco/index.html
```

The aggregate report is written to:

```text
target/site/jacoco-aggregate/index.html
```

The build prints each module report path during `verify`.

Current line coverage thresholds:

| Module | Minimum |
| --- | ---: |
| `ujfe-core` | 90% |
| `ujfe-signals` | 85% |
| `ujfe-router` | 80% |
| `ujfe-live` | 75% |
| `ujfe-http` | 70% |

Additional modules currently inherit the default coverage check and generate reports, but they do not set a non-zero gate until their API surface stabilizes enough for a meaningful baseline. This includes adapter/example/CLI modules where coverage is still being shaped by integration behavior and example-only rendering code. Do not exclude production code to pass a threshold. Acceptable exclusions should be narrow, documented in the POM near the JaCoCo configuration, and limited to generated code, build metadata, pure constants-only classes, CLI launcher boilerplate, or explicitly example-only code.

When coverage fails, open the module report, identify untested behavior that matters, and add deterministic tests. Prefer behavior assertions over tests that only execute lines. Tests must not call external services, depend on current dates, require credentials, or rely on machine-specific paths.

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
