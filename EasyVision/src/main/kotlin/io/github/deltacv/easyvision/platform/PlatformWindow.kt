package io.github.deltacv.easyvision.platform

import imgui.ImVec2

interface PlatformWindow {

    var title: String
    var icon: String

    val size: ImVec2

}