package org.deltacv.papervision.annotation.codectype

import com.google.devtools.ksp.processing.*

class CodecTypeAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        _root_ide_package_.org.deltacv.papervision.annotation.codectype.CodecTypeAnnotationProcessor(environment)
}
