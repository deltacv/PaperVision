package io.github.deltacv.easyvision.serialization.ev

import com.google.gson.JsonElement
import io.github.deltacv.easyvision.gui.NodeEditor
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
import io.github.deltacv.easyvision.node.vision.InputMatNode
import io.github.deltacv.easyvision.node.vision.OutputMatNode
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import io.github.deltacv.easyvision.serialization.data.DataSerializer

object EasyVisionSerializer {

    fun serialize(nodes: List<Node<*>>, links: List<Link>): String {
        val serializables = mutableMapOf<String, List<DataSerializable<*>>>()
        serializables["nodes"] = nodes
        serializables["links"] = links

        return DataSerializer.serialize(serializables)
    }

    fun serializeCurrent() = serialize(
        nodes = Node.nodes.elements,
        links = Link.links.elements
    )

    private fun deserialize(obj: JsonElement?, json: String?, apply: Boolean, nodeEditor: NodeEditor?): EasyVisionData {
        val data = if(obj != null) {
            DataSerializer.deserialize(obj)
        } else {
            DataSerializer.deserialize(json!!)
        }

        val nodes = mutableListOf<Node<*>>()
        val links = mutableListOf<Link>()

        if(apply) {
            Node.nodes.clear()
            Node.attributes.clear()
            Link.links.clear()
        }

        val nodesData = data["nodes"]
        if(nodesData != null) {
            for(node in nodesData) {
                if(node is Node<*>) {
                    // applying the inputmatnode and outputmatnode positions in case they passed a node editor
                    if(nodeEditor != null) {
                        when (node) {
                            is InputMatNode -> nodeEditor.inputNode = node
                            is OutputMatNode -> nodeEditor.outputNode = node
                        }
                    }

                    if(apply) {
                        node.enable()
                    }
                    nodes.add(node)
                }
            }
        }

        val linksData = data["links"]
        if(linksData != null) {
            for(link in linksData) {
                if(link is Link) {
                    if(apply) {
                        link.enable()
                    }
                    links.add(link)
                }
            }
        }

        return EasyVisionData(nodes, links)
    }

    fun deserialize(json: String) = deserialize(null, json, false, null)
    fun deserialize(obj: JsonElement) = deserialize(obj, null, false, null)

    fun deserializeAndApply(json: String, nodeEditor: NodeEditor) = deserialize(null, json, true, nodeEditor)
    fun deserializeAndApply(obj: JsonElement, nodeEditor: NodeEditor) = deserialize(obj, null, true, nodeEditor)

}

data class EasyVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)