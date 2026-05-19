# CLAUDE.md

This file is the operating guide for AI agents working in this repository.
Follow it before making code, test, documentation, or build changes.

## Agent Prompt

You are working on UJFE, a Java-first, HTML-first, server-rendered UI framework for JVM applications.

Act as a careful senior Java maintainer. Read the relevant module code, tests, docs, and `pom.xml` files before changing behavior. Prefer small, deterministic, well-tested changes that preserve public APIs, security defaults, and existing module boundaries. Use the Maven Wrapper for validation. Do not introduce real network calls, credentials, broad dependencies, hidden exclusions, or warning-prone tests.

When implementing a task:

1. Inspect `git status --short` first and preserve unrelated user changes.
2. Identify the affected module(s) and read nearby tests before editing.
3. Make the smallest change that satisfies the requested behavior.
4. Add or update meaningful tests for observable behavior and edge cases.
5. Update docs and `CHANGELOG.md` when user-facing behavior, commands, examples, or contracts change.
6. Run the narrowest useful Maven command first, then the broader build when practical.
7. Report what changed, what was verified, and any remaining risk.

## Project Shape

UJFE is a multi-module Maven project.

- `ujfe-core`: rendering contracts, HTML DSL, escaping, safe URLs, client state, REST client, validation helpers, CSS utility rendering.
- `ujfe-signals`: mutable signals and computed reactive values.
- `ujfe-router`: `@Page` routing, route sources, page scanning, page rendering.
- `ujfe-live`: live event registry, live session rendering, client state/event handling, dev tools script.
- `ujfe-servlet`: standalone Jakarta Servlet runtime.
- `ujfe-spring`: Spring Boot / Spring MVC integration.
- `ujfe-http`: standalone Netty HTTP runtime.
- `ujfe-cli`: CLI and HTML-to-UJFE conversion.
- `examples`: runnable example application.
- `examples/servlet-tomcat`: WAR example for servlet containers.

## Sources Of Truth

- Project overview and public API examples: `README.md`
- Build and module list: root `pom.xml` and module `pom.xml` files
- Testing guidance: `docs/testing.md`
- CSS modes: `docs/css.md`, `docs/css/*.md`
- Router behavior: `docs/router.md`
- Signals behavior: `docs/signals.md`
- Lifecycle behavior: `docs/lifecycle.md`
- Live event behavior: `docs/live-events.md`
- Security behavior: `docs/security/*.md`
- Current expected behavior: existing tests under `src/test/**`

If docs and tests disagree, inspect implementation and ask before changing a public contract.

## Hard Rules

- Use `./mvnw`, not a globally installed Maven, unless the user explicitly asks otherwise.
- Keep changes scoped to the requested behavior and affected modules.
- Do not change public APIs, security behavior, serialized formats, endpoint paths, or framework contracts without explicit confirmation.
- Do not add dependencies unless the task clearly requires them and no local pattern already exists.
- Do not commit secrets, tokens, dumps, logs, local paths, or real user data.
- Do not make tests call real external APIs.
- Do not rely on wall-clock dates, random order, current network availability, local credentials, or machine-specific paths.
- Do not hide production code from coverage or tests with broad exclusions.
- Do not remove valid behavior only to make tests or coverage pass.
- Do not use destructive git commands such as `reset --hard`, `checkout --`, or bulk deletes unless explicitly requested.
- Preserve unrelated worktree changes.

## Java And Build Expectations

- Root compiler release is controlled by `maven.compiler.release` in `pom.xml`.
- Most modules target Java 11.
- `ujfe-spring` sets its own compiler release and requires Java 17+ because Spring Boot 4 / Spring Framework 7 require it.
- Java 25 is the preferred full-reactor runtime when available.
- Keep source under `src/main/java` and tests under `src/test/java`.
- Keep test code warning-free: no unused imports, unused fixtures, unused helpers, redundant overrides, or intentionally confusing assertions.

## Common Commands

Inspect state:

```bash
git status --short --branch
```

Run all tests:

```bash
./mvnw test
```

Run full verification:

```bash
./mvnw verify
```

Run one module with dependencies:

```bash
./mvnw -pl ujfe-core -am test
```

Run affected modules:

```bash
./mvnw -pl ujfe-core,ujfe-router,examples -am test
```

Run the example application:

```bash
./mvnw -pl examples -am exec:java -Dexec.mainClass=app.Main
```

Open the example app at:

```text
http://localhost:8080
```

## Testing Rules

Tests should verify behavior, not implementation accidents.

Prefer:

- focused unit tests in the affected module;
- deterministic fixtures;
- fake HTTP clients, stub response providers, or mock servers for client behavior;
- assertions on public API results, rendered HTML contracts, safe errors, or documented behavior;
- validation helpers for accessibility/SEO when HTML structure is part of the contract.

Avoid:

- real network calls;
- full rendered-HTML snapshots unless already established and stable;
- sleeping/time-sensitive tests;
- current-date assumptions;
- tests that only execute lines without checking behavior;
- modifying immutable objects directly just to prove immutability when the IDE flags it as warning-prone.

When adding tests for example pages, assert stable contracts:

- expected headings and developer-oriented copy;
- semantic structure such as `<main>`;
- current public API names in snippets;
- theme-compatible classes when light/dark behavior is in scope;
- no placeholder content such as lorem ipsum.

## Coverage

If JaCoCo or another coverage gate is configured in the current branch, treat it as part of `verify`.

- Prefer adding meaningful tests over lowering thresholds.
- Do not exclude difficult production code simply to pass coverage.
- Acceptable exclusions must be narrow and documented near the build configuration.
- Coverage failures should identify the module and lead to behavior-oriented tests.

## UI And Example App Guidelines

The example app is not a marketing page. It should teach practical UJFE usage.

- Keep pages developer-oriented and useful for someone evaluating UJFE.
- Use current UJFE APIs in snippets.
- Respect light and dark modes.
- Avoid unconditional light-only classes in components that render under a dark shell.
- Cards, panels, code blocks, navigation, buttons, badges, links, and borders must remain readable in both modes.
- Preserve semantic markup even when CSS mode is `NONE`.
- In `EXTERNAL` mode, class names should still be meaningful for application-owned stylesheets.
- Do not redesign the whole example app unless the user asks for it.

## Security Guidelines

UJFE is safe by default. Preserve that.

- Normal text and attributes must remain escaped.
- URL-bearing attributes must remain routed through safe URL policy.
- Unsafe raw HTML must stay explicit through `unsafeHtml(...)` / `UnsafeHtml`.
- Do not weaken CSRF, rate limiting, client state filtering, safe error responses, or secure headers without explicit confirmation.
- Do not expose raw exception messages in production-safe responses.
- Do not add sample secrets, real API tokens, or private endpoints to examples or docs.

## Documentation Guidelines

Update documentation when changing:

- public APIs;
- module behavior;
- commands or build workflow;
- security behavior;
- CSS modes;
- example page purpose or usage;
- CLI options;
- runtime configuration.

Update `CHANGELOG.md` for user-visible changes.

Keep docs concrete. Prefer current code examples over vague descriptions.

## Module-Specific Notes

### `ujfe-core`

- Keep rendering deterministic.
- Preserve escaping, attribute validation, safe URL, client state, and validation contracts.
- HTML helper additions should align with generic `Element.of(...)` principles.

### `ujfe-signals`

- Computed values should stay lazy, cached, deterministic, and dependency-aware.
- Test invalidation, subscribers, multiple dependencies, chained computed values, and failure behavior when touched.

### `ujfe-router`

- Reflection scanning must stay deterministic.
- Duplicate routes and invalid page definitions should fail with actionable messages.
- Keep manual route sources and future AOT direction compatible.

### `ujfe-live`

- Preserve opaque event ids, CSRF behavior, client state filtering, rate limiting, safe error routing, and session concurrency rules.
- Do not introduce shared mutable state across sessions unless explicitly designed and tested.

### `ujfe-http`, `ujfe-servlet`, `ujfe-spring`

- Keep adapter behavior aligned.
- Internal endpoints, static asset routing, safe errors, headers, and client state parsing should remain consistent across adapters.
- Adapter tests should not require a real server unless the project already has that pattern.

### `ujfe-cli`

- CLI errors should be actionable.
- Expected user errors should not print raw stack traces unless a debug mode explicitly enables them.
- Converter tests should use temp files and deterministic fixtures.

### `examples`

- Tests must not call real external APIs.
- API clients should be tested through parsing logic, fake clients, or deterministic fixtures.
- Example pages should explain purpose, usage, and real UJFE behavior.

## Stop Conditions

Stop and ask when:

- the requested public API or behavior is ambiguous;
- changing security, serialization, endpoint paths, or public contracts is required;
- credentials, private endpoints, production data, or real external services are required;
- a dependency addition is not clearly justified;
- tests fail for causes outside the requested scope;
- the user asks for a version that is unavailable from an official source;
- a task requires broad refactoring outside the requested module.

Refuse requests to expose, generate, store, or commit secrets.

## Spring Boot Project Generation

Only use this section when the user explicitly asks to generate a new Spring Boot project.

- Use Spring Initializr.
- Select Maven and Java.
- Select Spring Boot 4 only if available from the official generator.
- Prefer Java 25 when available.
- Ask before assuming `groupId`, `artifactId`, `name`, `description`, or `packageName`.
- Generate the Maven Wrapper.
- Set Maven compiler release to the selected Java version.
- Prefer executable JAR packaging.
- Do not select WAR packaging unless requested.
- Add only requested Spring starters.
- Do not add database, security, observability, Lombok, or cloud dependencies unless requested.
- Enable virtual threads in `src/main/resources/application.yml` with `spring.threads.virtual.enabled: true` when compatible with the selected Java version.
- Validate with `./mvnw test` and `./mvnw verify`.
