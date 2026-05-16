# Standard HTML Helpers

UJFE provides helper methods for the common HTML surface through `ujfe.html.UI`. These helpers are convenience factories; the compatibility foundation remains `Element.of(...)`.

Every helper returns an `Element`, so behavior is consistent across tags:

```java
section()
        .child(h1("Dashboard"))
        .child(p("Escaped text"))
        .child(Element.of("future-html-element").attr("data-ready", true));
```

Helpers with text overloads render text nodes, so content is escaped the same way as `text(...)`:

```java
p("<script>")
```

renders:

```html
<p>&lt;script&gt;</p>
```

## Helper Coverage

| Group | Helpers |
| --- | --- |
| Document structure | `html()`, `head()`, `body()`, `title()`, `meta()`, `link()`, `style()`, `script()`, `base()` |
| Semantic layout | `main()`, `section()`, `article()`, `aside()`, `header()`, `footer()`, `nav()`, `address()` |
| Text | `h1()`, `h2()`, `h3()`, `h4()`, `h5()`, `h6()`, `p()`, `span()`, `strong()`, `em()`, `small()`, `mark()`, `abbr()`, `cite()`, `code()`, `pre()`, `blockquote()`, `q()`, `br()`, `hr()` |
| Grouping | `div()`, `figure()`, `figcaption()`, `details()`, `summary()`, `dialog()` |
| Lists | `ul()`, `ol()`, `li()`, `dl()`, `dt()`, `dd()` |
| Navigation | `a()` |
| Media and embedded content | `img()`, `picture()`, `source()`, `audio()`, `video()`, `track()`, `canvas()`, `svg()`, `map()`, `area()`, `iframe()`, `embed()`, `object()`, `param()` |
| Tables | `table()`, `thead()`, `tbody()`, `tfoot()`, `tr()`, `td()`, `th()`, `caption()`, `colgroup()`, `col()` |
| Forms | `form()`, `input()`, `textarea()`, `button()`, `select()`, `option()`, `optgroup()`, `label()`, `fieldset()`, `legend()`, `datalist()`, `output()`, `progress()`, `meter()` |
| Templates and Web Components | `template()`, `slot()` |

## Rendering Rules

Void helpers render without closing tags:

```java
img().src("/logo.png").alt("Logo");
br();
input().attr("required", true);
```

Normal helpers render opening and closing tags:

```java
section().child(p("Content"));
```

renders:

```html
<section><p>Content</p></section>
```

## Helper Policy

Helpers must delegate to `Element.of(...)` or an equivalent centralized element creation path. They must not introduce a whitelist that blocks valid HTML, future HTML, custom elements, SVG, or MathML.

When a helper is missing or too specific for an application need, use the generic API:

```java
Element.of("my-card");
Element.of("future-html-element");
Element.svg("path");
Element.mathMl("mi");
```
