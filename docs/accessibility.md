# Accessibility Validation

UJFE renders normal HTML and keeps accessibility choices in application code. The optional validation helpers help catch common semantic mistakes during tests, CI, documentation builds, examples, or development rendering.

Validation is disabled by default. Enabling it does not change the HTML renderer; it inspects the rendered output and reports findings.

## Modes

| Mode | Behavior |
| --- | --- |
| `OFF` | Does not run validation. This is the default runtime behavior. |
| `WARN` | Runs validation and reports findings without failing rendering. |
| `STRICT` | Runs validation and throws `ValidationException` when findings are present. |

## Java API

```java
ValidationResult result = DocumentValidator.accessibility()
        .validate(documentHtml);

DocumentValidator.of(ValidationOptions.builder()
        .mode(ValidationMode.STRICT)
        .accessibilityValidationEnabled(true)
        .build())
        .validateOrThrow(documentHtml);
```

For live rendering:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .validationMode(ValidationMode.WARN)
        .accessibilityValidationEnabled(true)
        .build();
```

## Rules

Initial accessibility rule ids:

| Rule id | Check |
| --- | --- |
| `accessibility.document.single-main` | Exactly one `<main>` landmark should be present. |
| `accessibility.heading.single-h1` | Exactly one page `<h1>` should be present. |
| `accessibility.img.alt` | Images need meaningful `alt` text or explicit decorative intent. |
| `accessibility.button.accessible-name` | Buttons need visible text, `aria-label`, or valid `aria-labelledby`. |
| `accessibility.input.label` | Form inputs need a label or accessible name. Hidden inputs are ignored. |
| `accessibility.id.duplicate` | Duplicate `id` attributes are reported. |
| `accessibility.aria-labelledby.target` | `aria-labelledby` ids must point to existing elements. |
| `accessibility.aria-describedby.target` | `aria-describedby` ids must point to existing elements. |
| `accessibility.interactive.accessible-name` | Interactive elements such as links need accessible names. |
| `accessibility.interactive.nested` | Obvious nested interactive controls are reported. |

## Common Fixes

Use one primary landmark and heading:

```java
main()
        .child(h1("Account settings"))
        .child(section().child(h2("Profile")));
```

Give content images meaningful alt text:

```java
img().src("/charts/revenue.png")
        .alt("Quarterly revenue chart");
```

Decorative images must be explicit. Bare `alt=""` is not treated as valid by itself:

```java
img().src("/decorative-divider.png")
        .alt("")
        .role("presentation");
```

Label buttons with text or ARIA:

```java
button("Save");
button().ariaLabel("Close dialog");
```

Label inputs with a normal `<label>`, wrapping label, or ARIA name:

```java
label("Email").forId("email");
inputEmail().id("email");

label().child("Accept terms")
        .child(checkbox().name("terms"));

inputSearch().ariaLabel("Search documentation");
```

## Tests

Strict validation is intended for tests and CI:

```java
assertDoesNotThrow(() -> DocumentValidator.of(ValidationOptions.builder()
        .mode(ValidationMode.STRICT)
        .accessibilityValidationEnabled(true)
        .build())
        .validateOrThrow(documentHtml));
```

Disable a rule only when the exception is intentional and documented:

```java
ValidationOptions options = ValidationOptions.builder()
        .accessibilityValidationEnabled(true)
        .disableRule(AccessibilityValidator.IMG_ALT)
        .build();
```
