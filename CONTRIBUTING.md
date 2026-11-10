# Contributing

This is an early M0/M1 implementation. Read `docs/IMPLEMENTATION_STATUS.md`,
`docs/SHIZUKU_REVIEW.md`,
`docs/MILESTONE_REPORT_M0_M1.md` and the requirement tracker before changing it.

Use `feature/*`, `fix/*` or `build/*` branches and open pull requests against
`main` while the project uses a single integration branch. Require at least one human
approval before merge; repository rules must enforce this once published.
Never force-push or rewrite shared history. Do not create a release tag without
maintainer approval.

Use Conventional Commits with a body containing Motivation, Files,
Verification and Rollback. Reference canonical requirement IDs when advancing
behavior. Mark commands EXECUTED or NOT RUN; never invent successful builds,
test counts, APKs, screenshots or device results.

Run the structural checker, debug assembly, unit tests and lint. Run applicable
instrumented tests on a device. Record the real results in the tracker/report;
source implementation alone does not move a requirement to DONE.

Do not commit credentials, signing keys, keystores or local.properties. Review
permission changes and destructive-action approval separately. Submit only
changes you have permission to contribute under the project's MIT licence.
