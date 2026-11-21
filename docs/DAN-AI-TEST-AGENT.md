# DAN AI Test Agent (local Ollama + GitHub handoff)

Runs Gradle tests on BlackArch, analyzes a bounded log excerpt through local Ollama, and can post a short test report to GitHub PR #20. That PR acts as a shared inbox for the connected GitHub app in ChatGPT. It cannot inject a message directly into this chat.

## Setup

    sudo pacman -S --needed python ollama github-cli android-tools
    sudo systemctl enable --now ollama
    ollama pull qwen2.5-coder:1.5b
    gh auth status

Keep Ollama bound to localhost and authenticate GitHub CLI under your own account. The agent needs no cloud AI keys.

## Commands

    cd /home/dan/dan-terminal-m3-verify
    python tools/dan_ai_test_agent.py --mode quick
    python tools/dan_ai_test_agent.py --mode ui
    python tools/dan_ai_test_agent.py --mode full

Quick builds and runs app JVM unit tests. UI runs the six graphical terminal cases. Full runs all connected Android instrumentation tests. UI/full require exactly one online ADB device; connected tests can install or update the Dev test package and interrupt its session.

Send a summarized report to the existing PR:

    python tools/dan_ai_test_agent.py --mode quick --publish --pr 20

Only the bounded Markdown summary is posted: branch/commit, Gradle exit, test counts, failed case names/types, and short redacted local-AI triage. No raw Gradle logs are uploaded. Full logs and JSON are saved in ~/.local/share/dan-os/test-reports/dan-terminal/<UTC-RUN-ID>/.

Before automated publication to a potentially public PR, inspect a sample chatgpt-handoff.md file. Redaction is best effort and AI-generated summaries may still contain private details.

In ChatGPT, send a message such as:

    Review the latest DAN-AI-TEST-REPORT on damunozx123-debug/dan-terminal PR #20.
    Diagnose failures and propose a minimal fix as a draft PR.
    Do not merge, release or remotely modify my devices.

ChatGPT can retrieve PR comments through the connected GitHub app after a conversation turn. The script does not push messages into the current chat, nor can the local AI apply code changes or merges.

## Fast unattended tests: run after source edits (recommended)

The default 20:00 daily timer is too slow for active development. The optional
watcher uses inotify (not polling) to detect Android source or Gradle config edits,
waits 10 seconds without another change, and runs a local quick build + unit gate.
It does not touch the phone; run UI/full instrumented tests explicitly.

    sudo pacman -S --needed inotify-tools
    mkdir -p ~/.config/systemd/user
    cp tools/systemd/dan-ai-test-watch.service ~/.config/systemd/user/
    systemctl --user daemon-reload
    systemctl --user enable --now dan-ai-test-watch.service
    journalctl --user -u dan-ai-test-watch -f

When the watcher is opt-in enabled, the service uses --publish-failures,
publishing ONLY failed/incomplete summary reports to GitHub PR #20.
A passing run is saved locally and not published, avoiding comment spam.
Review the published report format before enabling this service, especially
when PR #20 is public: redaction is best effort.

For initial local-only observation without posting to GitHub:

    python tools/dan_ai_test_watch.py --debounce 10

Stop/disable unattended testing:

    systemctl --user disable --now dan-ai-test-watch.service

This watcher responds to edits under app/src, core/*/src, feature/*/src,
gradle/ and repository-root build configuration. It doesn't react to ordinary
Gradle build output. It may queue another quick test if files change while the
current test run is executing; this avoids silently missing edits.

## Optional unattended quick tests (opt in)

The included user timer runs a quick test at 20:00 machine-local time and posts the short report. Nothing in this pull request enables it.

    mkdir -p ~/.config/systemd/user
    cp tools/systemd/dan-ai-test.service tools/systemd/dan-ai-test.timer ~/.config/systemd/user/
    systemctl --user daemon-reload
    systemctl --user enable --now dan-ai-test.timer
    systemctl --user list-timers dan-ai-test.timer

To stop:

    systemctl --user disable --now dan-ai-test.timer

The timer never runs ADB-connected tests. If the computer is offline or suspended, the next systemd user session may catch up once. Adjust the OnCalendar time before enabling, and review a published report first.
