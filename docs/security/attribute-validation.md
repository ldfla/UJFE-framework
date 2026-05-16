# Attribute Validation

## Overview

UJFE validates every attribute name before it is stored on an element.
This prevents malformed or dangerous attributes from reaching the
rendered HTML output.

## Validation rules

### Valid attribute names

A valid attribute name must:

1. Start with an ASCII letter (`A-Z` or `a-z`).
2. Contain only ASCII letters, digits, colons, underscores, or hyphens.

Any name that violates these rules throws an
`IllegalArgumentException` at construction time.

### Blocked characters

The following characters are explicitly rejected in attribute names:

| Character   | Reason                                |
|-------------|---------------------------------------|
| Whitespace  | Breaks HTML tokenization              |
| `<`         | Can start injected tags               |
| `>`         | Can close injected tags               |
| `=`         | Can break attribute value parsing     |
| `"`         | Can escape attribute value context    |
| `'`         | Can escape attribute value context    |

### Inline event handler attributes

Inline event handler attributes are **blocked by default**. Any
attribute whose name starts with `on` followed by an ASCII letter is
rejected. This includes, but is not limited to:

- `onclick`
- `onload`
- `onerror`
- `onmouseover`
- `onfocus`
- `onsubmit`

The check is case-insensitive: `onClick`, `ONCLICK`, and `OnLoad` are
all rejected.

#### Rationale

Inline event handler attributes embed JavaScript directly in the HTML
markup. In a server-rendered framework, allowing arbitrary inline
handlers would create XSS vulnerabilities, especially when attribute
values include user-controlled data.

UJFE live events use opaque server-side event identifiers registered
through `Element.on(event, handler)`. The server generates a unique
event ID, renders it as a `data-ujfe-event-*` attribute, and handles
the event callback entirely on the server. This design:

- Prevents client-side JavaScript injection.
- Keeps event logic in server-side Java code.
- Eliminates the need for inline JavaScript in rendered output.

#### Server-side event handling example

```java
// Correct: server-side live event
button("Save").on("click", () -> save());

// Incorrect: blocked inline handler
button("Save").attr("onclick", "save()"); // throws IllegalArgumentException
```

## Attribute value sanitization

Attribute values are escaped through `AttributeEscaper.escape(...)` at
render time. URL-valued attributes (`href`, `src`, `action`, etc.) are
additionally validated through `SafeUrl.sanitize(...)` to block
`javascript:` and dangerous `data:` URIs.

## Testing

All validation rules are covered by `AttributeValidationTest`:

- Standard attributes succeed.
- `aria-*`, `data-*`, and `hx-*` attributes succeed.
- Inline event handlers (`onclick`, `onload`, etc.) fail by default.
- Malformed names (whitespace, `<`, `=`) fail before rendering.
