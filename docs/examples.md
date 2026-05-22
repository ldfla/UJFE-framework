# Example Application

The example application is a runnable reference for UJFE behavior. It should help a new developer understand how pages, routing, live events, signals, lifecycle behavior, CSS modes, and runtime extension points fit together.

## Pages

- Home demonstrates the Java DSL, live server events, browser state access, modal/toast flows, and safe static asset routing behavior.
- Documentation maps the main framework concepts: `@Page`, `Router`, CSS modes, static assets, runtime cache intent, live endpoints, runtime lifecycle, security defaults, REST usage, and CLI conversion.
- Signals demonstrates mutable `Signal<T>` state, lazy `Computed<T>` values, dependency invalidation, caching, and subscribers.
- Lifecycle demonstrates mount, event-driven re-render, route-transition cleanup, and resource release patterns for server-side components.
- Forms demonstrates typed form controls and live submit/input behavior.
- Runtime Actions demonstrates server-side hooks for render, event, error, and head contribution phases.

## LLM Reference

The project-level reference for code-generating agents is [LLM framework reference](llm-reference.md). Keep it synchronized when public framework APIs, CSS behavior, asset behavior, runtime adapters, or security defaults change.

## Styling

Examples must work in light and dark modes. Use `AppTheme.darkMode()` or shared component helpers for cards, panels, code blocks, borders, navigation, buttons, and muted text. Do not hard-code light-only backgrounds such as `bg-white` or light-only text such as `text-slate-900` in a component that can render in dark mode unless the class is selected only for the light branch.

In `CssMode.EXTERNAL`, examples should still render semantic class names that an application stylesheet can target. In `CssMode.NONE`, pages may be unstyled, but markup should remain semantic and readable.

## Testing

Example tests should render pages directly and assert stable contracts:

- expected headings and developer-oriented explanations;
- `<main>` or other semantic structure;
- current API names in snippets;
- theme-compatible dark-mode classes;
- no real external API calls.

Avoid exact full-HTML snapshots unless the page has a small, stable rendering contract.
