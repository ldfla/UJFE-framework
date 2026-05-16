# Generic HTML Elements

`Element.of(String tagName)` is the compatibility foundation for UJFE HTML rendering. Helpers such as `div()`, `section()`, `dialog()`, `svg()`, and `math()` are optional syntax sugar over the same generic element model.

The generic model is intentionally not a whitelist of known HTML tags. It supports current HTML elements, future HTML elements, custom elements, Web Components, SVG, and MathML without requiring a new helper method or UJFE release.

## Standard and Future Elements

Use `Element.of(...)` for any valid current or future HTML tag name:

```java
Element.of("section")
        .child("Standard HTML");

Element.of("future-html-element")
        .attr("data-ready", true);
```

Both examples render normal HTML:

```html
<section>Standard HTML</section>
<future-html-element data-ready></future-html-element>
```

## Custom Elements and Web Components

Custom elements should follow the Web Components naming expectation of containing a hyphen:

```java
Element.of("my-card")
        .attr("data-state", "ready")
        .child("Card content");
```

For code that wants to document the intent explicitly, `Element.custom(...)` validates the hyphen rule:

```java
Element.custom("my-card");
```

## Validation

UJFE validates tag names before rendering. Tag names must start with an ASCII letter and may contain ASCII letters, digits, hyphens, and underscores.

These names are rejected:

```java
Element.of("");
Element.of(" ");
Element.of("<script>");
Element.of("div onclick=alert(1)");
Element.of("bad tag");
```

The validation rejects whitespace, angle brackets, quotes, equals signs, and other characters that can change markup structure.

## SVG and MathML

SVG and MathML use controlled namespace factories. The serialized output is still normal HTML, but the element metadata records the intended namespace:

```java
Element.of("svg")
        .attr("viewBox", "0 0 10 10")
        .child(Element.svg("path").attr("d", "M0 0h10v10H0z"));

Element.of("math")
        .child(Element.mathMl("mi").child("x"))
        .child(Element.mathMl("mo").child("="))
        .child(Element.mathMl("mn").child("1"));
```

The root `svg()` and `math()` helpers are convenience methods for the same model. Specialized SVG and MathML helpers can be added later without changing the generic compatibility path.

## Helper Policy

Helpers must never be required for standards compatibility. When a helper does not exist, use `Element.of(...)`, `Element.svg(...)`, or `Element.mathMl(...)` directly.
