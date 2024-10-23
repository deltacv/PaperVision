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

package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.ConValue

object CPythonOpenCvTypes {
    object cv2 : CPythonType("cv2") {
        val RETR_LIST = ConValue(this, "cv2.RETR_LIST").apply {
            additionalImports(this)
        }

        val RETR_EXTERNAL = ConValue(this, "cv2.RETR_EXTERNAL").apply {
            additionalImports(this)
        }

        val CHAIN_APPROX_SIMPLE = ConValue(this, "cv2.CHAIN_APPROX_SIMPLE").apply {
            additionalImports(this)
        }

        val MORPH_RECT = ConValue(this, "cv2.MORPH_RECT").apply {
            additionalImports(this)
        }

        val contourArea = ConValue(this, "cv2.contourArea").apply {
            additionalImports(this)
        }
    }

    val np = CPythonType("numpy", null, "np")

    val npArray = object: CPythonType("np.ndarray") {
        override var actualImport = np
    }
}