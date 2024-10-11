package io.github.deltacv.papervision.codegen.build.type

object CPythonOpenCvTypes {
    val cv2 = CPythonType("cv2")
    val np = CPythonType("numpy", null, "np")

    val npArray = object: CPythonType("np.ndarray") {
        override var actualImport = np
    }
}