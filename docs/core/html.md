# Core HTML APIs

The canonical HTML element model lives in `ujfe-core` and uses the `ujfe.core` package.

```java
import ujfe.core.Element;
import ujfe.core.Node;

import static ujfe.core.UI.*;
```

## Canonical Surface

- `ujfe.core.Element` is the generic HTML element model.
- `ujfe.core.UI` contains helper factories such as `div()`, `main()`, `button()`, `svg()`, and `math()`.
- `Element.of(...)` remains the compatibility foundation for current HTML, future HTML, custom elements, Web Components, SVG, and MathML entry points.
- `ujfe.core.TextNode` escapes text by default.
- `ujfe.core.UnsafeHtml` and `unsafeHtml(...)` are the explicit raw HTML escape hatch.
- `ujfe.core.HtmlElementMetadata` centralizes void element metadata.
- `ujfe.core.SafeUrl` and `UrlPolicy` enforce safe URL attribute behavior.

Use only `ujfe-core` for rendering contracts and HTML helpers:

```xml
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-core</artifactId>
    <version>${ujfe.version}</version>
</dependency>
```
