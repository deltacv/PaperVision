package org.deltacv.papervision.serialization.v2

import org.deltacv.papervision.PaperVision
import org.deltacv.papervision.node.FlagsNode
import org.deltacv.papervision.node.Link
import org.deltacv.papervision.node.Node
import org.deltacv.papervision.node.vision.InputMatNode
import org.deltacv.papervision.node.vision.OutputMatNode
import org.deltacv.papervision.util.loggerForThis

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

    val logger by loggerForThis()

    override fun encode(encoder: DataEncoder) {
        encoder.objList("nodes", nodes)
        encoder.objList("links", links)
    }

    override fun decode(decoder: DataDecoder) {
        val decodedNodes = decoder.objList("nodes").map {
            it as? Node<*> ?: throw MalformedDataException("Decoded object is not a PaperVision Node", it)
        }

        nodes.clear()
        nodes.addAll(decodedNodes)

        val decodedLinks = decoder.objList("links").map {
            it as? Link ?: throw MalformedDataException("Decoded object is not a PaperVision Link", it)
        }

        links.clear()
        links.addAll(decodedLinks)
    }

    fun apply(paperVision: PaperVision) {
        logger.info("Loading project with ${nodes.size} nodes and ${links.size} links")

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
                    if(createdInputNode) {
                        throw IllegalStateException("Only one InputMatNode can be present in the node editor.")
                    }
                    paperVision.nodeEditor.inputNode = node
                    createdInputNode = true
                }

                is OutputMatNode -> {
                    if(createdOutputNode) {
                        throw IllegalStateException("Only one OutputMatNode can be present in the node editor.")
                    }
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
            // If the project doesn't have an input node, create a default one to ensure the node editor is in a valid state
            paperVision.nodeEditor.inputNode = InputMatNode().apply { enable() }
        }
        if (!createdOutputNode) {
            // If the project doesn't have an output node, create a default one to ensure the node editor is in a valid state
            paperVision.nodeEditor.outputNode = OutputMatNode().apply { enable() }
        }

        paperVision.nodeEditor.inputNode.ensureAttributeExists()
        paperVision.nodeEditor.outputNode.ensureAttributeExists()

        paperVision.onUpdate.once {
            for (link in links) {
                if (link.aAttrib != null && link.bAttrib != null) {
                    link.enable()
                } else {
                    logger.debug("Cleaning up orphaned link during project application: {}", link)
                    link.delete()
                }
            }
            paperVision.onDeserialization.run()
        }
    }
}