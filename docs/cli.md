# CLI

UJFE ships a small Java CLI for framework tooling. The first production command is `convert`, which migrates HTML files into UJFE Java DSL pages.

```bash
ujfe convert page.html --output src/main/java/app/pages/Page.java --type html
```

See [HTML converter](cli/convert.md) for options, conversion behavior, CSS extraction, unsafe fallback, and troubleshooting.
