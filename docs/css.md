# CSS Modes

UJFE is CSS agnostic. The live runtime preserves HTML `class` attributes in every mode and lets the application choose how styles are produced.

## Modes

| Mode | Behavior | `/_ujfe/css` | Head output |
| --- | --- | --- | --- |
| `INTERNAL` | Collects classes during render and emits UJFE generated CSS. | Returns generated CSS for requested classes. | Adds `<style data-ujfe-css>...</style>`. |
| `EXTERNAL` | Does not generate UJFE CSS. Use application-owned stylesheets. | Returns an empty stylesheet. | Renders configured `externalStylesheet(...)` links. |
| `NONE` | Does not generate or link CSS. | Returns an empty stylesheet. | Does not render internal CSS or configured external stylesheet links. |

## Configuration

```java
LiveSessionConfig internal = LiveSessionConfig.builder()
        .cssMode(CssMode.INTERNAL)
        .build();

LiveSessionConfig external = LiveSessionConfig.builder()
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/assets/app.css")
        .build();

LiveSessionConfig none = LiveSessionConfig.builder()
        .cssMode(CssMode.NONE)
        .build();
```

Use `INTERNAL` for the bundled utility renderer, `EXTERNAL` for Tailwind, Bootstrap, design systems, CSS files, or CDN stylesheets, and `NONE` for tests, embedded rendering, or fully custom styling.

## Dark Mode In Examples

The example app controls dark mode through `AppTheme.darkMode()`. Shared layout and page components should branch on that value or use theme-aware helpers for surfaces, text, borders, code blocks, links, and buttons.

Avoid unconditional light-only classes in example components that render inside the shared page shell. For example, a card should not always use `bg-white text-slate-900`; it should choose a dark branch such as `bg-slate-900 text-slate-100 border-slate-800` when dark mode is active. Code blocks should remain high contrast in both modes.

## Security And CSP

The default standalone CSP allows same-origin scripts and framework style output. In `EXTERNAL` mode, keep stylesheets same-origin or deliberately extend `style-src`. In `NONE` mode, UJFE emits no framework CSS and does not require `/_ujfe/css` for styling.

See also:

- [Internal CSS mode](css/internal-mode.md)
- [External CSS mode](css/external-mode.md)
- [None CSS mode](css/none-mode.md)
