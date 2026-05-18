# SEO Validation

UJFE's SEO validation helpers inspect rendered HTML for basic document structure. They are not a crawler, Lighthouse replacement, sitemap generator, or structured-data validator.

Validation is opt-in and disabled by default. Use warnings while developing and strict validation in tests or CI when a page has a known SEO contract.

## Java API

```java
ValidationResult result = DocumentValidator.seo()
        .validate(documentHtml);

DocumentValidator.of(ValidationOptions.builder()
        .mode(ValidationMode.STRICT)
        .seoValidationEnabled(true)
        .canonicalLinkValidationEnabled(true)
        .openGraphValidationEnabled(true)
        .htmlLangValidationEnabled(true)
        .build())
        .validateOrThrow(documentHtml);
```

For live rendering:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .validationMode(ValidationMode.WARN)
        .seoValidationEnabled(true)
        .build();
```

## Rules

Initial SEO rule ids:

| Rule id | Check |
| --- | --- |
| `seo.document.title` | A non-empty `<title>` should exist. |
| `seo.document.meta-description` | A non-empty `<meta name="description" content="...">` should exist. |
| `seo.heading.single-h1` | Exactly one page `<h1>` should be present. |
| `seo.heading.hierarchy` | Obvious heading-level skips, such as `h1` to `h3`, are reported. |
| `seo.link.canonical` | Canonical links are validated when enabled. |
| `seo.meta.duplicate-title` | Duplicate `<title>` tags are reported. |
| `seo.meta.duplicate-description` | Duplicate meta descriptions are reported. |
| `seo.html.lang` | `<html lang="...">` is validated when enabled. |
| `seo.open-graph.basic` | `og:title` and `og:description` are validated when enabled. |
| `seo.img.alt` | Content images should have alt text or explicit decorative intent. |

## Title And Description

The live runtime renders a title from `LiveSessionConfig.title(...)`:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .title("Account settings")
        .head(meta()
                .attr("name", "description")
                .attr("content", "Manage profile, billing, and security settings."))
        .build();
```

The validator reports missing, empty, or duplicate title and description tags. It does not log the full document or request data.

## Headings

A page should generally have one `<h1>` that describes the page. Subsequent sections should follow a predictable hierarchy:

```java
main()
        .child(h1("Reports"))
        .child(section()
                .child(h2("Revenue"))
                .child(h3("Quarterly trend")));
```

Skipping directly from `h1` to `h3` is reported as `seo.heading.hierarchy`.

## Optional Canonical And Open Graph Checks

Canonical validation is off unless configured:

```java
ValidationOptions options = ValidationOptions.builder()
        .seoValidationEnabled(true)
        .canonicalLinkValidationEnabled(true)
        .build();
```

When enabled, the page should include:

```html
<link rel="canonical" href="https://example.test/page">
```

Open Graph validation is also opt-in because not every application needs social preview metadata. When enabled, UJFE checks for non-empty `og:title` and `og:description` tags.

## Strict SEO Validation

Use `STRICT` for pages whose SEO structure is part of the build contract. Use `WARN` for local development, documentation previews, and pages that are still evolving.
