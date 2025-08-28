# AI Agent Guide

This file provides working notes and conventions for AI agents contributing to this repository.

## Project
- Kotlin library providing a common `LlmClient` API for multiple providers (OpenAI, Gemini).
- Modules:
  - `core-api` – shared interfaces, request/response models, and error handling
  - `providers/openai`
  - `providers/gemini`
  - `examples` – non-published demo module

## Development workflow
- Use Kotlin official code style; run `./gradlew test` before every commit.
- Keep commits small and descriptive; avoid creating new branches.
- This file is the single source of truth for the release checklist and tasks.

## Useful commands
- `./gradlew test` – run unit tests for all modules
- `./gradlew build` – build all modules
- `./gradlew ktlintCheck` – run Ktlint checks across all modules
- `./gradlew ktlintFormat` – auto-format Kotlin code across all modules

---

# Consolidated Task List (Single Source of Truth)

Ordering rules:
1) Completed tasks come first.
2) Items that were previously marked uncompleted but are actually implemented appear next with strikethrough.
3) Remaining pending tasks follow in priority order, each with an explanatory note.

## Completed
- [x] Split repository into publishable modules
- [x] Provide unified LlmClient API with `generate()`
- [x] Provide text convenience API
- [x] Provide GenerationRequest builder and `of()` shortcuts
- [x] Add `idempotencyKey` and `tags` to GenerationRequest
- [x] Keep provider clients extending LlmClient with builder pattern
- [x] Keep provider configs as single source of behavior
- [x] Validate configs on construction
- [x] Enforce explicit HttpClient from consumers
- [x] Support OpenAI idempotency header
- [x] Provide Gemini content cleanup
- [x] Map exceptions to a stable error model (`LlmError`)
- [x] Use SLF4J logging in providers
- [x] Expose usage metrics hook for OpenAI
- [x] Remove library reliance on keys.properties
- [x] Document environment variable usage for keys
- [x] Keep examples as a non-published module
- [x] Configure publishing to GitHub Packages
- [x] Replace global mutable secrets in factory with DI-friendly alternatives
- [x] Introduce DI-friendly `LlmClientFactory` capturing API keys and HttpClient
- [x] Add configurable retry policy with backoff and jitter
- [x] Surface 429 as `RateLimitError` with Retry-After
- [x] Add streaming API design notes and TODOs
- [x] Implement streaming API
- [x] Generalize usage sink to `LlmUsageSink` in api
- [x] Decide & document `JsonResponseProcessor` visibility — Internal (not public API)
- [x] Unit tests for request builders and extractors
- [x] Unit tests for error mapping
- [x] Performance smoke tests
- [x] Add streaming example (post-implementation)
- [x] Adopt SemVer and add CHANGELOG.md
- [x] Document migration away from keys.properties
- [x] README provider matrix and config tables
- [x] Contract tests per provider against LlmClient guarantees
- [x] Add repository governance files
- [x] Make Gemini cleanup configurable
- [x] Apply Ktlint formatting rules for whole project
- [x] Dokka site and publishing instructions

## Pending (prioritized)
- [ ] Integration test plan with real keys (CI-gated)
  - Why: Validates end-to-end behavior against real providers in a secrets-gated nightly/release-tag job.
