# External CSS Mode

`CssMode.EXTERNAL` disables UJFE's internal stylesheet and renders configured stylesheet links.

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/assets/app.css")
        .externalStylesheet("/assets/theme.css")
        .build();
```

In this mode UJFE:

- keeps `class` attributes in the HTML;
- does not emit `<style data-ujfe-css>`;
- returns an empty response from `/_ujfe/css`;
- renders configured stylesheet links in registration order.

Use this mode with Tailwind, Bootstrap, CSS files, CSS Modules, proprietary design systems, or any other application-owned CSS pipeline.

For the standalone Netty runtime, same-origin stylesheets work with the default CSP. CDN stylesheets require explicit CSP changes through `SecurityHeadersConfig`.
