package io.github.deltacv.papervision.platform

import imgui.ImVec2

interface PlatformWindow {

    var title: String
    var icon: String

    val size: ImVec2

    var visible: Boolean

    var maximized: Boolean

}