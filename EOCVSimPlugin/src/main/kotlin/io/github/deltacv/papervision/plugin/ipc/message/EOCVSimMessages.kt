package io.github.deltacv.papervision.plugin.ipc.message

import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageBase

data class InputSourceData(
    var name: String,
    var type: InputSourceType
)
enum class InputSourceType {
    IMAGE, CAMERA, VIDEO
}

class GetInputSourcesMessage : PaperVisionEngineMessageBase()

class GetCurrentInputSourceMessage : PaperVisionEngineMessageBase()

class SetInputSourceMessage(
    var inputSource: String
) : PaperVisionEngineMessageBase()