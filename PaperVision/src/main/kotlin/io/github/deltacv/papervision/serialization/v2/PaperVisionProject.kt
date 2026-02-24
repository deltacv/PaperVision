package io.github.deltacv.papervision.serialization.v2

import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.node.FlagsNode
import io.github.deltacv.papervision.node.Link
import io.github.deltacv.papervision.node.Node
import io.github.deltacv.papervision.node.vision.InputMatNode
import io.github.deltacv.papervision.node.vision.OutputMatNode

class PaperVisionProject(
    val nodes: MutableList<Node<*>> = mutableListOf(),
    val links: MutableList<Link> = mutableListOf()
) : DataCodec {

    companion object {
        fun from(paperVision: PaperVision): PaperVisionProject {
            val project = PaperVisionProject()

            project.nodes.addAll(paperVision.nodes.inmutable)
            project.links.addAll(paperVision.links.inmutable)

            return project
        }
    }

    override fun encode(encoder: DataEncoder) {
        encoder.objList("nodes", nodes)
        encoder.objList("links", links)
    }

    override fun decode(decoder: DataDecoder) {
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

    fun apply(paperVision: PaperVision) {
        // clear existing state
        for (node in paperVision.nodes.inmutable) {
            if (node != paperVision.nodeEditor.originNode)
                node.forceDelete()
        }
        for (link in paperVision.links.inmutable) {
            link.delete()
        }

        var createdInputNode = false
        var createdOutputNode = false
        var hasAddedFlags = false

        for (node in nodes) {
            when (node) {
                is InputMatNode -> {
                    paperVision.nodeEditor.inputNode = node
                    createdInputNode = true
                }
                is OutputMatNode -> {
                    paperVision.nodeEditor.outputNode = node
                    createdOutputNode = true
                }
                is FlagsNode -> {
                    if (hasAddedFlags) {
                        throw IllegalStateException("Only one FlagsNode can be present in the node editor.")
                    }
                    paperVision.nodeEditor.flagsNode = node
                    hasAddedFlags = true
                }
            }
            node.enable()
        }

        if (!createdInputNode) {
            paperVision.nodeEditor.inputNode = InputMatNode().apply { enable() }
        }
        if (!createdOutputNode) {
            paperVision.nodeEditor.outputNode = OutputMatNode().apply { enable() }
        }

        paperVision.nodeEditor.inputNode.ensureAttributeExists()
        paperVision.nodeEditor.outputNode.ensureAttributeExists()

        paperVision.onUpdate.once {
            for (link in links) {
                link.enable()
            }
            paperVision.onDeserialization.run()
        }
    }
}