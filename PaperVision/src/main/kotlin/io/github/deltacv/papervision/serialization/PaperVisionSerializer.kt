package io.github.deltacv.papervision.serialization

import com.google.gson.JsonElement
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.vision.InputMatNode
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.data.DataSerializer

object PaperVisionSerializer {

    fun serialize(nodes: List<Node<*>>, links: List<Link>): String {
        val serializables = mutableMapOf<String, List<DataSerializable<*>>>()
        serializables["nodes"] = nodes.filter { it.shouldSerialize }
        serializables["links"] = links.filter { it.shouldSerialize }

        return DataSerializer.serialize(serializables)
    }

    fun serializeToTree(nodes: List<Node<*>>, links: List<Link>): JsonElement {
        val serializables = mutableMapOf<String, List<DataSerializable<*>>>()
        serializables["nodes"] = nodes.filter { it.shouldSerialize }
        serializables["links"] = links.filter { it.shouldSerialize }

        return DataSerializer.serializeToTree(serializables)
    }

    private fun deserialize(obj: JsonElement?, json: String?, paperVision: PaperVision?): PaperVisionData {
        val data = if(obj != null) {
            DataSerializer.deserialize(obj)
        } else {
            DataSerializer.deserialize(json!!)
        }

        val nodes = mutableListOf<Node<*>>()
        val links = mutableListOf<Link>()

        paperVision?.let {
            for(node in it.nodes.inmutable) {
                if(node != it.nodeEditor.originNode) // everything freaking breaks if we delete this thing
                    node.forceDelete()
            }

            for(link in it.links.inmutable) {
                link.delete()
            }
        }

        var createdOutputNode = false
        var createdInputNode = false

        val nodesData = data["nodes"]
        if(nodesData != null) {
            for(node in nodesData) {
                if(node is Node<*>) {
                    // applying the inputmatnode and outputmatnode positions in case they passed a node editor
                    if(paperVision != null) {
                        when (node) {
                            is InputMatNode -> {
                                paperVision.nodeEditor.inputNode = node
                                createdInputNode = true
                            }
                            is OutputMatNode -> {
                                paperVision.nodeEditor.outputNode = node
                                createdOutputNode = true
                            }
                        }
                        node.enable()
                    }

                    nodes.add(node)
                }
            }
        }

        paperVision?.let {
            if(!createdInputNode) {
                it.nodeEditor.inputNode = InputMatNode().apply { enable() }
            }
            if(!createdOutputNode) {
                it.nodeEditor.outputNode = OutputMatNode().apply { enable() }
            }

            it.nodeEditor.inputNode.ensureAttributeExists()
            it.nodeEditor.outputNode.ensureAttributeExists()
        }

        val linksData = data["links"]
        if(linksData != null) {
            for(link in linksData) {
                if(link is Link) {
                    if(paperVision != null) {
                        link.enable()
                    }
                    links.add(link)
                }
            }
        }

        return PaperVisionData(nodes, links)
    }

    fun deserialize(json: String) = deserialize(null, json, null)
    fun deserialize(obj: JsonElement) = deserialize(obj, null, null)

    fun deserializeAndApply(json: String, paperVision: PaperVision) = deserialize(null, json, paperVision)
    fun deserializeAndApply(obj: JsonElement, paperVision: PaperVision) = deserialize(obj, null, paperVision)

}

data class PaperVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)