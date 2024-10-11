package io.github.deltacv.papervision.codegen.build.type

import io.github.deltacv.papervision.codegen.build.Type

object CPythonOpenCvTypes {

    val cv2 = object: Type("cv2", "cv2") {
        override val shouldImport = true
    }

}