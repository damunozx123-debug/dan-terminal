# Third-party notices

The root MIT licence applies to DAN Terminal's original source. It does not
relicense third-party libraries, tools, applications or downloaded packages.

## Shizuku API

The project declares `dev.rikka.shizuku:api:13.1.5` and
`dev.rikka.shizuku:provider:13.1.5`. The upstream Shizuku-API repository uses
MIT, copyright (c) 2021 RikkaW:
https://github.com/RikkaApps/Shizuku-API/blob/master/LICENSE

The full upstream notice is retained at `licenses/Shizuku-API-MIT.txt`.
This source distribution does not bundle the Shizuku manager/service APK.
Any future service embedding requires a separate dependency, licensing and
security review; permission to use the API is not a review of that proposal.

## Termux terminal view and emulator

The app uses `com.termux.termux-app:terminal-view:0.118.0` and its transitive
`terminal-emulator` library for terminal rendering and PTY creation. The upstream
Termux repository identifies the terminal-view and terminal-emulator code as
Apache License 2.0 exceptions; this does not incorporate the GPLv3-only Termux
application or `termux-shared` library. The full Apache License 2.0 text is
retained at `licenses/Apache-2.0.txt`.

Upstream source and library guidance:
https://github.com/termux/termux-app/tree/v0.118.0/terminal-view
https://github.com/termux/termux-app/tree/v0.118.0/terminal-emulator

## Net Intel connectivity model

The connectivity diagnostics module adapts the read-only measurement fields,
failure handling and report concepts from `damunozx123-debug/net-intel-app-v9`
(MIT, copyright (c) 2026 Project contributors). The synchronous Android UI,
SharedPreferences model and exporter implementation were not copied; DAN
Terminal uses an injected probe transport, coroutine IO execution and its own
storage/export APIs. The full MIT notice is retained at
`licenses/Net-Intel-MIT.txt`.

Reviewed upstream snapshot: `b565fb691760beea1f24da097e168b62a2bbafe8`.
Relevant source references: `ProbeEngine.java`, `ScanResult.java`,
`HistoryStore.java` and `ReportExporter.java` under
`android/app/src/main/java/nz/netintel/mobile/`.
Upstream source: https://github.com/damunozx123-debug/net-intel-app-v9

## Remaining dependency audit

AndroidX/Compose, Kotlin, coroutines, test libraries, build tools and their
transitive dependencies retain their upstream licences. A resolved-dependency
licence inventory has not been produced because dependency resolution could
not run here. Complete that inventory and include required notices before
shipping an APK.

Termux and packages installed through its package manager are separate upstream
projects. Their licences are not replaced by DAN Terminal's MIT licence.
