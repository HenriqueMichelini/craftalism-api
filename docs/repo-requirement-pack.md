# Repo Requirement Pack: craftalism-api

## Repo Role
`craftalism-api` is the authoritative backend source of truth for the economy domain. It must define and enforce canonical server-side behavior for balances, transactions, transfers, API error semantics, idempotency behavior, and incident persistence.

## Owned Contracts
- `transfer-flow`
  - Own and enforce the canonical transfer endpoint and transfer semantics
  - Guarantee transfer atomicity and authoritative transfer behavior
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
  - Validate tokens consistently with the canonical issuer/JWKS contract
- `ci-cd`
  - Meet backend quality-gate standards for PR/push/release workflows
- `testing`
  - Meet test expectations appropriate for a contract-owning backend service
- `documentation`
  - Keep README and endpoint docs aligned with implementation and ecosystem contracts

## Current Priority Areas
- Verify and enforce transfer-flow correctness and canonical endpoint behavior
- Verify and enforce transaction route consistency and compatibility behavior
- Verify and enforce deterministic error semantics for API consumers
- Verify and enforce idempotency behavior for transfer retries
- Verify and enforce incident recording/queryability behavior
- Verify issuer alignment and fail-fast configuration safety
- Improve CI/CD quality gates if missing or weak
- Ensure README/docs reflect actual implementation and current canonical behavior

## Local Requirements
- Keep controller/service/repository boundaries clean
- Keep transactional boundaries explicit and correct
- Maintain persistence correctness and migration safety
- Keep security policy intentional and documented
- Preserve backward compatibility only where explicitly required

## Governance Requirements
- Comply with shared `ci-cd`, `testing`, and `documentation` standards
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
- Are tests strong enough for a contract-owning backend repo?
- Are CI/CD workflows enforcing quality, not just publishing artifacts?

## Success Criteria
- Owned contracts are implemented or explicitly verified as already compliant
- API behavior is authoritative and safe for consumers
- Docs match implementation
- CI/CD includes meaningful quality gates
- Tests provide confidence in critical behaviors
