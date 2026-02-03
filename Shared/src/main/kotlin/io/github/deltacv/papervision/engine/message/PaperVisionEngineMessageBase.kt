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

package io.github.deltacv.papervision.engine.message

abstract class PaperVisionEngineMessageBase(
    override var persistent: Boolean = false
) : PaperVisionEngineMessage {

    companion object {
        private var idCount = -1

        @Synchronized fun nextId(): Int {
            idCount++
            return idCount
        }
    }

    @Transient
    private val onResponseCallbacks = mutableListOf<OnResponseCallback>()

    override var id = nextId()

    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        for(callback in onResponseCallbacks) {
           callback.onResponse(response)
        }
    }

    override fun onResponse(callback: OnResponseCallback): PaperVisionEngineMessageBase {
        onResponseCallbacks.add(callback)
        return this
    }

    inline fun <reified T : PaperVisionEngineMessageResponse> onResponseWith(crossinline callback: (T) -> Unit) =
        onResponse {
            if(it is T) {
                callback(it)
            }
        }.run { this }

    override fun toString() = "MessageBase(type=\"${this::class.java.typeName}\", id=$id)"

}
