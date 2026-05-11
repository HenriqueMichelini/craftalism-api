# Feature Index

## Purpose

This directory contains feature-level documentation.

Each feature should have:

- `contract.md`
- `cards/`

Feature `index.md`, `notes.md`, card indexes, wiki pages, ADRs, and architecture overviews are optional expansion points for larger projects only.

## Features

| Feature | Status | Purpose |
|---|---|---|
| `balance-integrity` | planned | Preserve authoritative non-negative balance invariants across service, API, and persistence boundaries. |
| `market-pressure-ladder` | planned | Replace legacy segment stock behavior with authoritative pressure-ladder market pricing and state. |
| `market-trade-history` | planned | Persist and expose committed market executions for dashboard and ops trade-history reads. |

## Feature Documentation Rules

Feature documentation must separate stable rules from execution tasks.

Use:

- `contract.md` for stable feature rules
- `cards/` for implementation tasks
- optional expansion documents only when routed by `docs/index.md`, a feature contract, or a selected card

## Agent Rule

Do not read every feature folder.

Select the relevant feature from this index, then read only its routed documents.
