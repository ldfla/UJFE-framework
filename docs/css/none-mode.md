# None CSS Mode

`CssMode.NONE` disables framework CSS output completely.

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .cssMode(CssMode.NONE)
        .build();
```

In this mode UJFE:

- keeps `class` attributes in the HTML;
- does not emit `<style data-ujfe-css>`;
- does not render `externalStylesheet(...)` links;
- returns an empty response from `/_ujfe/css`.

Use `NONE` for tests, plain semantic HTML, embedded rendering, or applications where CSS is delivered entirely outside the UJFE runtime.
