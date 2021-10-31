package io.github.deltacv.easyvision.serialization

import io.github.deltacv.easyvision.gui.NodeEditor
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
import io.github.deltacv.easyvision.node.vision.InputMatNode
import io.github.deltacv.easyvision.node.vision.OutputMatNode
import io.github.deltacv.easyvision.serialization.data.interfaces.DataSerializable
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

    fun deserialize(json: String, nodeEditor: NodeEditor? = null) {
        val data = DataSerializer.deserialize(json)

        val nodes = mutableListOf<Node<*>>()
        val links = mutableListOf<Link>()

        val nodesData = data["nodes"]
        if(nodesData != null) {
            for((i, node) in nodesData.withIndex()) {
                if(node is Node<*>) {
                    // applying the inputmatnode and outputmatnode positions in case they passed a node editor
                    if(nodeEditor != null) {
                        if(node is InputMatNode) {

                        } else if(node is OutputMatNode) {

                        } else {
                            nodes.add(node) // do not add the inputmat and outputmat nodes to the list
                        }
                    } else {
                        nodes.add(node)
                    }
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
    }

}

data class EasyVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)