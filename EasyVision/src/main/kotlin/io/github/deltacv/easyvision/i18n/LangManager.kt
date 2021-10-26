package io.github.deltacv.easyvision.i18n

import com.github.serivesmejia.eocvsim.util.Log
import com.opencsv.CSVReader
import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader
import java.util.*

object LangManager {

    const val TAG = "LangManager"

    var langFile = "/lang.csv"
        set(value) {
            field = value
            load()
        }

    var lang = "es"
        set(value) {
            field = value
            load()
        }

    var langIndex = -1
        private set

    private lateinit var csv: List<Array<String>>

    fun get(key: String): String? {
        loadIfNeeded()

        for(line in csv) {
            if(line[0] == key) {
                return line[langIndex]
            }
        }

        return null
    }

    fun loadIfNeeded() {
        if(!::csv.isInitialized) {
            load()
        }
    }

    fun load() {
        Log.info(TAG, "Loading \"$lang\" language from $langFile file")

        val reader = try {
            BufferedReader(InputStreamReader(javaClass.getResourceAsStream(langFile)))
        } catch(ignored: Exception) {
            FileReader(langFile)
        }

        csv = CSVReader(reader).readAll()

        if(csv.isEmpty()) {
            throw IllegalArgumentException("The $langFile file is empty")
        }

        for((i, lang) in csv[0].withIndex()) {
            if(lang == this.lang) {
                langIndex = i
                Log.info(TAG, "Successfully loaded \"$lang\" language with index $i")
                return // if we were able to find the language
            }
        }

        throw IllegalArgumentException("The language \"$lang\" does not exist in the $langFile file")
    }

}

private val variableRegex = Regex("\\$\\[(.*?)]")
private val trCache = WeakHashMap<String, String>()

fun tr(text: String): String {
    if(trCache.containsKey(text)){
        return trCache[text]!!
    }

    val matches = variableRegex.findAll(text)
    var finalTxt = text

    var hasMatches = false
    for(match in matches) {
        hasMatches = true

        val trValue = LangManager.get(match.groupValues[1]) ?: continue
        finalTxt = finalTxt.replace(match.value, trValue)
    }

    if(!hasMatches) {
        val trValue = LangManager.get(text)
        if(trValue != null) {
            finalTxt = trValue
        }
    }

    trCache[text] = finalTxt

    return finalTxt
}