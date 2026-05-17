# Lifecycle Example

Run the standard examples application and open `/lifecycle`:

```bash
./mvnw -pl examples -am exec:java -Dexec.mainClass=app.Main
```

Then open:

```text
http://localhost:8080/lifecycle
```

The route demonstrates server-side lifecycle behavior:

- page mount and unmount callbacks;
- nested component lifecycle tracking through `component(...)`;
- stable re-render behavior for stateful live components;
- cleanup patterns for server-owned resources;
- route transition cleanup when leaving `/lifecycle`.

## What To Try

1. Open `/lifecycle`.
2. Click "Refresh lifecycle view". The page re-renders, but the mounted page and
   nested resource component should not mount again.
3. Click "Touch stateful component". The nested component state updates while
   preserving the same lifecycle instance.
4. Navigate to another route, such as `/docs` or `/runtime-actions`. The
   lifecycle page and nested resource component are unmounted.
5. Return to `/lifecycle`. New page and component instances enter the tree and
   mount again.

Lifecycle callbacks run on the server. They are not browser-side hooks, do not
access the DOM, and do not require JavaScript lifecycle code.

For the complete lifecycle guide, see `../../docs/lifecycle.md`.
