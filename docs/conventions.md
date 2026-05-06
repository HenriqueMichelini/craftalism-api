# Project Conventions

## Purpose

This file defines stable project-wide conventions that support the feature/card workflow.

Keep this document concise. Feature-specific rules belong in feature contracts.

## Documentation Conventions

- Use `docs/index.md` for routing.
- Use `docs/features/<feature>/contract.md` for stable feature rules.
- Use `docs/features/<feature>/cards/` for task-specific implementation work.
- Do not duplicate stable rules across cards.
- Historical or exploratory notes are non-authoritative unless routed by a contract or selected card.

## Change Conventions

- Keep one card implementation focused.
- Avoid mixing feature work, formatting, and unrelated refactoring.
- Do not change public APIs, schemas, persistence, permissions, security, or external behavior unless the selected card explicitly scopes that change.

## Validation Conventions

- Use the validation commands in the selected card.
- Prefer focused tests for narrow changes.
- Use `rtk ./gradlew test` from `java/` for cross-service market behavior changes.
