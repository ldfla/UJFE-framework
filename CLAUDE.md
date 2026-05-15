# Hard Rules

- Follow `README.md`, `pom.xml`, module `pom.xml` files, and existing tests.
- Keep changes scoped to the requested module and behavior.
- Do not guess requirements, versions, credentials, endpoints, or business rules.
- Do not change public APIs, security behavior, schemas, or external contracts without confirmation.
- Do not add dependencies unless the task requires them.
- Do not commit secrets, tokens, dumps, logs, or real user data.
- Prefer small, testable, reversible changes.

# Authority & Links

- Project guide: `README.md`
- Build authority: `pom.xml`
- Source modules: `ujfe-*`
- Example app: `examples`
- Tests: `src/test/**`
- Maven: https://maven.apache.org/guides/
- Spring Initializr: https://start.spring.io/
- Spring Boot: https://docs.spring.io/spring-boot/
- Java 25: https://docs.oracle.com/en/java/javase/25/

# Setup / Test

- Use the Maven Wrapper.
- Current repository build target is defined by `maven.compiler.release`.
- Run tests before finalizing code changes.
- Report unrelated failing tests without hiding them.

# Workflow

- `git status --short`
- `./mvnw test`
- `./mvnw verify`
- `./mvnw -pl examples -am exec:java -Dexec.mainClass=app.Main`

# Stop Conditions

- Stop and ask when `groupId`, `artifactId`, package name, or project purpose is missing.
- Stop before modifying authentication, authorization, cryptography, persistence schemas, or migrations.
- Stop when credentials, external access, or production data are required.
- Refuse requests to expose, generate, store, or commit secrets.
- Stop when a requested version is not available from an official source.
- Stop when required tests fail for causes outside the requested scope.

# New Spring Boot 4 Project Generation With Maven

- Use Spring Initializr.
- Select Maven.
- Select Java.
- Select Spring Boot 4.
- Prefer Java 25 LTS.
- Stop if Java 25 or Spring Boot 4 is unavailable from the generator.
- Ask before assuming `groupId`, `artifactId`, `name`, `description`, or `packageName`.
- Generate the Maven Wrapper.
- Set the Maven compiler release to `25`.
- Prefer executable JAR packaging.
- Do not select WAR packaging unless requested.
- Add only requested Spring starters.
- Do not add database, security, observability, Lombok, or cloud dependencies unless requested.
- Enable virtual threads in `src/main/resources/application.yml` with `spring.threads.virtual.enabled: true`.
- Keep application code under `src/main/java`.
- Keep tests under `src/test/java`.
- Validate with `./mvnw test`.
- Validate with `./mvnw verify`.
