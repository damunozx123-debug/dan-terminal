package com.dan.terminal.feature.ssh;

import com.termux.terminal.TerminalOutput;

/**
 * Adapter across supported Termux terminal-emulator artifact revisions.
 * The clipboard callback was renamed in newer upstream revisions, so expose
 * both spellings without overriding a method absent in an older artifact.
 *
 * Remote OSC clipboard commands are intentionally ignored.
 */
public final class NativeSshTerminalOutput extends TerminalOutput {
    public interface Callback {
        void onWrite(byte[] bytes, int offset, int count);
        void onRedraw();
    }

    private final Callback callback;

    public NativeSshTerminalOutput(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void write(byte[] bytes, int offset, int count) {
        callback.onWrite(bytes, offset, count);
    }

    @Override
    public void titleChanged(String oldTitle, String newTitle) {
        // The initial SSH screen displays a fixed, locally chosen title.
    }

    // Older terminal-emulator versions use this signature.
    public void clipboardText(String text) {
        // Do not let remote output silently read or write device clipboard.
    }

    // Newer terminal-emulator versions use these signatures.
    public void onCopyTextToClipboard(String text) {
        // Do not let remote output silently update Android clipboard.
    }

    public void onPasteTextFromClipboard() {
        // Do not allow remote-initiated clipboard paste.
    }

    @Override
    public void onBell() {
        // No background alerts for remote BEL escape sequences.
    }

    @Override
    public void onColorsChanged() {
        callback.onRedraw();
    }
}
