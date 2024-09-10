package io.github.deltacv.papervision.serialization.ev

import com.google.gson.JsonElement
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.vision.InputMatNode
import io.github.deltacv.papervision.node.vision.OutputMatNode
import io.github.deltacv.papervision.serialization.data.DataSerializable
import io.github.deltacv.papervision.serialization.data.DataSerializer

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

    private fun deserialize(obj: JsonElement?, json: String?, paperVision: PaperVision?): EasyVisionData {
        val data = if(obj != null) {
            DataSerializer.deserialize(obj)
        } else {
            DataSerializer.deserialize(json!!)
        }

        val nodes = mutableListOf<Node<*>>()
        val links = mutableListOf<Link>()

        paperVision?.apply {
            nodes.clear()
            attributes.clear()
            links.clear()
        }

        val nodesData = data["nodes"]
        if(nodesData != null) {
            for(node in nodesData) {
                if(node is Node<*>) {
                    // applying the inputmatnode and outputmatnode positions in case they passed a node editor
                    if(paperVision != null) {
                        when (node) {
                            is InputMatNode -> paperVision.nodeEditor.inputNode = node
                            is OutputMatNode -> paperVision.nodeEditor.outputNode = node
                        }
                    }

                    if(paperVision != null) {
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
                    if(paperVision != null) {
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

    fun deserializeAndApply(json: String, paperVision: PaperVision) = deserialize(null, json, paperVision)
    fun deserializeAndApply(obj: JsonElement, paperVision: PaperVision) = deserialize(obj, null, paperVision)

}

data class EasyVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)