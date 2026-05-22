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

Classes are registered through normal `.css(...)` calls:

```java
main()
        .css("min-h-screen bg-slate-50 text-slate-900 p-6")
        .child(h1("Dashboard").css("text-3xl font-bold text-primary-700"));
```

In `INTERNAL` mode, only classes observed during rendering generate CSS. In
`EXTERNAL` and `NONE`, classes still render into HTML unchanged.

## Internal Utility Renderer

The internal renderer is intentionally small and deterministic. It is suitable
for examples, server-rendered admin surfaces, prototypes, and projects that do
not need a separate CSS build step.

Supported groups include:

- layout: `grid`, `flex`, `inline-flex`, `flex-col`, `flex-wrap`,
  `items-center`, `items-start`, `items-stretch`, `justify-between`,
  `justify-center`, `justify-end`, `grid-cols-1` through `grid-cols-4`;
- framework example grids: `app-shell`, `demo-grid`, `docs-grid`,
  `catalog-grid`;
- spacing: `p-*`, `px-*`, `py-*`, `pt-*`, `pr-*`, `pb-*`, `pl-*`, `m-*`,
  `mx-*`, `my-*`, `mt-*`, `mr-*`, `mb-*`, `ml-*`, `gap-*`, `gap-x-*`,
  `gap-y-*`;
- typography: `text-xs` through `text-5xl`, `font-normal`, `font-medium`,
  `font-semibold`, `font-bold`, `font-extrabold`, `font-black`, `font-sans`,
  `font-mono`, `uppercase`, `italic`, `leading-none`, `leading-relaxed`;
- surfaces: `rounded`, `rounded-md`, `rounded-lg`, `rounded-full`, `border`,
  `border-t`, `border-b`, `shadow-sm`, `shadow-inner`, `ring-1`;
- responsive and state variants: `sm:`, `md:`, `lg:`, `hover:`, `focus:`,
  `active:`, `placeholder:`;
- static colors for common examples and theme-driven `primary` and
  `secondary` palettes.

Spacing utilities accept numeric scales where `1` equals `0.25rem`; for
example `p-4` emits `padding:1rem`. Negative margin utilities such as `-mt-2`
are supported for margin prefixes only.

Theme-driven classes include:

```text
bg-primary, bg-primary-50, bg-primary-500, bg-primary-950
text-secondary, text-secondary-700
border-primary-200
```

The available theme steps are `50, 100, 200, 300, 400, 500, 600, 700, 800,
900, 950`.

Unknown classes are left in the HTML and ignored by the internal renderer. That
lets application-owned CSS and design systems coexist with UJFE markup.

## External CSS And Assets

In `EXTERNAL` mode, configure stylesheet links through `LiveSessionConfig`:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/assets/app.css")
        .externalStylesheet("/assets/theme.css")
        .build();
```

UJFE renders regular `<link rel="stylesheet" href="...">` nodes. The host app
must serve those files. See [Static assets](static-assets.md).

## Dark Mode In Examples

The example app controls dark mode through `AppTheme.darkMode()`. Shared layout and page components should branch on that value or use theme-aware helpers for surfaces, text, borders, code blocks, links, and buttons.

Avoid unconditional light-only classes in example components that render inside the shared page shell. For example, a card should not always use `bg-white text-slate-900`; it should choose a dark branch such as `bg-slate-900 text-slate-100 border-slate-800` when dark mode is active. Code blocks should remain high contrast in both modes.

## Security And CSP

The default standalone CSP allows same-origin scripts and framework style output. In `EXTERNAL` mode, keep stylesheets same-origin or deliberately extend `style-src`. In `NONE` mode, UJFE emits no framework CSS and does not require `/_ujfe/css` for styling.

See also:

- [Internal CSS mode](css/internal-mode.md)
- [External CSS mode](css/external-mode.md)
- [None CSS mode](css/none-mode.md)
