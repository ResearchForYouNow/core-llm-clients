# Contributing to core-llm-clients

Thank you for your interest in contributing! This repository provides a Kotlin multi-module library offering a common `LlmClient` API for multiple providers (e.g., OpenAI, Gemini).

## Ways to contribute
- Report bugs and propose enhancements via GitHub Issues
- Improve documentation and examples
- Add tests to improve coverage and guard against regressions
- Implement features aligned with the roadmap
- Implement llm-clients or add more capabilities for consumers

## Code style and principles
- Use the official Kotlin style guidelines.
- Keep changes small and focused. Prefer incremental, well-described commits.
- Add or update unit tests for any behavior change.
- Avoid introducing new runtime dependencies unless necessary; discuss in an issue first.
- Ensure public API changes are intentional and documented (README and CHANGELOG).

## Running tests
- Unit tests can be run across all modules with `./gradlew test`.
- Provider contract and error mapping tests live under `providers/<provider>/src/test`.
- Add tests close to the code you modify.

## Commit messages and PRs
- Use descriptive commit messages (what and why).
- In Pull Requests, include:
  - Summary of the change and motivation
  - Links to related issues or design notes
  - Notes on testing and potential impacts
- Ensure CI (tests/build) passes.

## Versioning and releases
- This project follows Semantic Versioning (SemVer).
- Maintainers own the release process and CHANGELOG updates.

## Security
Please do not file public issues for sensitive security reports. See SECURITY.md for coordinated disclosure instructions.

## License
By contributing, you agree that your contributions will be licensed under the Apache License, Version 2.0. See LICENSE.
