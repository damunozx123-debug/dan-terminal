# Security tool catalog integration (22 Nov 2026)

Branch: `feature/tooling-bridge-20261122`. This source change adds a **Security tools (Termux + Debian)** entry on the DAN Terminal home screen. It uses the existing `TermuxBridge.runCommand` plus `AccessCapability.TERMUX_APP` and `TERMUX_RUN_COMMAND_PERMISSION` checks to launch **only fixed help/version commands**, never scans or arbitrary target input. A tap submits the command; it does not confirm process completion. Android builds and hardware acceptance are pending.

Verified operator inventory (command presence, not execution capability): native Termux: nmap, adb, fastboot, python, git, ssh, ollama client; Debian proot: nmap, sqlmap, gobuster, ffuf, hydra, dig, nc, python3, git, ssh. Nikto was **not** verified installed. Remote BlackArch tools and Ollama via SSH were verified outside this Android app but are **not** direct app execution features here.

Before release, run Android compile/unit/lint and physical-device acceptance: permission denial, no Termux installed, Debian profile missing, help button submits command, command failure surfaced in Termux, no unexpected scans, correct back navigation, and no credentials in logs. Termux's `allow-external-apps` configuration must be enabled by the device owner. This does not grant root.
