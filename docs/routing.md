# Runtime Routing

UJFE runtime routing distinguishes framework endpoints, static assets, and page routes.

## Request Order

1. Internal UJFE endpoints:
   - `/_ujfe/client.js`
   - `/_ujfe/dev.js`
   - `/_ujfe/css`
   - `/_ujfe/event`
   - `/_ujfe/state`
2. Static asset-like paths.
3. Registered page routes.
4. Safe `404 Not Found`.

This prevents normal browser asset requests from becoming UJFE page render failures.

## Missing Pages

Missing page routes return a safe UJFE error response with `UJFE_ROUTE_NOT_FOUND`. The response does not expose stack traces or internal exception messages.

## Missing Assets

Missing static assets return a normal text `404` body:

```text
Static asset not found.
```

They do not call `LiveSession.renderDocument(...)`, do not mount components, do not mutate live session state, and do not trigger severe route error logs.
