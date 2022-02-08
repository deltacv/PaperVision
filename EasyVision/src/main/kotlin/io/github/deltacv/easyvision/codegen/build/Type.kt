package io.github.deltacv.easyvision.codegen.build

import io.github.deltacv.easyvision.codegen.csv

data class Type(val className: String, val packagePath: String = "", val generics: Array<Type>? = null) {

    companion object {
        val NONE = Type("", "")
    }

    val hasGenerics get() = generics != null && generics.isNotEmpty()

    val shouldImport get() = className != packagePath

    val shortNameWithGenerics get() =
        if(generics != null)
            "$className<${generics.csv()}>"
        else className

}

private val typeCache = mutableMapOf<Class<*>, Type>()

val Any.genType: Type get() = this::class.java.genType

val Class<*>.genType: Type get() {
    if(!typeCache.containsKey(this)) {
        typeCache[this] = Type(simpleName, getPackage()?.name ?: "")
    }

    return typeCache[this]!!
}