package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.ConValue

object CPythonOpenCvTypes {
    object cv2 : CPythonType("cv2") {
        val RETR_LIST = ConValue(this, "cv2.RETR_LIST")
        val CHAIN_APPROX_SIMPLE = ConValue(this, "cv2.CHAIN_APPROX_SIMPLE")
    }

    val np = CPythonType("numpy", null, "np")

    val npArray = object: CPythonType("np.ndarray") {
        override var actualImport = np
    }
}