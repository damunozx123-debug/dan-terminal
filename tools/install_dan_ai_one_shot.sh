#!/usr/bin/env bash
# Opt-in one-shot testing after Git commits. Does not start or enable a timer.
set -euo pipefail
root=$(git rev-parse --show-toplevel)
cd "$root"
if [[ -n $(git config --local --get core.hooksPath || true) ]]; then
  echo "A custom core.hooksPath exists; refusing to override it." >&2
  exit 1
fi
hook=$(git rev-parse --git-path hooks/post-commit)
mkdir -p "$(dirname "$hook")"
if [[ -e "$hook" ]] && ! cmp -s tools/git-hooks/post-commit "$hook"; then
  echo "Existing post-commit hook at $hook; refusing to overwrite." >&2
  exit 1
fi
mkdir -p "$HOME/.config/systemd/user"
install -m 644 tools/systemd/dan-ai-test-once.service \
  "$HOME/.config/systemd/user/dan-ai-test-once.service"
install -m 755 tools/git-hooks/post-commit "$hook"
# Previous mechanisms are intentionally disabled to avoid duplicate testing.
systemctl --user disable --now dan-ai-test-watch.service >/dev/null 2>&1 || true
systemctl --user disable --now dan-ai-test.timer >/dev/null 2>&1 || true
systemctl --user daemon-reload
echo "DAN-AI one-shot commit hook installed."
echo "No persistent watcher or recurring timer has been enabled."
echo "On next git commit, the service starts quick tests then exits."
echo "Evidence: ~/.local/share/dan-os/test-reports/dan-terminal/"
echo "Review any failure report before posting sensitive details to public GitHub."
