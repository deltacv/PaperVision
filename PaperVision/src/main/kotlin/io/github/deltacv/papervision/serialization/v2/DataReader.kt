package io.github.deltacv.papervision.serialization.v2

class MalformedDataException(message: String, obj: Any) : RuntimeException(message)

interface DataReader {

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
    fun objList(name: String, targets: List<DataCodec>): List<DataCodec>          // fixed, pre-existing
    fun objList(name: String, targetFactory: (Int) -> DataCodec): List<DataCodec> // dynamic, pre-existing type but unknown size

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

inline fun <reified T: DataCodec> DataReader.objTyped(name: String): T {
    val obj = obj(name)
    if(obj !is T) {
        throw MalformedDataException("Expected object of type ${T::class}, but got ${obj::class}", obj)
    }

    return obj
}