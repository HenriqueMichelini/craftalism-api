# Workflow: Reverify Card

## Purpose

Use this workflow to verify whether a completed implementation satisfies its card, contract, and validation criteria.

## Required Steps

1. Read `AGENTS.md`.
2. Read `docs/index.md`.
3. Read `docs/context-policy.md`.
4. Read this workflow.
5. Read the selected card.
6. Read the selected feature contract.
7. Inspect changed files and related tests.
8. Run or review validation commands.
9. Report whether the implementation passes, fails, or is partially verified.

## Verification Scope

Check:

- acceptance criteria
- feature contract compliance
- test coverage
- validation output
- unintended unrelated changes

## Report Format

State:

- selected card
- expected behavior
- evidence reviewed
- validation commands/results
- pass/fail status
- issues found
- required fixes, if any

## Rule

Do not modify files during reverification unless explicitly asked.

If the selected card path does not exist, stop before reverification and follow `docs/context-policy.md`.
