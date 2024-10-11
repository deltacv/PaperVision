package io.github.deltacv.papervision.codegen.build

import io.github.deltacv.papervision.codegen.csv

open class Type(
    val className: String,
    open val packagePath: String = "",
    val generics: Array<Type>? = null,

    open val actualImport: Type? = null,
    val isArray: Boolean = false
) {

    companion object {
        val NONE = Type("", "")
    }

    val hasGenerics get() = !generics.isNullOrEmpty()

    open val shouldImport get() = className != packagePath

    val shortNameWithGenerics get() =
        if(generics != null)
            "$className<${generics.csv()}>"
        else className

    override fun toString() = "Type(className=$className, packagePath=$packagePath, actualImport=$actualImport, isArray=$isArray)"

}

private val typeCache = mutableMapOf<Class<*>, Type>()

val Any.genType: Type get() = this::class.java.genType

val Class<*>.genType: Type get() {
    if(!typeCache.containsKey(this)) {
        typeCache[this] = Type(simpleName, getPackage()?.name ?: "")
    }

    return typeCache[this]!!
}

