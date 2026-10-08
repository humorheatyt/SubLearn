# Agent entry point

This file is the single entry point for future agents. Before changing code, read `docs/PROJECT_BRIEF.md`, `docs/PRODUCT_SPEC.md`, `docs/PHASES.md`, `docs/DECISIONS.md`, `docs/CHECKLIST.md`, `docs/AGENT_REQUESTS.md`, and `docs/KNOWN_ISSUES.md`.

## Non-negotiable constraints

- Work only on Arena's session branch `arena/4386d633-sublearn`. Do not switch branches. Do not commit/push any other branch.
- Inspect before coding. Preserve the project structure. Use phase labels from `docs/PHASES.md`; they are work labels, not permission to create another Git branch.
- For each task, write a 3–6 line plan, implement, run `./gradlew assembleDebug testDebugUnitTest lint`, fix observed failures, verify against `docs/CHECKLIST.md`, then update the relevant docs and `docs/PROGRESS.md`.
- Do not ask the user questions. Make a reasonable assumption and record it in `docs/DECISIONS.md` with the explicit marker `ASSUMPTION`.
- Do not fake success or leave silent TODO/stub paths. LATER features need a domain interface, explicit `NotImplemented` implementation, false feature flag, disabled Coming Soon UI when surfaced, and `docs/EXTENSION_POINTS.md` instructions.
- Keep the NOW/LATER boundary. The current build/tests/device state is **unverified**; do not claim Definition of Done until CI is green and all NOW checklist items have real verification.

## Licensing and privacy

- Never copy GPL code or assets. DualSub Replay may be reused only with MIT attribution, but this repository currently reuses no upstream code. GPL references are ideas only.
- Never add `fastdic_plain.sqlite`, any derived dictionary content, `.bipe` file, or extracted translation model. The future dictionary must be a user-supplied import with an independent, documented schema. Official ML Kit downloads/manages its own model.
- Audit all resolved dependencies/assets/fonts/icons/word lists. Update `THIRD_PARTY_NOTICES.md`. Google ML Kit's non-OSS Terms of Service is open as `REQ-003`; dictionary provenance is `REQ-001`.
- API keys belong only in the Keystore-backed secret store; never log, export, back up or commit them. No ads/monetization.
- Do not commit build outputs, `local.properties`, secrets, signing keys, or large generated data.

## Validation and blocked work

- Run the exact required Gradle task even if an earlier task fails, and record the command/output truthfully.
- If blocked, try at least three distinct recovery approaches. Add/update `docs/AGENT_REQUESTS.md` with ID, phase/task, need, attempts/results, suspected cause, next step, severity; continue with independent work rather than silently stopping.
- Local blocker `REQ-002`: official Gradle 8.9 wrapper restored; local JDK was obtained only under `/tmp`, but distribution download is blocked by sandbox host policy. Static syntax/XML checks are not a substitute for compiler/tests. Use GitHub Actions after pushing to this fixed branch.
- Before release: inspect CI and artifact, verify API 31 smoke, test on a Poco X3 Pro or equivalent, resolve open license review, ensure checklist/docs are complete, then and only then tag `v0.1.0`.

## GitHub workflow

Use `git` for status/diff/commit/push and `gh` for PR/issues/checks/releases. Push only with `git push origin arena/4386d633-sublearn`. If authentication fails, ask the user to reconnect GitHub in Arena; never request credentials in chat. See developer/session instructions for fixed branch constraints.

## Documentation map

- Product and phases: `docs/PRODUCT_SPEC.md`, `docs/PHASES.md`, `docs/DECISIONS.md`.
- Implementation: `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`, `docs/EXTENSION_POINTS.md`.
- Verification/continuation: `docs/CHECKLIST.md`, `docs/KNOWN_ISSUES.md`, `docs/AGENT_REQUESTS.md`, `docs/PROGRESS.md`.
- License: `LICENSE`, `THIRD_PARTY_NOTICES.md`, `docs/REFERENCES.md`.

## Working procedure

1. Inspect the current branch/status and the relevant module/tests.
2. Write a short plan in the response or task note; do not begin by replacing files blindly.
3. Implement in the narrowest appropriate module and keep strings in `values`/`values-fa`.
4. Run focused tests, then the full Gradle command; if blocked, document it and use CI.
5. Update checklist, notices/decisions/issues/progress as appropriate.
6. Review `git diff`, check for secrets/assets/build outputs, push only the fixed branch, and report actual checks and next steps.
