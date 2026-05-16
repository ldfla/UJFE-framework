# Raw HTML

UJFE is safe by default: text nodes and helper text overloads escape content before rendering.

```java
p("<strong>x</strong>");
```

renders:

```html
<p>&lt;strong&gt;x&lt;/strong&gt;</p>
```

Raw HTML is available only through the explicit unsafe API:

```java
unsafeHtml("<strong>x</strong>");
```

renders:

```html
<strong>x</strong>
```

The object API is also intentionally named:

```java
UnsafeHtml.of("<strong>x</strong>");
```

There is no neutral raw HTML helper. The unsafe name is part of the API contract so code review can find unsafe rendering boundaries.

## Preferred Pattern

Prefer normal element construction:

```java
strong("x");
section().child(p("Content"));
```

Use `unsafeHtml(...)` only for trusted pre-rendered HTML where escaping would be incorrect and sanitization happens before the value reaches UJFE.

See [Unsafe HTML Escape Hatch](../security/unsafe-html.md) for the security rules.
