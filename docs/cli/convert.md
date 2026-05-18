# HTML To UJFE Converter

`ujfe convert` reads an HTML file and writes a Java page that uses the UJFE DSL. It is intended for static pages, prototypes, documentation pages, forms, tables, and server-rendered layouts.

The converter treats input HTML as untrusted. It does not execute scripts, does not fetch remote assets, and does not use `unsafeHtml(...)` unless explicitly configured.

## Basic Usage

```bash
ujfe convert page.html \
  --output src/main/java/app/pages/Page.java \
  --type html
```

Common options:

| Option | Behavior |
| --- | --- |
| `--output`, `--out` | Java page file to write. Parent directories are created. |
| `--class-name GeneratedPage` | Generated Java class name. |
| `--package app.pages` | Package name. If omitted, `src/main/java/...` output paths infer it. |
| `--encoding UTF-8` | Input file encoding. Invalid or unsupported encodings fail with an actionable error. |
| `--comments drop\|preserve\|unsafe-fallback` | Comment handling policy. Default is `drop`. |
| `--unsafe-fallback` | Allows malformed unsupported input to emit explicit `unsafeHtml(...)`. |
| `--css none\|extract\|external` | CSS migration mode. Default is `none`. |
| `--css-class-name GeneratedStyles` | Class name for extracted CSS. |
| `--componentize` | Extracts direct page sections into private render methods for larger pages. |
| `--safe-class-name` | Converts invalid class names into deterministic fallback names. |
| `--debug` | Shows stack traces for unexpected CLI failures. |

CLI output reports the input file, output file, generated class, package, converted element count, preserved attribute count, warning count, unsafe fallback usage, CSS migration result, and component method count.

## Conversion Behavior

Supported common tags include document structure, semantic layout, text, links, images, lists, forms, tables, media tags, and custom elements.

Known UJFE helpers are emitted through helper calls:

```java
section()
        .attr("class", "p-4")
        .child(h1().attr("title", "Hero").child(text("Hello")));
```

Unknown and custom tags use the generic API:

```java
element("my-widget")
        .attr("data-id", "123")
        .child(span().child(text("Ready")));
```

Attributes are preserved unless conversion fails. This includes `id`, `class`, `style`, `href`, `src`, `alt`, `title`, `role`, `aria-*`, `data-*`, form attributes, table attributes, media attributes, and unknown attributes.

Classes are preserved exactly as written. The converter does not reorder utility classes and does not require UJFE internal CSS mode.

Boolean attributes are emitted as presence booleans:

```java
input()
        .attr("required", true)
        .attr("disabled", true);
```

Void elements such as `br`, `hr`, `img`, `input`, `meta`, `link`, `source`, and `wbr` are not given generated children. HTML-compatible self-closing syntax such as `<input />` is accepted.

`<!DOCTYPE html>` is recognized and ignored because UJFE page render methods return page nodes, not raw document declarations.

Entities are decoded before Java generation and Java strings are escaped again. For example, `Tom &amp; Jerry` becomes `text("Tom & Jerry")`.

## Comments

Default comment policy is `drop`, so comments are not copied into generated code.

`preserve` currently fails with an actionable error because UJFE does not expose a safe comment node API.

`unsafe-fallback` emits comments through explicit `unsafeHtml(...)`:

```java
unsafeHtml("<!-- TODO -->")
```

Do not use unsafe comment fallback for sensitive comments.

## Malformed HTML

Malformed nesting, unexpected closing tags, unclosed tags, unterminated comments, and broken attributes fail by default:

```text
Malformed HTML: Unexpected closing tag </div>; expected </span>.
```

Use `--unsafe-fallback` only when you intentionally want the original fragment emitted through `unsafeHtml(...)` for later manual audit.

## CSS Migration

CSS migration is disabled by default.

```bash
ujfe convert page.html \
  --output src/main/java/app/pages/Page.java \
  --css extract \
  --css-class-name GeneratedStyles
```

`--css extract` moves inline `<style>` blocks into one generated Java class:

```java
public final class GeneratedStyles {
    public static String css() {
        return "...";
    }
}
```

The generated page includes a `styles()` method returning `GeneratedStyles.css()`. Relative local `<link rel="stylesheet" href="...">` files are extracted when readable and inside the input file directory. Remote, absolute, unreadable, or unsafe stylesheet paths are preserved and reported as warnings.

Generated CSS is independent of UJFE CSS modes. Use it with `CssMode.EXTERNAL` by writing it into your application stylesheet pipeline, or with custom runtime hooks. `CssMode.INTERNAL` is not required.

## Componentization

`--componentize` keeps larger pages readable by extracting direct page sections into deterministic private methods such as `renderHeader()`, `renderSection()`, `renderForm()`, `renderTable()`, and `renderFooter()`.

Small fragments are left inline to avoid unnecessary methods.

## Errors

Expected user-facing errors do not print stack traces unless `--debug` is present.

Examples:

```text
Input file not found: ./page.html
Input path is a directory, expected an HTML file: ./pages
Input file is empty: ./page.html
Unsupported encoding: NO_SUCH_ENCODING
Output path is a directory, expected a Java file: ./generated
Invalid Java class name: 123Page. Use --class-name with a valid Java class name or --safe-class-name.
```

## Examples

Form input:

```html
<form method="post" action="/signup">
  <label for="email">Email</label>
  <input id="email" name="email" type="email" required>
  <button type="submit">Save</button>
</form>
```

Table input:

```html
<table class="prices">
  <caption>Prices</caption>
  <thead><tr><th scope="col">Plan</th></tr></thead>
  <tbody><tr><td colspan="2">Pro</td></tr></tbody>
</table>
```

Custom element:

```html
<my-widget data-id="123"><span>Ready</span></my-widget>
```
