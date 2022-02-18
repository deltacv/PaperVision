package io.github.deltacv.easyvision.util.eocvsim

enum class EOCVSimPrevizState(val running: Boolean = false) {
    NOT_RUNNING,
    NOT_CONNECTED,
    RUNNING(true),
    RUNNING_BUT_NOT_CONNECTED(true)
}