# Signals Example

Run the standard examples application and open `/signals`:

```bash
./mvnw -pl examples -am exec:java -Dexec.mainClass=app.Main
```

Then open:

```text
http://localhost:8080/signals
```

The route demonstrates:

- mutable signal updates;
- basic computed values;
- nested computed values;
- cached reads;
- lazy recomputation after invalidation;
- subscriber behavior with collapsed invalidations.

## What To Try

1. Open `/signals`.
2. Click "Increment count". The nested computed graph invalidates and
   recomputes during the next render.
3. Click "Read computed twice". The first read may use or refresh the cache; the
   second read reuses the same cached value.
4. Click "Run subscriber sample". The sample invalidates a computed value
   multiple times and then reads it once, so the subscriber receives only the
   latest computed value.

For the complete signal semantics guide, see `../../docs/signals.md`.
