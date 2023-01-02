package io.github.deltacv.easyvision.codegen.language.jvm

import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.codegen.build.type.JavaTypes
import io.github.deltacv.easyvision.codegen.build.type.KotlinTypes
import io.github.deltacv.easyvision.codegen.csv
import io.github.deltacv.easyvision.codegen.language.LanguageBase

object KotlinLanguage : LanguageBase(usesSemicolon = false) {

    init {
        mutableExcludedImports.add(KotlinTypes.Unit)
    }

    override val BooleanType get() = KotlinTypes.Boolean

    override val IntType get() = KotlinTypes.Int
    override val LongType get() = KotlinTypes.Long
    override val FloatType get() = KotlinTypes.Float
    override val DoubleType get() = KotlinTypes.Double

    override val VoidType get() = KotlinTypes.Unit

    override val Parameter.string get() = "$name: ${type.shortNameWithGenerics}"

    override fun instanceVariableDeclaration(
        vis: Visibility,
        variable: Variable,
        label: String?,
        isStatic: Boolean,
        isFinal: Boolean
    ): Pair<String?, String> {
        val ending = if(variable.variableValue.value != null) "= ${variable.variableValue.value}" else ""

        var modifiers = if(isFinal) "val" else "var"
        if(vis != Visibility.PUBLIC) {
            modifiers = "${vis.name.lowercase()} " + modifiers
        }

        return Pair(
            if(label != null) {
                variable.additionalImports(JavaTypes.LabelAnnotation)
                "@Label(name = \"$label\")"
            } else null,
            "$modifiers ${variable.name}: ${variable.type.shortNameWithGenerics} $ending"
        )
    }

    override fun localVariableDeclaration(variable: Variable, isFinal: Boolean): String {
        val ending = if(variable.variableValue.value != null) " = ${variable.variableValue.value}" else ""

        return "${if(isFinal) "val" else "var"} ${variable.name}: ${variable.type.shortNameWithGenerics}$ending"
    }

    override fun methodDeclaration(
        vis: Visibility,
        returnType: Type,
        name: String,
        vararg parameters: Parameter,
        isStatic: Boolean,
        isFinal: Boolean,
        isSynchronized: Boolean,
        isOverride: Boolean
    ): Pair<String?, String> {
        val visibility = if(vis != Visibility.PUBLIC) "${vis.name.lowercase()} " else ""
        val open = if(!isFinal) "open " else ""
        val returnTypeStr = if(returnType != VoidType) ": ${returnType.className}" else ""

        return Pair(
            if(isSynchronized) "@Synchronized" else "",
            "${if(isOverride) "override " else ""}$visibility${open}fun $name(${parameters.csv()})$returnTypeStr"
        )
    }

    override fun foreachLoopDeclaration(variable: Value, iterable: Value) =
        "for(${variable.value} in ${iterable.value})"


    override fun classDeclaration(
        vis: Visibility,
        name: String,
        body: Scope,
        extends: Type?,
        vararg implements: Type,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        val visibility = if(vis != Visibility.PUBLIC) "${vis.name.lowercase()} " else ""
        val open = if(isFinal) "" else "open "

        val extendsAndImplementsList = mutableListOf<String>()

        if(extends != null) extendsAndImplementsList.add("${extends.shortNameWithGenerics}()")

        for(type in implements) {
            extendsAndImplementsList.add(type.shortNameWithGenerics)
        }

        val extendsAndImplements = if(extendsAndImplementsList.isNotEmpty()) {
            " : ${extendsAndImplementsList.toTypedArray().csv()}"
        } else ""

        return "$visibility${open}class $name$extendsAndImplements "
    }

    override fun enumClassDeclaration(name: String, vararg values: String) = "enum class $name { ${values.csv() } "

    override fun new(type: Type, vararg parameters: Value) = ConValue(
        type, "${type.shortNameWithGenerics}(${parameters.csv()})"
    )

}