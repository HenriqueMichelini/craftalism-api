# Documentation Index

## Purpose

This file routes humans and AI agents to the smallest relevant context.

Do not read the entire documentation tree by default.

## Core Documents

| Need | Read |
|---|---|
| Agent operating rules | `../AGENTS.md` |
| Context selection rules | `context-policy.md` |
| Project-wide conventions | `conventions.md` |
| Feature list | `features/index.md` |
| Market pressure-ladder source of truth | `market-pressure-ladder-sigmoid-pricing.md` |
| Market API contract | `market-contract-mvp.md` |
| Repository contract map | `repo-contract-map.md` |
| Repository requirement pack | `repo-requirement-pack.md` |

## Task Routing

| Task Type | Required Context |
|---|---|
| Implement a feature card | `workflows/implement-card.md` + selected feature `contract.md` + selected card |
| Reverify completed work | `workflows/reverify-card.md` + selected card + changed files/tests |
| Debug a defect | related feature contract + failing test/log/source files |
| Update documentation | affected docs only |
| Refactor code | affected source/test files + relevant conventions |
| Change architecture or public contract | affected feature contracts + explicit card scope |
| Understand project background | `../README.md` first, then routed docs only |

## Ambiguous Cases

| Situation | Required Action |
|---|---|
| No selected card exists | Stop and ask for the selected card |
| Feature cannot be identified | Stop or use defect evidence to identify the smallest likely feature |
| Validation is missing | Stop before implementation |
| Expected files are unknown | Inspect only enough source structure to identify likely files, then declare scope |
| Task touches multiple features | Stop unless the selected card explicitly allows cross-feature work |
| Task changes public APIs, schemas, persistence, permissions, security, or external behavior | Stop unless explicit scope exists in the card |

## Context Escalation

Read additional context only when:

- the selected workflow requires it
- the selected card requires it
- source code contradicts documentation
- tests reveal hidden behavior
- validation fails
- the task affects architecture, persistence, security, public APIs, permissions, or cross-feature behavior

## Stop Rule

Stop reading once you have enough context to:

- describe the intended change
- identify affected files
- preserve relevant constraints
- implement safely
- validate the result
