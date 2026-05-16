# Changelog

## Unreleased

## Version 0.4.0 - 16/05/2026

- Updated the Maven project version to `0.4.0-SNAPSHOT`.
- Added reusable `HtmlElementMetadata` for standard HTML void element behavior.
- Rendered HTML void elements without closing tags and rejected children with an explicit error.
- Reused centralized void element metadata in the CLI HTML parser.
- Documented HTML void element rendering rules in `docs/html/void-elements.md`.

## Version 0.3.0 - 16/05/2024

- Updated the Maven project version to `0.3.0-SNAPSHOT`.
- Added standard HTML helper coverage tests for required tags, void rendering, normal closing tags, text escaping, and nested children.
- Documented the standard helper API in `docs/html/HELPERS.md` and the README helper table.
- Updated Netty to `4.1.132.Final` to keep the HTTP runtime on the current non-vulnerable Netty line.
- Removed empty-string snippet concatenation patterns from example pages.

## Version 0.2.0 - 16/05/2024

- Updated the Maven project version to `0.2.0-SNAPSHOT`.
- Added centralized HTML tag-name validation for `Element.of(...)`.
- Added generic SVG and MathML namespace factories through `Element.svg(...)` and `Element.mathMl(...)`.
- Documented generic element compatibility for current HTML, future HTML, custom elements, Web Components, SVG, and MathML.

## Version 0.1.0 - 16/05/2024
- Consolidated the UJFE project identity around the tagline "Modern Reactive UI Framework for the JVM".
- Documented UJFE's core principles: HTML-first, standards-first, Java-first, server-first, safe by default, CSS agnostic, and no required Node.js toolchain.
- Documented UJFE's non-goals in the README, vision, and principles documentation.
- Aligned root project objectives and Maven metadata with the UJFE technical name and tagline.
