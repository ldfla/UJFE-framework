# Attributes

## Generic attribute model

The generic `attr(name, value)` method is the primary API for setting
attributes on any element. It supports current HTML attributes, future
HTML attributes, `aria-*`, `data-*`, `hx-*`, and any other attribute
that follows valid attribute naming rules.

```java
Element.of("input")
    .attr("placeholder", "Name")
    .attr("required", true)
    .attr("aria-label", "Full name")
    .attr("data-id", "123")
    .attr("hx-get", "/fragment");
```

Typed helpers such as `.placeholder(...)`, `.aria(...)`, and `.data(...)`
are optional sugar over `attr(...)`. Applications can always use
`attr(name, value)` directly for any valid attribute.

## Attribute name validation

Attribute names are validated before they are stored. A valid attribute
name must start with an ASCII letter and may contain ASCII letters,
digits, colons, underscores, or hyphens.

Names that contain whitespace, `<`, `>`, `=`, `"`, or `'` are rejected
with an `IllegalArgumentException` at construction time, before the
element is rendered.

## Boolean attributes

Boolean attributes can be toggled using `attr(name, boolean)` or
`boolAttr(name, boolean)`. When `true`, the attribute is rendered as a
valueless attribute (e.g., `required`, `disabled`). When `false`, the
attribute is omitted entirely.

```java
input().attr("required", true);   // <input required>
input().attr("disabled", false);  // <input>
```

## Inline event handler attributes

Inline event handler attributes (`onclick`, `onload`, `onerror`, and
all other `on*` attributes) are **blocked by default**. Attempting to
set an inline event handler through `attr(...)` throws an
`IllegalArgumentException`.

```java
// This throws IllegalArgumentException:
div().attr("onclick", "alert(1)");
```

This policy exists because UJFE live events use opaque server-side
event identifiers instead of inline JavaScript handlers. Use
`Element.on(event, handler)` for server-driven event handling.

See [Attribute Validation](../security/attribute-validation.md) for
the full security rationale.

## Supported attribute prefixes

| Prefix    | Description                              | Example                          |
|-----------|------------------------------------------|----------------------------------|
| `aria-*`  | Accessible Rich Internet Applications    | `attr("aria-label", "Close")`    |
| `data-*`  | Custom data attributes                   | `attr("data-id", "123")`         |
| `hx-*`    | htmx progressive enhancement attributes | `attr("hx-get", "/fragment")`    |

All prefixed attributes pass through the same name validation rules as
standard attributes and are rendered identically.
