package io.github.deltacv.easyvision.serialization.data

import imgui.ImVec2

abstract class AttributeSerializationData {
    open var id: Int = 0
}

class BasicAttribData(id: Int) : AttributeSerializationData() {
    init {
        this.id = id
    }
}

abstract class NodeSerializationData {
    open var id: Int = 0
    open var nodePos: ImVec2 = ImVec2(0f, 0f)
}

class BasicNodeData(
    id: Int,
    nodePos: ImVec2
) : NodeSerializationData() {

    init {
        this.id = id
        this.nodePos = nodePos
    }

}

data class LinkSerializationData(
    var from: Int,
    var to: Int
)
