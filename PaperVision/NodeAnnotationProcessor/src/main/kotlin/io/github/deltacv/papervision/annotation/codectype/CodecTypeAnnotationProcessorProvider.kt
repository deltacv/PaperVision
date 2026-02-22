package io.github.deltacv.papervision.annotation.codectype

import com.google.devtools.ksp.processing.*

class CodecTypeAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        CodecTypeAnnotationProcessor(environment)
}