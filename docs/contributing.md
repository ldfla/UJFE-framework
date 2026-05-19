# Contributing

Run the full verification build before opening a pull request:

```bash
./mvnw clean verify
```

This compiles every module, runs unit tests, generates JaCoCo module reports, creates the aggregate coverage report, and enforces configured coverage thresholds. CI runs the same Maven `verify` lifecycle through the project Maven wrapper, so local verification should use `./mvnw` instead of a globally installed Maven.

Coverage reports are local HTML files under each module:

```text
ujfe-core/target/site/jacoco/index.html
ujfe-signals/target/site/jacoco/index.html
ujfe-router/target/site/jacoco/index.html
ujfe-live/target/site/jacoco/index.html
ujfe-http/target/site/jacoco/index.html
```

The aggregate report is at `target/site/jacoco-aggregate/index.html`.

Coverage thresholds are intentionally baselines, not a target to game. Add tests for observable behavior, error handling, edge cases, and deterministic contracts. If code is excluded from coverage, document the reason with the build configuration and keep the exclusion narrow.

When adding example pages, keep them practical and testable:

- explain what the page demonstrates and why it matters in a real UJFE app;
- use current public APIs in snippets;
- support light and dark modes through shared theme-aware classes or design tokens;
- avoid real network calls in tests;
- add render tests for headings, semantic structure, meaningful copy, and theme-compatible classes.
