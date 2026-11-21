package com.dan.terminal.feature.termux

enum class TerminalInputKey(val label: String, val sequence: ByteArray) {
    CTRL_C("Ctrl+C", byteArrayOf(0x03)),
    CTRL_D("Ctrl+D", byteArrayOf(0x04)),
    ESC("Esc", byteArrayOf(0x1b)),
    TAB("Tab", byteArrayOf(0x09)),
    UP("↑", "\u001b[A".toByteArray()),
    DOWN("↓", "\u001b[B".toByteArray()),
    RIGHT("→", "\u001b[C".toByteArray()),
    LEFT("←", "\u001b[D".toByteArray());

    fun bytes(applicationCursorKeys: Boolean): ByteArray =
        if (applicationCursorKeys && this in listOf(UP, DOWN, RIGHT, LEFT)) {
            "\u001bO${sequence.last().toInt().toChar()}".toByteArray()
        } else sequence
}
