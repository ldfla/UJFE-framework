# Client State Synchronization

UJFE can synchronize selected browser state with the JVM during live rendering. This is explicit and policy-driven: nothing from cookies, `localStorage`, or `sessionStorage` is exposed to application code unless it is allowed in `ClientStatePolicy`.

## What Can Be Synced

Supported state sources:

- Cookies visible to `document.cookie`.
- Explicitly allowed `localStorage` keys.
- Explicitly allowed `sessionStorage` keys.

The browser bridge reads only configured keys and sends them inside the live JSON `clientState` payload. The server filters the payload again before storing it in `LiveSession`, so manually crafted requests cannot bypass the policy.

## Configuration

Programmatic setup:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .allowClientCookie("ujfe_demo")
        .allowLocalStorageKey("ujfe.theme")
        .allowSessionStorageKey("ujfe.tab")
        .build();
```

Equivalent explicit policy:

```java
ClientStatePolicy policy = ClientStatePolicy.builder()
        .allowCookie("ujfe_demo")
        .allowLocalStorageKey("ujfe.theme")
        .allowSessionStorageKey("ujfe.tab")
        .build();

LiveSessionConfig config = LiveSessionConfig.builder()
        .clientStatePolicy(policy)
        .build();
```

By default, `ClientStatePolicy.denyAll()` is used.

## Reading State

Allowed state is available during render and event handlers:

```java
p(() -> "Cookie: " + Ujfe.cookie("ujfe_demo").orElse("not sent"));
p(() -> "Theme: " + Ujfe.localStorage("ujfe.theme").orElse("not found"));
p(() -> "Tab: " + Ujfe.sessionStorage("ujfe.tab").orElse("not found"));
```

Live events and `/_ujfe/state` updates both pass through the same policy. If a key is not allowed, `Ujfe.localStorage(...)` or `Ujfe.sessionStorage(...)` returns `Optional.empty()`.

## Servlet Properties

Plain Servlet deployments can configure the same allowlists through properties:

```properties
ujfe.client-state.cookies=ujfe_demo
ujfe.client-state.local-storage-keys=ujfe.theme
ujfe.client-state.session-storage-keys=ujfe.tab
```

These names match the Servlet example at `examples/servlet-tomcat/src/main/resources/application.properties`.

| Property | Meaning |
| --- | --- |
| `ujfe.client-state.cookies` | Comma-separated cookie names exposed through `Ujfe.cookie(...)` |
| `ujfe.client-state.local-storage-keys` | Comma-separated `localStorage` keys exposed through `Ujfe.localStorage(...)` |
| `ujfe.client-state.session-storage-keys` | Comma-separated `sessionStorage` keys exposed through `Ujfe.sessionStorage(...)` |

YAML uses the same property paths:

```yaml
ujfe:
  client-state:
    cookies: ujfe_demo
    local-storage-keys: ujfe.theme
    session-storage-keys: ujfe.tab
```

## Spring Boot

Spring Boot applications can use configuration properties:

```yaml
ujfe:
  client-state:
    cookies:
      - ujfe_demo
    local-storage-keys:
      - ujfe.theme
    session-storage-keys:
      - ujfe.tab
```

Applications with their own `LiveSessionConfig` bean can configure `ClientStatePolicy` directly.

## Merge Semantics

`ClientState.merge(...)` overlays cookies, local storage, and session storage onto the existing state.

`ClientState.mergeCookiesAndReplaceLocalStorage(...)` keeps previously known cookies and replaces browser storage with the latest allowed snapshot. Despite the historical method name, browser storage means both local storage and session storage. UJFE uses this for live browser synchronization because storage updates represent the current client-side snapshot, not a partial patch.

## Example

The example app allows:

- cookie: `ujfe_demo`;
- local storage key: `ujfe.theme`;
- session storage key: `ujfe.tab`.

To test it in the browser console:

```javascript
document.cookie = "ujfe_demo=active";
localStorage.setItem("ujfe.theme", "dark");
sessionStorage.setItem("ujfe.tab", "docs");
```

Then click **Read browser state** in the example page.
