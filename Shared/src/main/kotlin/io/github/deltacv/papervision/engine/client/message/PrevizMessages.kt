package io.github.deltacv.papervision.engine.client.message

import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageBase

class PrevizStartMessage(
    var previzName: String,
    var sourceCode: String,
    var streamWidth: Int,
    var streamHeight: Int
) : PaperVisionEngineMessageBase()

class PrevizSourceCodeMessage(
    var previzName: String,
    var sourceCode: String
) : PaperVisionEngineMessageBase()

class PrevizStopMessage(
    var previzName: String
) : PaperVisionEngineMessageBase()

class PrevizAskNameMessage : PaperVisionEngineMessageBase()

class TunerChangeValueMessage(
    var label: String,
    var index: Int,
    var value: Any
) : PaperVisionEngineMessageBase()

class TunerChangeValuesMessage(
    var label: String,
    var values: Array<*>
) : PaperVisionEngineMessageBase()