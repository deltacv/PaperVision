/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.node

import io.github.deltacv.papervision.gui.style.CurrentStyles.imnodesStyle
import io.github.deltacv.papervision.gui.style.hexColor

enum class Category(val properName: String,
                    val color: Int = imnodesStyle.titleBar,
                    val colorSelected: Int = imnodesStyle.titleBarHovered) {

    FLOW("cat_pipeline_flow",
        hexColor("#00838f"), // material cyan
        hexColor("#00acc1")),

    CODE("cat_coding"),
    HIGH_LEVEL_CV("cat_high_level_cv"),

    IMAGE_PROC("cat_image_proc",
        hexColor("#ff6f00"), // material amber
        hexColor("#ffa000")),

    FEATURE_DET("cat_feature_det",
        hexColor("#3949ab"), // material indigo
        hexColor("#5c6bc0")),

    CLASSIFICATION("cat_class",
        hexColor("#F9A825"), // material yellow 800
        hexColor("#FDD835")),

    OVERLAY("cat_overlay",
        hexColor("#00897b"), // material teal
        hexColor("#26a69a")),

    MATH("cat_math"),
    MISC("cat_misc")

}