#!/data/data/com.termux/files/usr/bin/bash
# DAN Terminal M1 idempotent bootstrap.
# Safe to run repeatedly. Does not assume root.

set -euo pipefail

DAN_HOME="$HOME/.dan-terminal"
mkdir -p "$DAN_HOME"/{bin,etc,var/log,workspaces}

echo "[DAN] Updating package lists..."
pkg update -y || true

echo "[DAN] Installing core toolchain..."
pkg install -y \
  git openssh python nodejs-lts clang make cmake \
  ripgrep fd fzf jq curl wget tar unzip \
  nano vim tmux htop procps 2>/dev/null || true

echo "[DAN] Installing Python tooling..."
pip install --upgrade pip setuptools wheel 2>/dev/null || true

echo "[DAN] Creating DAN workspace marker..."
cat > "$DAN_HOME/etc/bootstrap.env" <<EOF
DAN_BOOTSTRAP_VERSION=1
DAN_BOOTSTRAP_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

echo "[DAN] Bootstrap complete."
echo "[DAN] Runtime root: $DAN_HOME"
