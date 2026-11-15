# M4 runtime and PTY decision gate

Status: **BLOCKED before production backend selection.** This note records the
compatibility gate; it does not claim that a Linux profile or SSH session runs
inside DAN Terminal.

## Evidence available

- DAN Terminal starts an app-owned `/system/bin/sh` PTY using the Termux
  terminal emulator API.
- Net Intel's Debian scripts call Termux and `proot-distro`; they expect the
  Termux app, its package manager and an already-installed Debian distribution.
  They are useful consent/bootstrap references, not an app-private runtime.
- Termux `RUN_COMMAND` returns a one-shot command result. It does not provide a
  bidirectional process stream that DAN Terminal can attach to its current PTY.
- No sibling repository inspected provides a PRoot runtime, Linux profile
  manager or SSH client suitable for direct reuse.

## Exact architecture gate

A candidate M4 runtime is acceptable only if a prototype on Android can launch
an ARM64 Linux-profile shell as a child process under the DAN Terminal app UID,
connect the child's stdin/stdout/stderr to the existing app-owned PTY, retain
one shell process across commands, report exit and crash states, and shut down
or remove files without touching Termux data. A sequence of RUN_COMMAND calls
does not pass this gate.

## Candidate evaluation still required

| Candidate | Potential benefit | Unresolved gate |
|---|---|---|
| App-private PRoot + Debian rootfs | Self-contained app data and clearer lifecycle ownership | Find maintained Android ARM64 PRoot binary/dependencies; verify licence and hashes; test process launch, ptrace/SELinux behavior, PTY attachment, storage size, upgrade rollback and removal on Redmi/HyperOS. |
| Termux companion runtime | Reuses an established Android package environment and existing user install | Prove a supported bidirectional PTY/session API with host-side process lifetime and safe ownership. RUN_COMMAND is excluded. Review Termux app/API licences and version compatibility. |
| Bundled Linux image/container manager | Potentially richer profile management | No inspected maintained implementation; Android kernel/container constraints and image size are unproven. |

## Decision

Do not select or ship a production M4 backend yet. The missing proof is a
working Android-side runtime process whose file descriptors can be attached to
the existing PTY and whose lifecycle remains under DAN Terminal control. The
Net Intel scripts do not supply that proof. Keep Debian as the proposed first
profile only; it is not yet a supported profile.

## Next focused spike

1. Record exact candidate project, commit/tag, licence, binary provenance and
   SHA-256 for an Android ARM64 PRoot executable and its dependencies.
2. Build a disposable prototype in app-private storage; do not add a permanent
   dependency or modify the release workflow.
3. Demonstrate `cd`, environment persistence, interactive input/output,
   `stty size`, Ctrl+C, process exit, app restart cleanup and uninstall cleanup
   through the current PTY.
4. Run first on Android 16 emulator, then on Dan's Redmi/HyperOS. Emulator
   success permits continued work but cannot establish device acceptance.
5. If any process, PTY, integrity, licence or lifecycle condition fails, reject
   that candidate and evaluate the next one.

Until these checks pass, M4 Linux runtime, profile and SSH/SFTP claims remain
unimplemented. SSH library selection follows runtime selection so credentials
and host verification can be designed against the actual storage/process
boundary.
