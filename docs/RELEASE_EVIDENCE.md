# M6 release evidence

Status: **not release-accepted**. No signed candidate has been produced in this
work session.

The local `scripts/verify_release_candidate.sh` gate checks the APK package,
version name and code, requires a versionCode greater than the supplied prior
version, verifies the APK signature against a supplied expected signer
fingerprint, and prints/checks the APK SHA-256. Its mock-tool tests cover the
success path, missing APK, wrong package/name/code, non-monotonic version,
signature failure, certificate mismatch and checksum mismatch.

Before a prerelease can pass M6, record:

- Signed APK file and SHA-256.
- Package name, version name and monotonically increasing versionCode.
- Expected and observed signer certificate SHA-256 fingerprints.
- Resolved dependency licence inventory/SBOM and shipped notices.
- Release notes and artifact checksum attached to the manually gated draft.
- Physical Redmi/HyperOS acceptance recorded by Dan.

The manual workflow now accepts the trusted fingerprint and previous versionCode
as explicit inputs, requires recorded physical acceptance, runs the verifier,
and creates a draft prerelease. It has not been dispatched. The resolved
runtime dependency tree is retained as evidence; it is not an SBOM or a
completed licence audit. Public release remains gated on the items above.
