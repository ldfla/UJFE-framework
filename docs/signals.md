# UJFE Signals

UJFE signals are server-side reactive values for live rendering. They provide a
small Java API for mutable state, derived state, subscriptions, and deterministic
re-rendering.

The signal model is intentionally runtime-safe:

- mutable signals own writable state;
- computed signals derive read-only state;
- computed values are lazy by default;
- dependency tracking is automatic;
- cache invalidation is deterministic;
- failed computed evaluations never publish invalid values.

## Mutable Signals

Create mutable state with `Signals.signal(...)`:

```java
Signal<Integer> count = Signals.signal(0);

count.set(1);
count.update(value -> value + 1);

int current = count.get();
```

`set(...)` and `update(...)` notify subscribers only when the value actually
changes according to `Objects.equals(...)`.

```java
AutoCloseable subscription = count.subscribe(value -> {
    System.out.println("count = " + value);
});

count.set(2);
subscription.close();
```

Subscribers are synchronous Java callbacks. They run on the server, not in the
browser.

## Computed Signals

Create derived state with `Signals.computed(...)`:

```java
Signal<Integer> count = Signals.signal(1);
Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);

int value = doubled.get(); // 2
```

`Computed<T>` implements `Signal<T>`, but it is read-only. Calling `set(...)` or
`update(...)` on a computed signal throws `UnsupportedOperationException`.

## Lazy Evaluation

Computed signals are lazy by default.

```java
AtomicInteger evaluations = new AtomicInteger();

Computed<Integer> expensive = Signals.computed(() -> {
    evaluations.incrementAndGet();
    return runExpensiveOperation();
});

// evaluations == 0 here
Integer value = expensive.get();
// evaluations == 1 here
```

Creating a computed signal does not evaluate it. Evaluation starts only when
`get()` is called.

## Cache Semantics

Computed signals cache the latest successful evaluation.

```java
Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);

doubled.get(); // evaluates
doubled.get(); // reuses cache
doubled.get(); // reuses cache
```

Rules:

- consecutive `get()` calls without dependency changes reuse the cached value;
- dependency changes invalidate the cache;
- invalidation does not recompute immediately;
- the first `get()` after invalidation recomputes;
- successful recomputation replaces the cached value.

## Dependency Tracking

Dependencies are tracked automatically while a computed value is evaluated.

```java
Signal<Integer> count = Signals.signal(1);
Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);

doubled.get(); // tracks count as a dependency
count.set(2); // invalidates doubled
doubled.get(); // recomputes lazily
```

Dependencies are collected per evaluation. Dynamic dependency changes do not
leak old dependencies:

```java
Signal<Boolean> usePrimary = Signals.signal(true);
Signal<Integer> primary = Signals.signal(10);
Signal<Integer> secondary = Signals.signal(20);

Computed<Integer> selected = Signals.computed(() ->
        usePrimary.get() ? primary.get() : secondary.get());
```

After `usePrimary` changes to `false` and `selected.get()` recomputes, updates
to `primary` no longer invalidate `selected`.

## Subscriber Behavior

Computed subscribers are notified only after successful recomputation changes the
computed value.

```java
List<Integer> updates = new ArrayList<>();
doubled.subscribe(updates::add);

doubled.get();  // initial cache fill; no subscriber notification
count.set(2);  // invalidates only; no subscriber notification yet
doubled.get();  // recomputes and notifies if the value changed
```

Rules:

- invalidation alone does not notify computed subscribers;
- notification happens after successful recomputation;
- multiple invalidations before recomputation collapse into one recalculation;
- subscribers receive the latest computed value;
- subscribers are not notified if the recomputed value equals the cached value.

## Nested Computed Signals

Computed signals can depend on other computed signals:

```java
Signal<Integer> count = Signals.signal(2);
Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);
Computed<String> label = Signals.computed(() -> "Value: " + doubled.get());
```

When `count` changes:

```text
count invalidates doubled
doubled invalidates label
label recomputes lazily on next get()
```

Nested invalidation preserves deterministic ordering and avoids unnecessary
recalculation.

## Exception Behavior

If computed evaluation throws:

- the exception propagates to the caller of `get()`;
- the cache is not updated with invalid data;
- the previous successful cache remains available for comparison;
- subscribers do not receive invalid values;
- dependency tracking state is cleaned up.

Example:

```java
Signal<Integer> denominator = Signals.signal(1);
Computed<Integer> quotient = Signals.computed(() -> {
    int value = denominator.get();
    if (value == 0) {
        throw new IllegalStateException("division by zero");
    }
    return 10 / value;
});
```

If `denominator` changes to `0`, `quotient.get()` throws. A later successful
evaluation replaces the cache only after the supplier completes normally.

## Circular Dependency Behavior

Circular computed dependencies fail predictably with `ComputedCycleException`.

```java
AtomicReference<Computed<Integer>> first = new AtomicReference<>();
AtomicReference<Computed<Integer>> second = new AtomicReference<>();

first.set(Signals.computed(() -> second.get().get() + 1));
second.set(Signals.computed(() -> first.get().get() + 1));

first.get().get(); // throws ComputedCycleException
```

The runtime detects the cycle during evaluation and avoids unbounded recursive
recomputation.

## Thread Safety

Computed signals prioritize correctness over lock-free optimization.

Concurrent reads share a coherent cache entry. Concurrent invalidation and reads
do not corrupt dependency tracking or cache state. The implementation uses
synchronization internally; it is designed to be predictable under concurrent
server-side access.

## Reactive Rendering

In UJFE live pages, computed signals can be read inside render suppliers:

```java
public final class CounterPage implements Component {
    private final Signal<Integer> count = Signals.signal(0);
    private final Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);

    public Node render() {
        return div()
                .child(p(() -> "Count: " + count.get()))
                .child(p(() -> "Doubled: " + doubled.get()))
                .child(button("Increment").onClick(() -> count.update(value -> value + 1)));
    }
}
```

When the button event updates `count`, `doubled` is invalidated. It recomputes
lazily during the next render only if `doubled.get()` is reached.

## Performance Expectations

Computed signals avoid unnecessary recomputation:

- cache hits are reused;
- repeated dependency changes before access collapse;
- nested computed values propagate invalidation without recomputing immediately;
- dependencies that are no longer read are unsubscribed after successful
  recomputation.

Use computed signals for derived state that is expensive, repeated, or shared
across multiple render nodes.
