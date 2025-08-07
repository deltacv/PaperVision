/*
 * PaperVision
 * Copyright (C) 2024 Sebastian Erives, deltacv

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.serialization

import com.google.gson.JsonElement
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.node.FlagsNode
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

        if(paperVision != null) {
            for(node in paperVision.nodes.inmutable) {
                if(node != paperVision.nodeEditor.originNode) // everything freaking breaks if we delete this thing
                    node.forceDelete()
            }

            for(link in paperVision.links.inmutable) {
                link.delete()
            }
        }

        var createdOutputNode = false
        var createdInputNode = false

        val nodesData = data["nodes"]

        var hasAddedFlags = false

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
                            is FlagsNode -> {
                                if(hasAddedFlags) {
                                    throw IllegalStateException("Huh? Only one FlagsNode can be present in the node editor.")
                                }

                                paperVision.nodeEditor.flagsNode = node
                                hasAddedFlags = true
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

        paperVision?.onDeserialization?.run()

        return PaperVisionData(nodes, links)
    }

    fun deserialize(json: String) = deserialize(null, json, null)
    fun deserialize(obj: JsonElement) = deserialize(obj, null, null)

    fun deserializeAndApply(json: String, paperVision: PaperVision) = deserialize(null, json, paperVision)
    fun deserializeAndApply(obj: JsonElement, paperVision: PaperVision) = deserialize(obj, null, paperVision)

}

data class PaperVisionData(@JvmField val nodes: List<Node<*>>, @JvmField val links: List<Link>)