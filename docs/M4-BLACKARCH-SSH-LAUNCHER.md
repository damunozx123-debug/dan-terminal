# M4: BlackArch SSH launcher (Termux handoff)

Scope: adds a BlackArch button to DAN Terminal Preview. It launches the existing SSH + tmux connection IN TERMUX using its documented RUN_COMMAND intent. It does not embed SSH output into DAN Terminal's native terminal renderer, nor access or copy private keys.

## Setup on Redmi (local Termux, not BlackArch)

Ensure phone Termux has a private SSH key and working SSH host alias. The user's tested setup:

    Host blackarch
        HostName 100.72.159.123
        User dan
        IdentityFile ~/.ssh/id_ed25519_blackarch
        IdentitiesOnly yes
        PasswordAuthentication no
        ServerAliveInterval 30
        ServerAliveCountMax 3

In local Termux, open ~/.termux/termux.properties and ensure exactly:

    allow-external-apps=true

Then run termux-reload-settings, and manually verify:

    ssh -tt blackarch 'tmux new-session -A -s dan-phone'

In Android Settings > Apps > DAN Terminal Preview > Permissions > Additional permissions, allow Run commands in Termux environment if shown. The AndroidManifest already declares com.termux.permission.RUN_COMMAND. The Termux app itself must be installed. Android 16 may require you to open Termux to view the new session.

## Test

1. On the BlackArch review branch: ./gradlew :app:assembleDebug
2. After verifying the package/signature, install the Dev APK if you approve interruption of the existing Dev installation.
3. Open DAN Terminal, tap BlackArch.
4. In Termux, verify you are in dan-phone and run hostname. Expected: blackarch.
5. The local DAN Terminal shell remains separate. Termux retains SSH host-key verification and private key storage.

## Limit

Native in-app SSH is follow-up development. It needs a separate SSH protocol client, persistent host-key checking and byte-stream-to-terminal rendering. Termux RUN_COMMAND cannot provide the live Termux terminal screen inside DAN Terminal's TerminalView.

Official Termux API reference: https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent
