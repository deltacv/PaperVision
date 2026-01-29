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

package io.github.deltacv.papervision.annotation

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.deltacv.papervision.node.PaperNode

class PaperNodeAnnotationProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val packageName: String = environment.options["paperNodeClassesMetadataPackage"] ?: "io.github.deltacv.papervision.node"
    private val fileName: String = environment.options["paperNodeClassesMetadataClassName"] ?: "NodeClassesMetadata"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedSymbols = resolver.getSymbolsWithAnnotation(PaperNode::class.qualifiedName!!)

        val classList = annotatedSymbols
            .filterIsInstance<KSClassDeclaration>()

        generateClassListFile(classList)

        return emptyList()
    }

    private fun generateClassListFile(classes: Sequence<KSClassDeclaration>) {
        val classNames = classes.map { it.qualifiedName!!.asString() }.toList()
        val sourceFiles = classes.map { it.containingFile!! }.toList()

        val fileSpec = FileSpec.builder(packageName, fileName)
            .addType(TypeSpec.objectBuilder(fileName)
                .addProperty(PropertySpec.builder("classList", List::class.asTypeName().parameterizedBy(String::class.asTypeName()))
                    .initializer("listOf(${classNames.joinToString { "\"$it\"" }})")
                    .build())
                .build())
            .build()

        val dependencies = Dependencies(aggregating = true, *sourceFiles.toTypedArray())

        try {
            val file = environment.codeGenerator.createNewFile(
                dependencies,
                packageName = packageName,
                fileName = fileName,
                extensionName = "kt"
            )
            file.bufferedWriter().use { writer ->
                fileSpec.writeTo(writer)
            }
        } catch(_: Exception) {
            // ignore
        }
    }
}
