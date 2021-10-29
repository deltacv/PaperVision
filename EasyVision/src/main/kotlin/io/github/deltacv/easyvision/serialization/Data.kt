package io.github.deltacv.easyvision.serialization

import imgui.ImVec2
import io.github.deltacv.easyvision.node.Node


abstract class AttributeSerializationData {
    open val id: Int = 0
}

class NoAttribData(override val id: Int) : AttributeSerializationData()

abstract class NodeSerializationData {
    open var id: Int = 0
    open var node: Node<*, *>? = null
    open var nodePos: ImVec2 = ImVec2(0f, 0f)
}

class NoNodeData(
    override var id: Int,
    node: Node<*, *>,
    override var nodePos: ImVec2
) : NodeSerializationData() {
    override var node: Node<*, *>? = node
}