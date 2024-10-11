package io.github.deltacv.papervision.gui.style

object CurrentStyles {
    lateinit var imnodesStyle: ImNodesStyleTemplate
}

interface Style {
    fun apply()
}