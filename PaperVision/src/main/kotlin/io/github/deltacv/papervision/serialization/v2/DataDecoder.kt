@file:Suppress("UNUSED")

package io.github.deltacv.papervision.serialization.v2

class MalformedDataException(message: String, obj: Any) : RuntimeException(message)

interface DataDecoder {

    // Primitives
    fun int(name: String): Int
    fun float(name: String): Float
    fun double(name: String): Double
    fun string(name: String): String
    fun bool(name: String): Boolean

    // Single object
    fun obj(name: String): DataCodec
    fun obj(name: String, target: DataCodec)

    // Typed lists
    fun intList(name: String): List<Int>
    fun floatList(name: String): List<Float>
    fun doubleList(name: String): List<Double>
    fun stringList(name: String): List<String>
    fun boolList(name: String): List<Boolean>
    fun objList(name: String): List<DataCodec>
    fun <C: DataCodec> objList(name: String, targets: List<C>): List<C>          // fixed, pre-existing
    fun <C: DataCodec> objList(name: String, targetFactory: (Int) -> C): List<C> // dynamic, pre-existing type but unknown size

    // Entries of each type
    fun intEntries(): Map<String, Int>
    fun floatEntries(): Map<String, Float>
    fun doubleEntries(): Map<String, Double>
    fun boolEntries(): Map<String, Boolean>
    fun stringEntries(): Map<String, String>
    fun objEntries(): Map<String, DataCodec>
    fun intListEntries(): Map<String, List<Int>>
    fun floatListEntries(): Map<String, List<Float>>
    fun doubleListEntries(): Map<String, List<Double>>
    fun boolListEntries(): Map<String, List<Boolean>>
    fun stringListEntries(): Map<String, List<String>>
    fun objListEntries(): Map<String, List<DataCodec>>

    fun has(name: String): Boolean
}

inline fun <reified T: DataCodec> DataDecoder.objTyped(name: String): T {
    val obj = obj(name)
    if(obj !is T) {
        throw MalformedDataException("Expected object of type ${T::class}, but got ${obj::class}", obj)
    }

    return obj
}

fun DataDecoder.intOrNull(name: String): Int? = if (has(name)) int(name) else null
fun DataDecoder.floatOrNull(name: String): Float? = if (has(name)) float(name) else null
fun DataDecoder.doubleOrNull(name: String): Double? = if (has(name)) double(name) else null
fun DataDecoder.stringOrNull(name: String): String? = if (has(name)) string(name) else null
fun DataDecoder.boolOrNull(name: String): Boolean? = if (has(name)) bool(name) else null
fun DataDecoder.objOrNull(name: String): DataCodec? = if (has(name)) obj(name) else null
fun DataDecoder.intListOrNull(name: String): List<Int>? = if (has(name)) intList(name) else null
fun DataDecoder.floatListOrNull(name: String): List<Float>? = if (has(name)) floatList(name) else null
fun DataDecoder.doubleListOrNull(name: String): List<Double>? = if (has(name)) doubleList(name) else null
fun DataDecoder.stringListOrNull(name: String): List<String>? = if (has(name)) stringList(name) else null
fun DataDecoder.boolListOrNull(name: String): List<Boolean>? = if (has(name)) boolList(name) else null
fun DataDecoder.objListOrNull(name: String): List<DataCodec>? = if (has(name)) objList(name) else null