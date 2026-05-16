# Changelog

## Unreleased

## Version 0.7.0 - 16/05/2026

- Updated the Maven project version to `0.7.0-SNAPSHOT`.
- Introduced `UrlPolicy` for configurable URL scheme handling with secure defaults.
- Refactored `SafeUrl` to delegate scheme decisions to the active `UrlPolicy`.
- Blocked `javascript:` and `vbscript:` schemes unconditionally.
- Made `http:`, `mailto:`, and `tel:` schemes configurable through `UrlPolicy.builder()`.
- Added `background` to the set of URL-bearing attributes sanitized by `SafeUrl`.
- Allowed `data:image/svg+xml` and `data:image/avif` MIME types for data URLs.
- Documented the `data:image/*` MIME prefix validation limitation.
- Documented the SafeUrl policy in `docs/security/safe-url.md`.

## Version 0.6.0 - 16/05/2026

- Updated the Maven project version to `0.6.0-SNAPSHOT`.
- Hardened the generic attribute model with inline event handler blocking.
- Blocked `onclick`, `onload`, `onerror`, and all `on*` inline event handler attributes by default.
- Validated attribute names to reject whitespace, `<`, `=`, and other dangerous characters before rendering.
- Allowed `aria-*`, `data-*`, and `hx-*` attributes through the generic `attr(...)` API.
- Documented the attribute validation rules in `docs/html/attributes.md`.
- Documented the security rationale for inline event handler blocking in `docs/security/attribute-validation.md`.

## Version 0.5.0 - 16/05/2026

- Updated the Maven project version to `0.5.0-SNAPSHOT`.
- Added the explicit unsafe raw HTML APIs `unsafeHtml(...)` and `UnsafeHtml.of(...)`.
- Documented the XSS risk and acceptable use cases for trusted raw HTML rendering.
- Added tests proving safe text remains escaped and unsafe HTML renders raw content only through unsafe-named APIs.
- Refined the example documentation page with stronger security guidance and a cleaner visual structure.

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
