# Repository Guidelines

## Project Structure & Module Organization
The repository is Gradle-multi-module: `netty-server-core` houses domain entities, ports, and mappers shared by concrete servers; `netty-server` is the unified server module that supports both blocking (for EventLoop blocking reproduction) and non-blocking (async refactoring) modes via configuration. Infrastructure assets live under `docker/` (compose stack plus service configs) and `scripts/` (automation helpers such as `setup-infra.sh` and report generators). Load and regression scenarios are defined in `k6/`, while documentation and monitoring artifacts reside in `docs/` and `reports/`. Keep new components inside the matching module and reuse core adapters before adding duplicates.

## Build, Test, and Development Commands
`./scripts/setup-infra.sh` or `cd docker && docker compose up -d` brings up RabbitMQ, Prometheus, Grafana, and support services. Use `./gradlew clean build` for a full compile plus unit test run, and `./gradlew :netty-server:bootRun` to launch the server locally. The server mode (blocking/non-blocking) is controlled by `application.yml` or environment variable `SERVER_MODE`. `k6 run k6/blocking-reproduction-test.js` executes the EventLoop blocking reproduction test, and `./scripts/run-test-with-report.sh` captures both Gradle results and `k6-summary.json` for sharing.

## Coding Style & Naming Conventions
Target Java 21 and Spring Boot 3.2 as defined in `build.gradle`; use four-space indentation, `UpperCamelCase` for classes, and `lowerCamelCase` for members. Favor constructor injection, immutable DTOs, and Netty-friendly non-blocking flows—any blocking call must leave the event loop. Place adapters in adapter packages (`infra`, `api`) and keep package names aligned with `com.readtimeout.*`. Stick to IDE auto-format with these defaults and keep logging consistent with the existing `log.info(...)` patterns.

## Testing & Load Verification
JUnit 5 is configured via the root Gradle script, so `./gradlew test` must pass before opening a PR. Name unit tests with the `*Test` suffix (e.g., `GatewayMessageHandlerTest`) and annotate important scenarios with `@DisplayName` to describe the ReadTimeout variant being exercised. Extend coverage for domain services and sendMessage ports whenever behavior changes. Load tests belong in `k6-tests/`; update or add scenarios and commit any resulting `k6-summary.json` deltas so reviewers can compare throughput/timeout rates.

## Commit & Pull Request Guidelines
Use concise Conventional Commit style subjects such as `feat(core): add async publisher` or `fix(v1): guard confirm timeout`. Every PR should include: linked issue or context, summary of functional impact, manual verification steps (commands or curl samples), and relevant metrics or screenshots (Grafana, k6, Prometheus). Include notes about Docker or infra changes because reviewers will need to restart services accordingly.

## Security & Operations Tips
Secrets and credentials are stored inside the compose files under `docker/`; do not hard-code them in Java classes. When modifying Docker images or ports, reflect the change in `docs/` and the troubleshooting section of `README.md`. Always stop local stacks with `docker compose down` when finished to avoid orphaned queues or Grafana sessions.
