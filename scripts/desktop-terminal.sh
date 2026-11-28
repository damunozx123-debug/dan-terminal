#!/usr/bin/env bash
# Desktop continuity bootstrap. No install, remote access configuration or AI execution.
set -euo pipefail
session=dan-phone
terminal=auto
mode=launch
while (( $# )); do
  case "$1" in
    --terminal) [[ $# -ge 2 ]] || exit 2; terminal="$2"; shift 2 ;;
    --session) [[ $# -ge 2 ]] || exit 2; session="$2"; shift 2 ;;
    --check) mode=check; shift ;;
    --help) printf '%s\n' 'Usage: bash scripts/desktop-terminal.sh [--terminal auto|kitty|wezterm|ghostty|current] [--session NAME] [--check]'; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$1" >&2; exit 2 ;;
  esac
done
[[ "$session" =~ ^[A-Za-z0-9_-]{1,48}$ ]] || { echo 'Invalid session name' >&2; exit 2; }
case "$terminal" in auto|kitty|wezterm|ghostty|current) ;; *) echo 'Unsupported terminal' >&2; exit 2;; esac
if [[ "$mode" == check ]]; then
  for tool in kitty wezterm ghostty tmux ssh zellij; do
    if command -v "$tool" >/dev/null 2>&1; then printf '%s: installed\n' "$tool"; else printf '%s: missing\n' "$tool"; fi
  done
  exit 0
fi
command -v tmux >/dev/null 2>&1 || { echo 'tmux is required' >&2; exit 1; }
if [[ -n "${TMUX:-}" ]]; then
  tmux new-session -Ad -s "$session"
  exec tmux switch-client -t "=$session"
fi
if [[ "$terminal" == auto ]]; then
  terminal=current
  if [[ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
    for candidate in kitty wezterm ghostty; do
      if command -v "$candidate" >/dev/null 2>&1; then terminal="$candidate"; break; fi
    done
  fi
fi
case "$terminal" in
  current) exec tmux new-session -A -s "$session" ;;
  kitty) exec kitty tmux new-session -A -s "$session" ;;
  wezterm) exec wezterm start -- tmux new-session -A -s "$session" ;;
  ghostty) exec ghostty -e tmux new-session -A -s "$session" ;;
esac
