# HTML Void Elements

UJFE renders HTML void elements according to the Web Platform rule: they have a start tag only and must not have children or an end tag.

## Void Element List

The centralized void element metadata is exposed through `ujfe.html.HtmlElementMetadata`:

```java
HtmlElementMetadata.isVoidElement("img");
HtmlElementMetadata.voidElements();
```

The standard HTML void elements are:

| Element | Element | Element | Element |
| --- | --- | --- | --- |
| `area` | `base` | `br` | `col` |
| `embed` | `hr` | `img` | `input` |
| `link` | `meta` | `param` | `source` |
| `track` | `wbr` | | |

The metadata is reusable by the renderer, conformance checks, and CLI conversion.

## Rendering

Void elements render without closing tags:

```java
img().src("/logo.png").alt("Logo");
```

renders:

```html
<img src="/logo.png" alt="Logo">
```

Generic creation follows the same rule:

```java
Element.of("wbr");
```

renders:

```html
<wbr>
```

## Children

UJFE fails immediately when code tries to add a child to an HTML void element:

```java
img().child("Logo");
```

throws an `IllegalStateException`.

The renderer also keeps a defensive validation path so a void element with children cannot silently drop content.

## Normal And Future Elements

Known non-void elements and unknown future elements keep normal element behavior:

```java
div().child(p("Content"));

Element.of("future-html-element")
        .attr("data-ready", true);
```

render:

```html
<div><p>Content</p></div>
<future-html-element data-ready></future-html-element>
```

SVG and MathML tags use their controlled namespaces and do not inherit HTML void rules just because their local tag name matches an HTML void element.
