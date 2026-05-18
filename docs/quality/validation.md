# Validation Helpers

UJFE provides optional rendered-document validation in `ujfe-core`. The validators are adapter-independent: Netty, Servlet, Spring, and test code all use the same `DocumentValidator` API.

Validation inspects rendered HTML. It does not execute JavaScript, crawl multiple pages, compute a browser accessibility tree, or guarantee WCAG compliance.

## Core Types

- `DocumentValidator`: entry point for validating rendered HTML or a `Node`.
- `ValidationOptions`: typed configuration for modes, enabled validators, optional rules, disabled rules, and strict rule overrides.
- `ValidationMode`: `OFF`, `WARN`, or `STRICT`.
- `ValidationResult`: immutable list of deduplicated findings.
- `ValidationFinding`: rule id, severity, category, message, suggestion, element, attribute, and selector-like location.
- `ValidationException`: thrown by strict validation helpers.
- `AccessibilityValidator` and `SeoValidator`: focused validators with stable rule id constants.

## Modes

| Mode | Runtime behavior |
| --- | --- |
| `OFF` | No validation runs and no warnings are logged. |
| `WARN` | Validation runs, rendering continues, and runtime integration logs structured warnings. |
| `STRICT` | Validation runs and any finding fails validation with `ValidationException`. |

Runtime validation is `OFF` by default.

## Recommended API

```java
ValidationResult result = DocumentValidator.recommended()
        .validate(documentHtml);

DocumentValidator.strict()
        .validateOrThrow(documentHtml);
```

For targeted validation:

```java
ValidationOptions options = ValidationOptions.builder()
        .mode(ValidationMode.STRICT)
        .accessibilityValidationEnabled(true)
        .seoValidationEnabled(false)
        .disableRule(AccessibilityValidator.INTERACTIVE_NESTED)
        .build();

DocumentValidator.of(options).validateOrThrow(documentHtml);
```

To make one warning rule strict while staying in warning mode:

```java
ValidationOptions options = ValidationOptions.builder()
        .mode(ValidationMode.WARN)
        .accessibilityValidationEnabled(true)
        .strictRule(AccessibilityValidator.IMG_ALT)
        .build();
```

## Live Runtime

`LiveSessionConfig` exposes runtime validation without making it mandatory:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .validationMode(ValidationMode.WARN)
        .accessibilityValidationEnabled(true)
        .seoValidationEnabled(true)
        .build();
```

`WARN` logs structured findings with safe fields: mode, rule id, severity, category, path, element, attribute, location, and message. It does not log rendered document bodies, cookies, tokens, headers, or request bodies.

`STRICT` throws `ValidationException` during document rendering and is most useful in tests, CI, examples, and documentation builds.

## Servlet And Spring Configuration

Standalone Servlet properties:

```properties
ujfe.validation.mode=WARN
ujfe.validation.accessibility.enabled=true
ujfe.validation.seo.enabled=true
ujfe.validation.canonical.enabled=false
ujfe.validation.open-graph.enabled=false
ujfe.validation.html-lang.enabled=false
ujfe.validation.disabled-rules=seo.img.alt
```

Spring Boot configuration can use a `LiveSessionConfig` bean or the validation properties:

```yaml
ujfe:
  validation:
    mode: warn
    accessibility-enabled: true
    seo-enabled: true
    canonical-enabled: false
    open-graph-enabled: false
    html-lang-enabled: false
    disabled-rules:
      - seo.img.alt
```

## Findings

Findings are designed to be actionable without exposing sensitive runtime data:

```text
rule=accessibility.img.alt
severity=WARN
category=accessibility
message=Image is missing meaningful alt text or explicit decorative intent.
suggestion=Add alt text that describes the image, or mark it decorative when appropriate.
element=img
attribute=alt
location=html:nth-of-type(1) > body:nth-of-type(1) > main:nth-of-type(1) > img:nth-of-type(1)
```

Repeated identical findings are deduplicated in `ValidationResult`.
