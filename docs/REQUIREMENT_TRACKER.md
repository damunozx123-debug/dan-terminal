# DAN Terminal — Requirement Tracker

Authoritative register: DAN_Terminal_Master_Features_v1_1.xlsx (95 requirements).
Status rule: DONE requires the register acceptance test to pass. No acceptance tests have run in this environment.

| ID | Requirement | Priority | Milestone | Status | Evidence / gap |
|---|---|---:|---|---|---|
| T01 | Kitty graphics and keyboard protocols | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T02 | Sixel and iTerm2 image support | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T03 | Tabs, split panes and floating terminal | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T04 | GPU-accelerated rendering where supported | P3 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T05 | Searchable session history | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T06 | Shell integration and command navigation | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T07 | Custom keyboard, mouse, themes and fonts | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| T08 | tmux/Zellij session support | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L01 | Native Termux shell and package environment | P0 | M1 + M7 | PARTIAL | Source now supplies the Termux Bash executable through RUN_COMMAND_PATH and command arguments separately; RUN_COMMAND delivery and HyperOS behavior remain device-unverified. |
| L02 | Debian, Alpine, Ubuntu and Kali profiles | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L03 | PRoot and OCI-image workflow management | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L04 | Git GUI, diffs and repository management | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L05 | Code editor with LSP integration | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L06 | Integrated local web-server preview | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L07 | Build and test task runner | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| L08 | Optional X11 Linux desktops | P2 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R01 | SSH, Mosh and saved hosts | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R02 | SFTP, SCP and rsync transfers | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R03 | SSH jump hosts and agent forwarding | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R04 | Local, remote and SOCKS tunnels | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R05 | Remote Docker and Compose dashboard | P2 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R06 | Remote database explorer | P2 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R07 | Service, log and resource monitoring | P2 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| R08 | Hardware-backed credential management | P0 | M0 + M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| H01 | Bluetooth Classic and BLE scanner | P0 | M1 | PARTIAL | BLE scan source exists; Bluetooth Classic/bonded-device handling and target-device acceptance are missing. |
| H02 | GATT discovery and permitted device control | P0 | M1 | PARTIAL | Connect, service discovery and read callback exist; read/write/notify UI and per-write approval are missing. |
| H03 | Wi-Fi discovery and diagnostics | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| H04 | USB-OTG serial console | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| H05 | NFC reader and tag tools | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| H06 | Sensors, battery and system monitoring | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| H07 | Android API command bridge | P0 | M1 | TODO | No typed Android command broker. |
| H08 | Built-in Shizuku-compatible Android privilege manager with optional ADB/root extension | P0 | M1 + M7 | PARTIAL | Official Shizuku API permission/recovery flow reviewed and patched; see docs/SHIZUKU_REVIEW.md. Embedded service and effective privilege checks remain missing; device acceptance NOT RUN. |
| A01 | AI command explanation and debugging | P2 | M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A02 | Selectable local or remote AI providers | P2 | M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A03 | Workflow builder and reusable tasks | P2 | M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A04 | Scheduled scripts and Android triggers | P2 | M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A05 | Natural-language file categorisation | P2 | M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A06 | Project-aware knowledge retrieval | P2 | M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A07 | Security-scoped AI tool execution | P0-gate | M0 + M6 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| A08 | Automated backup and recovery | P0 | M1 + M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| S01 | Permission-based plugin framework | P0 | M0 | TODO | No plugin permission boundary or sample plugin. |
| S02 | Encrypted secrets and backups | P0 | M0 | PARTIAL | Keystore AES/GCM secret storage exists; authenticated encrypted backups and tamper testing are absent. |
| S03 | Biometric app lock | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| S04 | Command execution approval controls | P0 | M0 | TODO | No structured approval policy or zero-side-effect denial test. |
| S05 | Plugin signature and integrity checks | P1 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| S06 | Audit logs and reproducible exports | P0 | M0 + M1 | TODO | No audit event store or redacted export. |
| S07 | No mandatory cloud account | P0 | M0 | PARTIAL | No account or cloud dependency appears in source; offline acceptance has not run. |
| S08 | Modular feature installation | P1 | M0 + M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| P01 | Context-sensitive Access Required popup and permission routing | P0 | M0 + M1 | PARTIAL | Source routes missing service before Shizuku permission, preserves the pending capability and shows errors. Two additional routing regression tests supplied; NOT RUN. See docs/SHIZUKU_REVIEW.md. |
| P02 | Shizuku API connection and service health/status | P0 | M1 | PARTIAL | Fresh API assessment, pre-v11/denial gates and callback-driven UI added with 12 fake-API tests supplied; NOT RUN. SDK status caching and effective operation/UID checks remain limitations. |
| P03 | Guided Developer options / wireless debugging activation and pairing | P0 | M1 | PARTIAL | Manual Developer options/Wireless debugging guidance exists; pairing and HyperOS navigation are unverified. |
| P04 | Service stop/reboot detection and restart guidance | P0 | M1 | PARTIAL | Binder death clears local grant/pending state and updates recovery. Regression source supplied; NOT RUN. Reboot receiver and HyperOS restart acceptance are still missing. |
| P05 | Privilege capability matrix: normal app, ADB shell, and optional root | P0 | M0 | TODO | No operation-to-capability matrix. |
| P06 | Approved ADB-shell command broker and Android package inspector | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| P07 | ADB/system diagnostics (logcat, dumpsys where permitted) | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| P08 | Shizuku-compatible manager and service integrated into final APK | P1 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| P09 | HyperOS/device-specific settings navigation with generic fallback | P1 | M1 | TODO | No tested HyperOS navigation recipe. |
| P10 | Resume interrupted action only after recheck and renewed write approval | P0 | M1 | TODO | No expiring pending-operation token or renewed write approval. |
| D01 | Spotlight/PowerToys-like universal search and command palette | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D02 | Quick Look-style document, image and structured-output previews | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D03 | Tags, smart folders and non-destructive file organisation | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D04 | Time Machine-style snapshots, version history and restore previews | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D05 | Sysinternals-style process, resource and service inspector | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D06 | Startup task and automation health dashboard | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D07 | Declarative bootstrap, package manifests and environment repair | P0 | M1 + M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D08 | Mixed graphical workspaces: terminal, logs, files, previews, charts | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D09 | Project and device workspace profiles with one-action switching | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D10 | Shell snippets, command autocomplete and cautious paste/broadcast controls | P1 | M2 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| D11 | Clickable command blocks, result cards and rich progress displays | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K01 | Arch Linux ARM/pacman rootless distribution profile | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K02 | BlackArch searchable tool catalogue and optional targeted installs | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K03 | Kali indexed tool catalogue, compatibility tags and lightweight install | P1 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K04 | Isolated authorised security-lab profiles and structured reports | P1 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K05 | Kali NetHunter rootless workflow compatibility and limitations | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K06 | Cross-distribution package search, diagnostics and dependency repair | P1 | M3 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K07 | Optional declarative Nix-on-Droid-inspired environment profiles | P3 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| K08 | Linux graphical sessions and remote desktop protocols (X11/VNC/RDP) | P2 | M5 + M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| Q01 | Connection health, reconnect queue and session recovery | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| Q02 | Host collections, shortcuts and reusable command snippets | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| Q03 | Port browser, tunnel status and remote service dashboards | P1 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| Q04 | Serial-console profiles and scripting for embedded devices | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| Q05 | Optional end-to-end encrypted cross-device settings sync | P3 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| Q06 | Remote Git/editor and database viewer within workspaces | P2 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| X01 | Termux:Boot, Widget and Tasker-compatible actions | P1 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| X02 | Scoped Android clipboard, notifications, camera and location APIs | P1 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| X03 | BLE light identity database, manufacturer/service matching and saved profiles | P0 | M1 | PARTIAL | BLE manufacturer/service metadata is collected; identity database, confidence scoring and saved light profiles are absent. |
| X04 | Android document picker, scoped storage and share-sheet bridge | P0 | M1 | TODO | No SAF picker or scoped document access. |
| X05 | Wi-Fi accessory/USB chipset capability inventory and compatibility guidance | P2 | M5 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| X06 | LAN file exchange and local service preview with explicit binding controls | P2 | M4 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| O01 | One-action setup/rebuild with verified manifests and restore | P0 | M1 + M7 | PARTIAL | Bootstrap script exists; signed/pinned manifests, dry-run, checksum verification, disk estimate and restore are absent. |
| O02 | Signed application/runtime/plugin updates with rollback | P1 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| O03 | Complete installation migration: old Termux to independent DAN runtime | P1 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| O04 | Accessibility, adaptive battery mode and capability-driven UI | P1 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| O05 | App-controlled signed feature/plugin catalogue and SDK | P2 | M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
| O06 | Release pipeline: reproducible APK, SBOM, licenses and security regression tests | P0 | M0 + M7 | TODO | Outside the M0/M1 source baseline; not implemented or acceptance-tested. |
