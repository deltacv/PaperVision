package io.github.deltacv.papervision.gui.compose.dsl

import imgui.ImGui
import imgui.ImVec2
import io.github.deltacv.papervision.gui.compose.Compose
import io.github.deltacv.papervision.gui.compose.item.*
import io.github.deltacv.papervision.gui.compose.item.layout.AlignedRowItem
import io.github.deltacv.papervision.gui.util.Font

class ComposeCtx(val composer: Compose) {
    fun pushFont(font: Font) {
        composer.add(PushFontItem(font))
    }

    fun button(text: String, onClick: () -> Unit = {}) {
        composer.add(ButtonItem(text, onClick))
    }

    fun text(text: String) {
        composer.add(TextItem(text))
    }

    fun alignedText(text: String, alignment: Float) {
        alignedRow(alignment) {
            text(text)
        }
    }

    fun newLine() {
        composer.add(NewLineItem())
    }

    fun alignedRow(
        alignment: Float,
        spacing: Float = 5f,
        // Opcional: permitir que la fila use un ancho específico o el total
        width: Float = composer.availableSize.x,
        content: ComposeCtx.() -> Unit
    ) {
        // Creamos un sub-composer para capturar los hijos de la fila
        val rowComposer = Compose(composer.availableSize)
        val rowCtx = ComposeCtx(rowComposer)

        rowCtx.content()

        composer.add(
            AlignedRowItem(
                children = rowComposer.items,
                alignment = alignment,
                spacing = spacing,
                width = width
            )
        )
    }
}

fun compose(
    size: ImVec2 = ImGui.getContentRegionAvail(),
    content: ComposeCtx.() -> Unit
): Compose {
    val composer = Compose(size)
    val ctx = ComposeCtx(composer)

    ctx.content()
    return composer
}

fun composeRender(
    size: ImVec2 = ImGui.getContentRegionAvail(),
    content: ComposeCtx.() -> Unit
) = compose(size, content).renderAll()