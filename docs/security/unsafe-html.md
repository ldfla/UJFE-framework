# Unsafe HTML Escape Hatch

UJFE escapes text content by default. That remains the normal and recommended behavior:

```java
p("<script>");
```

renders:

```html
<p>&lt;script&gt;</p>
```

For rare integration cases, UJFE provides an intentionally unsafe escape hatch:

```java
unsafeHtml("<p>Trusted HTML</p>");
UnsafeHtml.of("<p>Trusted HTML</p>");
```

Both APIs render the provided HTML exactly as supplied. UJFE does not escape, sanitize, validate, or rewrite this content.

## When It Is Acceptable

Use `unsafeHtml(...)` only when all of these are true:

- The HTML comes from a trusted source.
- The content has already been sanitized or otherwise controlled before it reaches UJFE.
- The caller understands that the content can create executable markup, scripts, event attributes, iframes, forms, or links.
- The unsafe boundary is visible in code review because the API name contains `unsafe`.

Appropriate examples include trusted CMS fragments that are sanitized at ingestion, generated documentation snippets from a controlled build step, or server-owned markup that cannot contain user input.

## When It Is Not Acceptable

Do not use `unsafeHtml(...)` for ordinary page construction. Use `Element`, `UI` helpers, and text nodes instead.

Do not pass these values to `unsafeHtml(...)`:

- User input.
- Markdown output that has not been sanitized.
- Database content edited by untrusted users.
- Request parameters, headers, cookies, or local storage values.
- Third-party API responses unless they are explicitly trusted and sanitized.

## Security Consequence

This is expected behavior:

```java
unsafeHtml("<script>alert(1)</script>");
```

renders:

```html
<script>alert(1)</script>
```

That behavior is why the API is named `unsafeHtml(...)`. If the content is not trusted, use `text(...)`, `p(...)`, or another normal element API so UJFE escapes the value.
