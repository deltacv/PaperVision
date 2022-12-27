package io.github.deltacv.easyvision.serialization.ev

import com.google.gson.JsonElement
import io.github.deltacv.easyvision.EasyVision
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

    fun serializeToTree(nodes: List<Node<*>>, links: List<Link>): JsonElement {
        val serializables = mutableMapOf<String, List<DataSerializable<*>>>()
        serializables["nodes"] = nodes
        serializables["links"] = links

        return DataSerializer.serializeToTree(serializables)
    }

    private fun deserialize(obj: JsonElement?, json: String?, easyVision: EasyVision?): EasyVisionData {
        val data = if(obj != null) {
            DataSerializer.deserialize(obj)
        } else {
            DataSerializer.deserialize(json!!)
        }

        val nodes = mutableListOf<Node<*>>()
        val links = mutableListOf<Link>()

        easyVision?.apply {
            nodes.clear()
            attributes.clear()
            links.clear()
        }

        val nodesData = data["nodes"]
        if(nodesData != null) {
            for(node in nodesData) {
                if(node is Node<*>) {
                    // applying the inputmatnode and outputmatnode positions in case they passed a node editor
                    if(easyVision != null) {
                        when (node) {
                            is InputMatNode -> easyVision.nodeEditor.inputNode = node
                            is OutputMatNode -> easyVision.nodeEditor.outputNode = node
                        }
                    }

                    if(easyVision != null) {
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
                    if(easyVision != null) {
                        link.enable()
                    }
                    links.add(link)
                }
            }
        }

        return EasyVisionData(nodes, links)
    }

    fun deserialize(json: String) = deserialize(null, json, null)
    fun deserialize(obj: JsonElement) = deserialize(obj, null, null)

    fun deserializeAndApply(json: String, easyVision: EasyVision) = deserialize(null, json, easyVision)
    fun deserializeAndApply(obj: JsonElement, easyVision: EasyVision) = deserialize(obj, null, easyVision)

}

data class EasyVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)