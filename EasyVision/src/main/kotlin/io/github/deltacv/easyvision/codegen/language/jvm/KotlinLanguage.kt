package io.github.deltacv.easyvision.codegen.language.jvm

import io.github.deltacv.easyvision.codegen.Visibility
import io.github.deltacv.easyvision.codegen.build.Parameter
import io.github.deltacv.easyvision.codegen.build.Scope
import io.github.deltacv.easyvision.codegen.build.Type
import io.github.deltacv.easyvision.codegen.build.Value
import io.github.deltacv.easyvision.codegen.build.type.KotlinTypes
import io.github.deltacv.easyvision.codegen.csv
import io.github.deltacv.easyvision.codegen.language.LanguageBase

object KotlinLanguage : LanguageBase(usesSemicolon = false) {

    init {
        excludedImports.add("kotlin.Unit")
    }

    override val VoidType get() = KotlinTypes.Unit
    override val Parameter.string get() = "$name: ${type.shortNameWithGenerics}"

    override fun instanceVariableDeclaration(
        vis: Visibility,
        name: String,
        variable: Value,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {

        val ending = if(variable.value != null) "= ${variable.value}" else ""

        var modifiers = if(isFinal) "val" else "var"
        if(vis != Visibility.PUBLIC) {
            modifiers = "${vis.name.lowercase()} " + modifiers
        }

        return "$modifiers $name: ${variable.type.shortNameWithGenerics} $ending"
    }

    override fun localVariableDeclaration(name: String, variable: Value, isFinal: Boolean): String {
        val ending = if(variable.value != null) "= ${variable.value}" else ""

        return "${if(isFinal) "val" else "var"} $name: ${variable.type.shortNameWithGenerics}  $ending"
    }

    override fun methodDeclaration(
        vis: Visibility,
        returnType: Type,
        name: String,
        vararg parameters: Parameter,
        isStatic: Boolean,
        isFinal: Boolean,
        isOverride: Boolean
    ): Pair<String?, String> {
        val visibility = if(vis != Visibility.PUBLIC) "${vis.name.lowercase()} " else ""
        val open = if(!isFinal) "open " else ""
        val returnTypeStr = if(returnType != VoidType) ": ${returnType.shortName}" else ""

        return Pair("",
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
        implements: Array<Type>?,
        isStatic: Boolean,
        isFinal: Boolean
    ): String {
        val visibility = if(vis != Visibility.PUBLIC) "${vis.name.lowercase()} " else ""
        val open = if(isFinal) "" else "open "

        val extendsAndImplementsList = mutableListOf<String>()

        if(extends != null) extendsAndImplementsList.add("${extends.shortNameWithGenerics}()")

        if(implements != null) {
            for(type in implements) {
                extendsAndImplementsList.add(type.shortNameWithGenerics)
            }
        }

        val extendsAndImplements = if(extendsAndImplementsList.isNotEmpty()) {
            " : ${extendsAndImplementsList.toTypedArray().csv()}"
        } else ""

        return "$visibility${open}class $name$extendsAndImplements "
    }

    override fun enumClassDeclaration(name: String, vararg values: String) = "enum class $name { ${values.csv() } "

    override fun new(type: Type, vararg parameters: Value) = Value(
        type, "${type.shortNameWithGenerics}(${parameters.csv()})"
    )

}