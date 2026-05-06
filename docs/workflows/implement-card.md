# Workflow: Implement Card

## Purpose

Use this workflow to implement one selected card.

## Required Steps

1. Read `AGENTS.md`.
2. Read `docs/index.md`.
3. Read `docs/context-policy.md`.
4. Read this workflow.
5. Read the selected card.
6. Read the selected feature contract.
7. Read additional documents only if routed by the card or required by context policy.
8. Inspect directly affected source and test files.
9. Declare scope before implementation.
10. Implement the smallest complete change.
11. Run validation commands.
12. Update completion notes only if implementation succeeds.

## Card Readiness Check

Before implementation, verify that the selected card has:

- objective
- expected behavior
- acceptance criteria
- expected files or source areas
- validation commands
- out-of-scope items

If any required section is empty, stop before implementation.

If validation commands cannot run, report why and use only the nearest safe fallback if one is defined.

If the selected card path does not exist, stop before implementation and follow `docs/context-policy.md`.

## Scope Declaration Format

Before implementation, state:

- selected card
- expected behavior
- selected context files
- files likely to change
- validation commands
- out-of-scope items

## Implementation Rules

- Keep the change focused on the card.
- Do not perform unrelated refactors.
- Do not change public contracts unless the card requires it.
- Do not update unrelated documentation.
- Preserve existing behavior unless acceptance criteria require a change.
- If implementation reveals additional work, report a follow-up card instead of expanding scope.
- If another feature must be changed, stop unless the selected card explicitly allows it.
- If public contracts must change, stop unless the selected card explicitly allows it.

## Completion Report Format

After implementation, state:

- files changed
- behavior changed
- tests or checks run
- validation result
- unresolved risks
- follow-up cards needed, if any
