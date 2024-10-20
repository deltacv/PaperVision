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