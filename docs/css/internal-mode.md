# Internal CSS Mode

`CssMode.INTERNAL` is the default.

In this mode UJFE:

- keeps `class` attributes in the HTML;
- collects classes used during server rendering;
- generates deterministic CSS for known utility classes;
- injects the generated stylesheet into the document head;
- serves generated CSS from `/_ujfe/css?classes=...` for dev tooling and runtime previews.

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .cssMode(CssMode.INTERNAL)
        .build();
```

Internal mode is useful for examples, prototypes, server-owned UIs, and environments that want a small framework-generated stylesheet without a Node.js build step.

Unknown classes remain in the HTML but do not produce internal CSS rules. That keeps the mode compatible with application-owned CSS layered on top of UJFE.
