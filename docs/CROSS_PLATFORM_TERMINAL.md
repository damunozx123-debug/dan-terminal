# DAN Terminal — cross-platform product contract

DAN Terminal is a standalone premium terminal workspace, not an Android-only companion. Android was the initial target because Kitty was unavailable there. Linux desktop is the next platform; macOS and Windows follow after architecture and compatibility validation. Phone continuity is optional, not a dependency for desktop use.

## One session, multiple clients

The authoritative shell/PTY and persistent tmux workspace run on the selected Linux host. The desktop launcher starts or attaches to the same named session. Android attaches through authenticated SSH over the existing private network. A disconnected phone does not terminate the server-side shell. No claim of live migration of a phone-local PTY, pixel-identical rendering, or cross-device private-key copying.

The initial workspace name is `dan-phone` to align with open Android PRs #22 and #23. Multiple workspaces are supported by the desktop bootstrap; configurable native Android profiles remain pending. Shared concurrent clients can influence terminal size; test sequential handoff first, then simultaneous attachments. Use app-owned SSH credentials, verified server keys, and existing access controls. No new SSH listener, firewall rules, agent forwarding, or public ports are configured.

## Linux bootstrap (not a native desktop emulator)

```bash
bash scripts/desktop-terminal.sh --check
bash scripts/desktop-terminal.sh --terminal auto
```

Use `--terminal kitty|wezterm|ghostty|current` to select an installed frontend. The launcher reuses tmux and does not install dependencies. It is a bridge while native DAN Terminal desktop is built, not feature parity. A user may use ordinary terminal apps without enabling phone handoff.

On the phone, the existing draft PR #22 implements Termux handoff and #23 proposes an embedded native SSH canvas. Use the same authorized host and `dan-phone` session. On an already configured trusted SSH host, the equivalent manual attachment is `ssh -t HOST_ALIAS tmux new-session -A -s dan-phone`. Do not bypass host-key verification.

## Feature and acceptance matrix

| Feature | Reference | Required acceptance |
|---|---|---|
| Native PTY, VT/ANSI, Unicode, IME, keyboard | Existing M3 Android work | Full device suite, wide/combining characters, resize, scrollback |
| Graphics and keyboard protocols | Kitty, Sixel | Protocol fixtures, images, keyboard negotiation and fallback on both clients |
| GPU accelerated rendering | Kitty, Ghostty, WezTerm | Frame time, latency, memory, power, font shaping measured on real hardware |
| Tabs, splits, layouts, persistent workspaces | tmux, Zellij, WezTerm | Detach/attach, restart, layout restore, per-session isolation |
| Remote transport and file transfer | OpenSSH, Mosh, SFTP | Verified host key, app-owned keys, reconnection, network change, transfer integrity |
| Shell integration | Fish, Bash, Zsh | Command boundaries, exit codes, history/search, opt-in secret redaction |
| Extensibility | Kitty kittens, Lua, Zellij plugins | Versioned permissioned extension API, no arbitrary remote execution |
| AI workspace | DAN-OS | Explicit provider routing, budget, local-first, no automatic execution of model text |
| Mobile UI | DAN Terminal | Touch selection, extra keys, physical keyboard, rotation, background lifecycle |
| DAN-OS integration | Operations console | Launch/attach, capability status, explicit local user action, no unauthenticated shell endpoint |
| Cross-device continuity | SSH + persistent host workspace | Same workspace and running foreground process after desktop/phone detach, reconnect, network change |

Do not promise all competing features or relative performance until measured. Use reusable open-source components where compatible with licensing; audit licenses before distribution. A native shared terminal core/renderer is a design choice requiring a spike (e.g. Rust core with platform-specific UI or Kotlin Multiplatform with native renderer). Do not lock in a framework before Android and desktop graphics/PTY tests.

## Release order

1. Consolidate existing stacked Android M3 PRs; resolve the full UI gate and font/input failure. PR #13 records 3/3 selected physical tests, not a full-suite pass.
2. Verify Linux bootstrap on actual CachyOS/BlackArch, then same-session desktop-to-phone handoff using the existing SSH setup.
3. Review PRs #22 and #23 in dependency order. Native SSH is experimental and not full-color/Sixel parity; verify positive and negative host-key cases and reconnect.
4. Prototype native desktop renderer and compare actual protocol/performance tests against installed reference terminals.
5. Add secure profiles, advanced graphics, extension API, AI workspace and DAN-OS integration incrementally, with platform-specific acceptance.
6. Expand macOS/Windows only after choosing and validating the shared core.

No native desktop binary, Android build, live SSH connection, phone runtime test, or benchmark is claimed by this branch.
