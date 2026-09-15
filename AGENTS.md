# Project instructions

## Overview

- `Diagram Maker` is a Java 21 / Spring Boot 4.1 application.
- The build uses Maven; always use the checked-in `./mvnw` wrapper.
- The frontend uses Thymeleaf with Webpack and Tailwind CSS.
- MapStruct owns DTO/entity mapping; extend the existing mappers instead of adding parallel manual mapping.

## Project structure

- Production sources live in `src/main/java` and tests in `src/test/java`.
- Code is organized by technical layer (`domain`, `repository`, `service`, `web`).
- Server-rendered templates live below `src/main/resources/templates`.
- Application configuration lives in `src/main/resources/application.yml`.

## Development commands

Run from the repository root:

```shell
./mvnw spring-boot:run
./mvnw test
./mvnw package
```

- Browser integration tests use Playwright and require its configured browser and system dependencies.

## Working conventions

- Use constructor injection and follow adjacent Java code and Lombok patterns.
- Prefer integration tests; use unit tests for focused coverage.
- Integration tests extend `BaseIT` and reuse its Spring context, `it` profile and test-data lifecycle.
- Name tests `*Test` so Gradle and Maven discover them.
- Use RestAssured for HTTP tests and reuse the existing port, authentication helpers and fixtures.
- Use the fresh Playwright `page` from `BaseIT`; do not create a browser or context per test.
- Run the narrowest meaningful verification while iterating.
- Keep changes task-scoped and preserve unrelated existing changes. When a required file is already modified, integrate with those changes.
- Never commit production credentials. Keep secrets in environment variables or local, unversioned overrides.
- Add concise, verifiable repository-wide guidance discovered during a task to `AGENTS.md`; omit task details and duplicate documentation.
