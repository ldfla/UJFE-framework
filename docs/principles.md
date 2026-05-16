# UJFE Principles

UJFE is guided by a small set of product and technical principles. These principles define what the framework should optimize for and what it should avoid.

## HTML-first

UJFE renders real HTML. The generic element model is the source of HTML support, and helper methods are convenience APIs over that model.

Generated pages should use standard elements, attributes, forms, links, metadata, and document structure. Future HTML elements, custom elements, and Web Components should not require a new UJFE release just to be represented.

## Standards-first

UJFE follows Web Platform contracts. It should make the correct HTML path easy instead of replacing the platform with a closed component model.

Standards-first also means generated output should remain easy to inspect, test, validate, cache, crawl, and style with existing web tooling.

## Java-first

UJFE application code is authored in Java. Components, pages, routing, state, event handlers, and server integrations should feel natural in JVM applications.

JavaScript can be used internally by the runtime bridge, but normal UJFE application development should not require application authors to write browser-side JavaScript.

## Server-first

UJFE keeps rendering, state, and event handling on the server by default. Browser events are represented as opaque identifiers in HTML and resolved to Java handlers on the server.

Server-first does not mean server-exclusive. It means the default architecture keeps business logic, security checks, integration code, and state transitions in the JVM where Java teams already operate.

## Safe by default

UJFE must make safe rendering the default path:

- Text content is escaped.
- Attribute values are escaped.
- URL-bearing attributes are sanitized.
- Element and attribute names are validated.
- Event ids do not expose Java method names.
- Unsafe rendering APIs are explicit and visibly named as unsafe.

Unsafe behavior should require an intentional opt-in and should be easy to identify in code review.

## CSS agnostic

UJFE does not require a proprietary CSS system. Generated markup uses normal `class` attributes and can be styled with plain CSS, utility CSS, Tailwind, Bootstrap, CSS Modules, or enterprise design systems.

UJFE may provide optional server-side CSS utilities, but those utilities must not prevent teams from using external stylesheets or existing styling pipelines.

## No required Node.js toolchain

UJFE does not require Node.js, npm, TypeScript, Babel, or a bundler for normal application development.

Projects can still choose those tools for their own asset pipeline, but they are not prerequisites for rendering UJFE pages or handling live server-side interactions.

## Non-goals

- UJFE is not a browser-side JavaScript framework clone.
- UJFE is not a proprietary HTML dialect.
- UJFE is not a mandatory CSS framework.
- UJFE is not a replacement for Spring Boot or Spring MVC.
