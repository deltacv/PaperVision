package io.github.deltacv.easyvision.serialization.ev

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

    fun deserializeAndApply(json: String, nodeEditor: NodeEditor? = null): EasyVisionData {
        val data = DataSerializer.deserialize(json)

        val nodes = mutableListOf<Node<*>>()
        val links = mutableListOf<Link>()

        val nodesData = data["nodes"]
        if(nodesData != null) {
            for(node in nodesData) {
                if(node is Node<*>) {
                    // applying the inputmatnode and outputmatnode positions in case they passed a node editor
                    if(nodeEditor != null) {
                        when (node) {
                            is InputMatNode -> nodeEditor.inputNode = node
                            is OutputMatNode -> nodeEditor.outputNode = node
                            //else -> nodes.add(node) // do not add the inputmat and outputmat nodes to the list
                        }
                    }

                    nodes.add(node)
                }
            }
        }

        val linksData = data["links"]
        if(linksData != null) {
            for(link in linksData) {
                if(link is Link) {
                    links.add(link)
                }
            }
        }

        return EasyVisionData(nodes, links)
    }

}

data class EasyVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)