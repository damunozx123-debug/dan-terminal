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

## Focused source research — 15 November 2026

Android's official API-29 behavior documentation prohibits direct execution of
files under an app's writable home directory for apps targeting API 29+.
DAN Terminal targets API 36. Therefore a proposed downloaded `filesDir/proot`
executable is insufficient: the spike must establish a supported executable
packaging/loader path and test it from the actual app UID, not merely ADB shell.

Primary reference:
https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission

A candidate source to evaluate is Termux's maintained Android PRoot fork. Its
package build recipe currently identifies version `5.1.107.102`, a pinned
source archive SHA-256
`30d12a6e2cc4147123756ff58edea262d3083bbc238c12d0ab9b39e3d1c40852`,
GPL-2.0, dependencies `libandroid-shmem` and `libtalloc`, and an unbundled loader
installed into Termux's libexec prefix. This hash is for the source archive,
not an Android ARM64 binary or a Debian image. The recipe is an input to a
compatibility/licensing spike, not approval to copy Termux's binary paths into
DAN Terminal.

Primary references:
https://github.com/termux/termux-packages/blob/master/packages/proot/build.sh
https://github.com/termux/proot/blob/master/COPYING

No PRoot binary, source dependency or Linux image was added to the APK. The
next proof remains an APK-packaged Android ARM64 host executable plus loader
whose Linux guest IO, resizing, signals and lifecycle attach to the current
PTY under Android 16/HyperOS. Licence notices, corresponding source packaging,
dependency compatibility and image integrity remain explicit review items.
