package io.github.deltacv.papervision.util

class ElapsedTime {

    var startTime = System.currentTimeMillis()
        private set

    val millis get() = System.currentTimeMillis() - startTime
    val seconds get() = millis.toDouble() / 1000.0

    fun reset() {
        startTime = System.currentTimeMillis()
    }

}