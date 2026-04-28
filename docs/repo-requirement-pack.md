# Repo Requirement Pack: craftalism-api

## Repo Role
`craftalism-api` is the authoritative backend source of truth for the economy domain. It defines and enforces canonical server-side behavior for balances, transactions, transfers, API error semantics, idempotency behavior, and incident persistence.

## Owned Contracts
- `transfer-flow`
  - Own and enforce the canonical transfer endpoint and transfer semantics
  - Guarantee transfer atomicity and authoritative transfer behavior
- `market-contract`
  - Own and enforce canonical market snapshot, quote, execute, and quote lifecycle semantics
  - Own authoritative market rejection codes and response shapes for consumers
- `transaction-routes`
  - Own and enforce canonical transaction route structure and compatibility policy
- `error-semantics`
  - Own authoritative HTTP/domain error behavior for consumers
- `idempotency`
  - Own retry-safety behavior for transfer operations
- `incident-recording`
  - Own structured incident persistence and queryability for critical failures

## Consumed Contracts
- `auth-issuer`
  - Own validation-side enforcement of issuer alignment for protected API access
  - Consume issuance-side truth from `craftalism-authorization-server`
- `ci-cd`
  - Meet backend quality-gate standards for PR/push/release workflows
- `testing`
  - Meet test expectations appropriate for a contract-owning backend service
- `documentation`
  - Keep README and endpoint docs aligned with implementation and ecosystem contracts
- `security-access-control`
  - Keep public/protected API surface policy explicit and aligned with implementation

## Current Phase Objective
This phase is limited to:
- verifying or implementing missing behavior for owned contracts
- aligning validation-side issuer behavior with the shared `auth-issuer` contract
- correcting documentation drift directly related to owned contracts
- correcting CI/CD or test gaps only when they block contract confidence for this repo

This phase is not for broad refactoring or unrelated platform improvements.

## Required This Phase
- Verify each owned contract and classify it as:
  - already compliant
  - partially compliant
  - missing
  - incorrectly implemented
- Implement only owned-contract gaps that are confirmed in this repo
- Verify validation-side issuer enforcement behavior against the shared contract
- Fix documentation only where it directly contradicts implementation or owned contracts
- Fix CI/CD or testing only where:
  - required standards are clearly violated, and
  - the gap materially weakens trust in this repo’s owned contracts

## Not Required This Phase
- Broad architectural rewrites
- Unrelated endpoint redesign
- Plugin/client consumer fixes
- Deployment-wide environment redesign
- Dashboard/auth feature expansion unrelated to API-owned contracts

## Local Requirements
- Keep controller/service/repository boundaries clean
- Keep transactional boundaries explicit and correct
- Maintain persistence correctness and migration safety
- Keep security policy intentional and documented
- Preserve backward compatibility only where explicitly required

## Governance Requirements
- Comply with shared `ci-cd`, `testing`, `documentation`, and `security-access-control` standards
- Do not leave owned contracts partially implemented without explicitly documenting the gap
- Treat this repo as the canonical source of truth for the contracts it owns

## Out of Scope
- Plugin-side fallback behavior implementation
- Minecraft command UX behavior
- Dashboard UI behavior
- Auth-server token issuance internals
- Deployment compose/runtime logic beyond API-consumed expectations

## Audit Questions
- Are all owned contracts fully implemented, authoritative, and documented?
- Are routes, transfer semantics, and error behavior stable and consistent for consumers?
- Is issuer validation aligned with the shared issuer contract?
- Is API access policy aligned with the shared security/access-control standard?
- Are tests and CI/CD sufficient to trust this repo’s owned contracts?

## Success Criteria
- Owned contracts are implemented or explicitly verified as already compliant
- API behavior is authoritative and safe for consumers
- Validation-side issuer behavior is aligned and explicit
- Docs match implementation where contract ownership applies
- CI/CD and tests meet minimum required confidence for this phase
