package io.github.deltacv.easyvision.node

import io.github.deltacv.easyvision.EasyVision
import io.github.deltacv.easyvision.gui.style.hexColor
import io.github.deltacv.easyvision.gui.style.rgbaColor

enum class Category(val properName: String,
                    val color: Int = EasyVision.imnodesStyle.titleBar,
                    val colorSelected: Int = EasyVision.imnodesStyle.titleBarHovered) {

    FLOW("cat_pipeline_flow",
        hexColor("#00838f"), // material cyan
        hexColor("#00acc1")),

    CODE("cat_coding"),
    HIGH_LEVEL_CV("cat_high_level_cv"),

    COLOR_OP("cat_color_op",
        hexColor("#ff6f00"), // material amber
        hexColor("#ffa000")),

    SHAPE_DET("cat_shape_det",
        hexColor("#3949ab"), // material indigo
        hexColor("#5c6bc0")),

    OVERLAY("cat_overlay",
        hexColor("#00897b"), // material teal
        hexColor("#26a69a")),

    MATH("cat_math"),
    MISC("cat_misc")

}