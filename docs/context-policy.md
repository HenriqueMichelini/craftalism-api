# Context Policy

## Purpose

This repository uses minimum sufficient context.

Agents must avoid both:

- context starvation: acting without enough constraints
- context flooding: reading broad unrelated material

## Core Principle

Use the smallest context set that contains enough concrete constraints to complete the task safely.

## Context Layers

| Layer | File or Directory | Purpose |
|---|---|---|
| 0 | `AGENTS.md` | Agent behavior contract |
| 1 | `docs/index.md` | Documentation router |
| 2 | `docs/workflows/` | Task execution process |
| 3 | repo-local docs | Stable contracts and design docs |
| 4 | `docs/features/<feature>/contract.md` | Feature-specific rules |
| 5 | `docs/features/<feature>/cards/` | Task-specific execution units |
| 6 | Source and tests | Concrete implementation evidence |

## Selection Rule

For every task, select context that answers:

1. What is the objective?
2. What domain or feature rules apply?
3. What project-wide constraints apply?
4. What files are likely affected?
5. What behavior must be preserved?
6. How will the result be validated?

## Escalation Rule

Read more context only when:

- the current context is insufficient
- the selected card references another document
- the feature contract does not answer a required question
- source code contradicts documentation
- validation fails
- the change affects architecture, persistence, security, public APIs, or cross-feature behavior

## De-escalation Rule

Do not continue reading once the current context is sufficient to act safely.

Avoid reading:

- unrelated feature folders
- entire documentation directories
- entire source trees
- historical notes unless explicitly routed

## Conflict Rule

If two sources conflict, use this priority:

1. Current source code and tests
2. Selected implementation card
3. Feature contract
4. Design or contract documents routed by the card
5. Repository conventions and backlog docs

If the conflict changes expected behavior, report it before modifying files.

## Hard Stop Conditions

Agents must stop before implementation if:

- no selected card exists for implementation
- the card has no objective
- the card has no acceptance criteria
- expected files or source areas are unknown
- validation commands are missing
- validation commands cannot run and no fallback is defined
- source code or tests contradict the selected card or feature contract
- the task affects public APIs, schemas, persistence, permissions, security, or external behavior without explicit scope
- the implementation requires touching another feature not listed in the card
- the selected card path does not exist

If the selected card path does not exist, agents may report nearby candidate cards, but must not select, re-route, implement, or reverify any candidate automatically.

## Documentation Drift Rule

One fact must have one authoritative home.

- Routing rules belong in `docs/index.md`.
- Context selection rules belong in `docs/context-policy.md`.
- Feature behavior belongs in `docs/features/<feature>/contract.md`.
- Task-specific execution belongs in the selected card.
- Existing repo-local design and contract docs remain authoritative where routed.
- Historical or exploratory notes are optional and non-authoritative unless routed by a contract or card.
