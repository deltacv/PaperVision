package io.github.deltacv.papervision.engine.client.message

import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageBase

class PrevizSourceCodeMessage(
    var previzName: String,
    var sourceCode: String
) : PaperVisionEngineMessageBase()

class PrevizPingPongMessage(
    var previzName: String
) : PaperVisionEngineMessageBase()

class PrevizStopMessage(
    var previzName: String
) : PaperVisionEngineMessageBase()

class PrevizSetStreamResolutionMessage(
    var previzName: String,
    var width: Int,
    var height: Int
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