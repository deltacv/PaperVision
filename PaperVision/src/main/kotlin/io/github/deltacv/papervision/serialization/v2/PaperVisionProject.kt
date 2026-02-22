package io.github.deltacv.papervision.serialization.v2

import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node

class PaperVisionProject(
    val nodes: MutableList<Node<*>> = mutableListOf(),
    val links: MutableList<Link> = mutableListOf()
) : DataCodec {

    override fun encode(encoder: DataWriter) {
        encoder.objList("nodes", nodes)
        encoder.objList("links", links)
    }

    override fun decode(decoder: DataReader) {
        val decodedNodes = decoder.objList("nodes").map {
            it as? Node<*> ?: throw IllegalStateException("Decoded object is not a PaperVision Node")
        }

        nodes.clear()
        nodes.addAll(decodedNodes)

        val decodedLinks = decoder.objList("links").map {
            it as? Link ?: throw IllegalStateException("Decoded object is not a PaperVision Link")
        }

        links.clear()
        links.addAll(decodedLinks)
    }
}