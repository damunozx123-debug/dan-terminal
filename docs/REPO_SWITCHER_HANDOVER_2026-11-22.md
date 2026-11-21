# DAN-OS multi-repository tmux switcher: handover

Date: 2026-11-22 (NZDT). This is a non-sensitive, versioned handover for future ChatGPT development sessions.

## Source of truth

- Canonical reusable shell implementation: [dan-os/scripts/dan-repo-switch.sh](https://github.com/damunozx123-debug/dan-os/blob/main/scripts/dan-repo-switch.sh).
- The operational BlackArch/Termux connection baseline remains [docs/DAN_OS_CONNECTION_HANDOVER_2026-11-22.md](DAN_OS_CONNECTION_HANDOVER_2026-11-22.md) in each of `dan-os`, `dan-terminal`, and `net-intel-app-v9`.
- GitHub source documentation is not proof that a script is installed or running on the laptop.

## VERIFIED by user in Termux/BlackArch (22 Nov 2026 NZDT)

- A Redmi local-Termux shell connects to BlackArch through Tailscale and SSH using the user-defined `dan` shortcut. The earlier `No route to host` problem was caused by **Tailscale being off** on the phone; the user switched it on and confirmed `dan` worked.
- Persistent tmux sessions existed on BlackArch: `dan-phone`, `dan-terminal`, and `bmw-app`. The tmux session list showed `dan-terminal` attached, and duplicate-session errors only meant the sessions already existed.
- The user confirmed two existing BlackArch Bash functions worked: `bmw` switches to `bmw-app`; `terminal` switches to `dan-terminal`.
- `vehicle-forge-x` is the BMW app checkout; the DAN Terminal checkout is `~/dan-terminal-m3-verify`.
- These are historical, user-reported results, not current connection/uptime evidence.

## IMPLEMENTED IN GITHUB, NOT YET VERIFIED ON BLACKARCH

- `dan-os/scripts/dan-repo-switch.sh` defines `repos` (interactive repo picker) and `repo <name>` (direct project switch).
- It searches local Git checkouts and, if GitHub CLI is installed and authenticated, the user's GitHub repository names. It does **not** clone silently: a missing repository prompts the user before `gh repo clone`.
- It reuses `bmw-app` for `vehicle-forge-x`, `dan-terminal` for `dan-terminal`, and otherwise creates a dedicated `repo-<name>` tmux session, leaving existing sessions/builds intact.
- `repo bmw` and `repo terminal` are accepted aliases. The existing standalone `bmw` and `terminal` functions and the phone-local `dan` shortcut remain untouched.
- A newly created tmux session starts in its repo checkout. **Existing sessions keep their current directory**, so use `cd` once inside an older session if required.
- An all-repo switcher script was previously pasted in chat, but no operator output proving its installation or runtime acceptance has yet been provided. Do not report `repo`/`repos` as verified.

## Safe setup on BlackArch (after user reviews code)

Run in a BlackArch shell, not Termux's local shell:

```bash
mkdir -p "$HOME/.config/dan"
if [[ -f "$HOME/.config/dan/repo-switch.sh" ]]; then
  cp -p "$HOME/.config/dan/repo-switch.sh" "$HOME/.config/dan/repo-switch.sh.previous"
fi

gh api repos/damunozx123-debug/dan-os/contents/scripts/dan-repo-switch.sh   --jq '.content' | base64 -d > "$HOME/.config/dan/repo-switch.sh"

bash -n "$HOME/.config/dan/repo-switch.sh" || return 1
LINE='source "$HOME/.config/dan/repo-switch.sh"'
grep -Fqx "$LINE" "$HOME/.bashrc" || printf '%s\n' "$LINE" >> "$HOME/.bashrc"
source "$HOME/.config/dan/repo-switch.sh"
type repo repos
```

Note: The `return 1` on syntax failure applies when these commands are run
from a sourced Bash setup; use `exit 1` instead in a stand-alone provisioning
script. Do not proceed to sourcing if `bash -n` fails. A future handover
should record actual shell validation and a `repos` interaction separately.

Interactive usage:

```bash
repos
repo dan-os
repo net-intel-app-v9
repo vehicle-forge-x
repo dan-terminal
```

If a project is not cloned, its initial `repo` command may request permission
to clone it. `fzf` is optional; Bash `select` is the fallback. `tmux`,
`git`, and optional authenticated `gh` are the runtime prerequisites.

## Boundaries for future ChatGPT integrations

1. Preserve the current working `dan`, `bmw`, `terminal` functions and existing tmux sessions. Do not kill, rename, or replace them without explicit consent.
2. Verify the active remote checkout, repo branch, `git status -sb`, and test evidence before any code updates. Switching does not make branches current or tests pass.
3. Do not commit SSH keys, GitHub tokens, environment files, private endpoints/IPs, backup archives, runtime status dumps, or local inventories.
4. Do not install packages, clone repos, start network scans, or change Tailscale/SSH/OS configuration as an automatic side effect of documentation.
5. If the repo switcher needs improvement, change the canonical source in `dan-os`, and update these handovers; require BlackArch shell test evidence before upgrading any VERIFIED claim.
