/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.engine.message

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class PaperVisionEngineMessageBase(
    override var persistent: Boolean = false
) : PaperVisionEngineMessage {

    companion object {
        private var idCount = AtomicInteger(-1)

        fun nextId() = idCount.incrementAndGet()
    }

    @Transient
    private val onTimeoutCallbacks = Collections.synchronizedList(mutableListOf<TimeoutCallback>())
    @Transient
    private val onResponseCallbacks = Collections.synchronizedList(mutableListOf<OnResponseCallback>())

    override var id = nextId()

    override fun acceptResponse(response: PaperVisionEngineMessageResponse) {
        for(callback in onResponseCallbacks) {
           callback.onResponse(response)
        }
    }

    override fun acceptElapsedTime(elapsedTimeMillis: Long): PaperVisionEngineMessage {
        var toRemove: MutableList<TimeoutCallback>? = null

        for((timeoutMillis, callback) in onTimeoutCallbacks) {
            if(elapsedTimeMillis >= timeoutMillis) {
                callback()

                if(toRemove == null) {
                    toRemove = mutableListOf()
                }
                toRemove.add(TimeoutCallback(timeoutMillis, callback))
            }
        }

        if(toRemove != null) {
            onTimeoutCallbacks.removeAll(toRemove)
        }

        return this
    }

    override fun onTimeout(timeoutMillis: Long, callback: () -> Unit): PaperVisionEngineMessage {
        onTimeoutCallbacks.add(TimeoutCallback(timeoutMillis, callback))
        return this
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
