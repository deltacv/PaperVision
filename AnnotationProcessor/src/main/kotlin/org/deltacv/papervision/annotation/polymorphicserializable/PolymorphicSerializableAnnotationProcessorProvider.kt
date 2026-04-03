package org.deltacv.papervision.annotation.polymorphicserializable

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class PolymorphicSerializableAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return _root_ide_package_.org.deltacv.papervision.annotation.polymorphicserializable.PolymorphicSerializableAnnotationProcessor(
            environment
        )
    }
}
