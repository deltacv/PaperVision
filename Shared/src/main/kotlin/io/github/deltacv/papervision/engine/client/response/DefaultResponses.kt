package io.github.deltacv.papervision.engine.client.response

import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

open class OkResponse(val info: String = "") : PaperVisionEngineMessageResponse() {
    override val status = true

    override fun toString(): String {
        return "OkResponse(type=\"${this::class.java.typeName}\", info=\"$info\")"
    }
}

open class ErrorResponse(val reason: String, val stackTrace: Array<String>? = null) : PaperVisionEngineMessageResponse() {
    override val status = false

    constructor(reason: String, throwable: Throwable) : this(reason, throwable.stackTrace.map { it.toString() }.toTypedArray())

    override fun toString(): String {
        return "ErrorResponse(type=\"${this::class.java.typeName}\", reason=\"$reason\", exception=\"${stackTrace?.getOrNull(0)}\")"
    }
}