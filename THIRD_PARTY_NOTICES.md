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

## Remaining dependency audit

AndroidX/Compose, Kotlin, coroutines, test libraries, build tools and their
transitive dependencies retain their upstream licences. A resolved-dependency
licence inventory has not been produced because dependency resolution could
not run here. Complete that inventory and include required notices before
shipping an APK.

Termux and packages installed through its package manager are separate upstream
projects. Their licences are not replaced by DAN Terminal's MIT licence.
