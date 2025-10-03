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

package io.github.deltacv.papervision.gui.util

import imgui.ImGui
import imgui.ImVec2
import imgui.internal.ImRect
import io.github.deltacv.papervision.PaperVision
import io.github.deltacv.papervision.id.Misc

// this is an extremely hacky "dummy" table which uses the columns api.
// mostly used for the nodes list, because nodes don't respect any sort of
// imgui constraint, whether they are being used within the columns api or anything.
// they are just placed on the default position regardless of anything.
// (that is in fact a good thing for a node editor, but in this case we are trying to
// make a nice looking table by using another node editor to be able to display the nodes
// as they are...)
//
// to use the columns api with the nodes within a node editor, we create the columns as normal,
// except that instead of putting the nodes in the columns directly, we place "dummy" invisible
// elements which have the same size as the node that would correspond in each cell.
//
// later on, we get the position of the dummy element of each cell and set it to the corresponding
// node, which is independent of our table, i could *technically* make the calculations myself to place
// the nodes in their corresponding positions without this bad hack, but hey if imgui can do it for us
// why not use it instead?
//
// ...i hate this so much
class Table(val maxColumns: Int = 4, val drawCallback: ((Int, ImVec2) -> Unit)? = null) {

    private val rects = mutableListOf<Pair<Int, ImVec2>>()
    private val currentPositions = mutableMapOf<Int, ImVec2>()

    private val _currentRects = mutableMapOf<Int, ImRect>()
    val currentRects = _currentRects as Map<Int, ImRect>

    private val columnsId by Misc.newMiscId()

    /**
     * Adds a dummy rectangle to the table with the specified size.
     */
    fun add(id: Int, size: ImVec2) {
        rects.add(Pair(id, size))
    }

    fun contains(id: Int): Boolean {
        for((rectId, _) in rects) {
            if(rectId == id) {
                return true
            }
        }

        return false
    }

    fun setSize(id: Int, size: ImVec2) {
        for((index, pair) in rects.toTypedArray().withIndex()) {
            if(pair.first == id) {
                rects[index] = Pair(pair.first, size)
                break
            }
        }
    }

    fun draw() {
        if(rects.isEmpty()) return

        val columns = if(rects.size >= maxColumns) maxColumns else rects.size

        var index = 0
        var currentColumn: Int

        if(ImGui.beginTable("###$columnsId", columns)) {
            rowLoop@ while(index < rects.size) {
                currentColumn = 0
                ImGui.tableNextRow()

                while(currentColumn < columns) {
                    if(index >= rects.size) break@rowLoop

                    ImGui.tableSetColumnIndex(currentColumn)

                    val id = rects[index].first
                    val size = rects[index].second

                    // our dummy element, it's an invisible button because
                    // i couldn't find anything better with an adjustable
                    // rectangle shape.
                    ImGui.invisibleButton("###$id", size.x, size.y)
                    if(!currentPositions.containsKey(id)) {
                        currentPositions[id] = ImVec2()
                    }

                    val pos = ImGui.getItemRectMin()

                    // store the invisible button position
                    currentPositions[id] = pos

                    _currentRects[id] = ImRect(ImGui.getItemRectMin(), ImGui.getItemRectMax())

                    // call the callback if there's one
                    drawCallback?.invoke(id, pos)

                    index++
                    currentColumn++
                }
            }

            ImGui.endTable()
        }
    }

    /**
     * Gets the absolute position of a dummy rectangle with the specified id
     */
    fun getPos(id: Int) = currentPositions[id]

}