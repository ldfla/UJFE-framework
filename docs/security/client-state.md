# Client State Security

Client state synchronization can expose sensitive browser data if it is too broad. UJFE therefore treats browser state as private by default and only synchronizes explicitly allowed keys.

## Default Behavior

The default policy is deny-all:

- no cookies are sent;
- no `localStorage` keys are sent;
- no `sessionStorage` keys are sent.

Applications opt in with `ClientStatePolicy` or the corresponding `LiveSessionConfig` builder methods.

## Privacy Rules

Do not allow keys that contain:

- authentication tokens;
- CSRF tokens;
- session identifiers intended to remain server-only;
- refresh tokens;
- personal data that is not required for rendering;
- secrets copied into browser storage by another library.

Prefer small, purpose-specific keys such as `ujfe.theme` over broad application storage namespaces.

## Cookies

UJFE can only read cookies visible to browser JavaScript. `HttpOnly` cookies are intentionally unavailable to `document.cookie` and should remain that way for authentication/session cookies.

Initial HTTP page requests may include normal `Cookie` headers, but UJFE still filters them through `ClientStatePolicy` before exposing values through `Ujfe.cookie(...)`.

## Storage Keys

The client bridge does not enumerate all browser storage. It reads only keys rendered into the document by the server policy:

- `data-ujfe-client-state-cookies`;
- `data-ujfe-local-storage-keys`;
- `data-ujfe-session-storage-keys`.

The server applies the same policy again after JSON parsing. This protects against manually crafted requests that include unauthorized keys.

## Spring Security

Spring Security-managed cookies, CSRF tokens, remember-me tokens, and authorization data should not be added to `ClientStatePolicy`.

When UJFE runs behind Spring Security:

- keep authentication cookies `HttpOnly`;
- keep CSRF handling configured for the application;
- use UJFE client state only for non-sensitive UI preferences or routing hints;
- prefer server-side security context APIs for identity and authorization.

## Safe Usage Pattern

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .allowClientCookie("ujfe_demo")
        .allowLocalStorageKey("ujfe.theme")
        .allowSessionStorageKey("ujfe.tab")
        .build();
```

This permits only the named values. Everything else is ignored by the browser bridge and filtered server-side if a request attempts to include it.
