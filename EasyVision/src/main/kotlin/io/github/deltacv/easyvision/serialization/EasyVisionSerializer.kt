package io.github.deltacv.easyvision.serialization

import com.google.gson.GsonBuilder
import io.github.deltacv.easyvision.node.Link
import io.github.deltacv.easyvision.node.Node
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

}

data class EasyVisionSerialization(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)