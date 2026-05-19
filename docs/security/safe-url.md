# Safe URL Policy

## Overview

UJFE sanitizes all URL-bearing HTML attributes through a centralized
`SafeUrl` sanitizer backed by a configurable `UrlPolicy`. This prevents
common injection vectors such as `javascript:` and `vbscript:` URIs
from reaching the rendered HTML output.

## Affected attributes

The following HTML attributes are routed through URL sanitization:

| Attribute      | Common elements                   |
|----------------|-----------------------------------|
| `href`         | `<a>`, `<link>`, `<base>`         |
| `src`          | `<img>`, `<script>`, `<source>`   |
| `action`       | `<form>`                          |
| `formaction`   | `<button>`, `<input>`             |
| `poster`       | `<video>`                         |
| `cite`         | `<blockquote>`, `<q>`             |
| `data`         | `<object>`                        |
| `background`   | `<td>`, `<body>` (legacy)         |

## Default policy

The default `UrlPolicy` is secure out of the box:

| Scheme          | Status            | Notes                                           |
|-----------------|-------------------|-------------------------------------------------|
| `https:`        | ✅ Allowed         | Always safe                                     |
| `http:`         | ❌ Blocked         | Opt-in via `allowHttp()`                        |
| `mailto:`       | ❌ Blocked         | Opt-in via `allowMailto()`                      |
| `tel:`          | ❌ Blocked         | Opt-in via `allowTel()`                         |
| `javascript:`   | 🚫 Always blocked | Cannot be allowed, even through configuration   |
| `vbscript:`     | 🚫 Always blocked | Cannot be allowed, even through configuration   |
| `data:image/*`  | ✅ Allowed         | Can be disabled via `allowDataImageUrls(false)` |
| `data:image/svg+xml` | ✅ Allowed by current default | Reviewed as a separate hardening follow-up because SVG is active markup, not a raster format |
| `data:*` (other)| ❌ Blocked         | Only image MIME types are allowed               |

### Relative references

Relative references are always allowed, regardless of policy:

- `/path` — absolute path
- `./path` — current directory relative
- `../path` — parent directory relative
- `#fragment` — fragment-only reference
- `path` — scheme-less path (no colon in value)

## Configuring the policy

Set the global default policy at application startup:

```java
UrlPolicy.setDefault(UrlPolicy.builder()
    .allowHttp()
    .allowMailto()
    .allowTel()
    .build());
```

The policy can also be passed explicitly to `SafeUrl.sanitize(value, policy)`.

### Builder API

| Method                          | Effect                                 |
|---------------------------------|----------------------------------------|
| `allowHttp()`                   | Allows `http:` URLs                    |
| `allowMailto()`                 | Allows `mailto:` URLs                  |
| `allowTel()`                    | Allows `tel:` URLs                     |
| `allowScheme(String)`           | Allows a custom scheme (not js/vbs)    |
| `allowDataImageUrls(boolean)`   | Toggles `data:image/*` URL support     |

## Blocked schemes

`javascript:` and `vbscript:` are unconditionally blocked. Attempting
to allow them through `allowScheme(...)` throws an
`IllegalArgumentException` at configuration time.

```java
// This throws IllegalArgumentException:
UrlPolicy.builder().allowScheme("javascript");
```

## Data URL handling

### Allowed image MIME types

When `data:image/*` URLs are enabled (the default), the following MIME
prefixes are accepted:

- `data:image/gif;`
- `data:image/png;`
- `data:image/jpeg;`
- `data:image/webp;`
- `data:image/svg+xml;`
- `data:image/avif;`
- `data:image/bmp;`

All other `data:` URLs are blocked, including `data:text/html` and
`data:application/javascript`.

### Known limitation

> [!WARNING]
> MIME prefix validation does not prove that decoded bytes are a valid
> image. A `data:image/gif;base64,...` URL could contain arbitrary bytes
> after decoding. Stricter validation such as image-byte inspection is
> an explicit future hardening option.

### SVG data URL hardening note

`data:image/svg+xml` remains allowed by the current default policy for
compatibility with existing image data URL handling. Treat SVG differently
from raster image formats during security review: SVG is XML markup and may
interact with browser behavior, embedding context, and CSP differently than
PNG, JPEG, GIF, or WebP.

Future hardening should split SVG data URL support from raster image data URL
support so applications can opt into SVG explicitly. Until then, do not pass
untrusted SVG data URLs to URL-bearing attributes without an application-level
sanitization decision.

## Examples

```java
// Allowed: relative paths
a("Home").href("/home");
a("Settings").href("../settings");
a("Section").href("#section");

// Allowed: HTTPS
a("Site").href("https://example.com");

// Blocked by default, opt-in available
a("Link").href("http://example.com");    // throws unless allowHttp()
a("Email").href("mailto:user@x.com");    // throws unless allowMailto()
a("Call").href("tel:+1234567890");        // throws unless allowTel()

// Always blocked
a("Bad").href("javascript:alert(1)");    // always throws
a("Bad").href("vbscript:MsgBox(1)");     // always throws
```

## Testing

All policy rules are covered by `SafeUrlPolicyTest`. See the test file
for the full acceptance criteria matrix.
