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

package io.github.deltacv.papervision.util.event

import io.github.deltacv.papervision.util.loggerOf

class PaperVisionEventHandler(val name: String, val catchExceptions: Boolean = true) : Runnable {

    val logger by loggerOf("${name}-EventHandler")

    private val lock = Any()
    private val onceLock = Any()

    val listeners: Array<EventListener>
        get()  {
            synchronized(lock) {
                return internalListeners.toTypedArray()
            }
        }

    val onceListeners: Array<EventListener>
        get() {
            synchronized(onceLock) {
                return internalOnceListeners.toTypedArray()
            }
        }

    var callRightAway = false

    private val internalListeners     = ArrayList<EventListener>()
    private val internalOnceListeners = ArrayList<EventListener>()

    override fun run() {
        for(listener in listeners) {
            if(catchExceptions) {
                try {
                    runListener(listener, false)
                } catch (ex: Exception) {
                    if(ex is InterruptedException) {
                        logger.warn("Rethrowing InterruptedException...")
                        throw ex
                    } else {
                        logger.error("Error while running listener ${listener.javaClass.name}", ex)
                    }
                }
            } else {
                runListener(listener, false)
            }
        }

        val toRemoveOnceListeners = mutableListOf<EventListener>()

        //executing "doOnce" listeners
        for(listener in onceListeners) {
            if(catchExceptions) {
                try {
                    runListener(listener, true)
                } catch (ex: Exception) {
                    if(ex is InterruptedException) {
                        logger.warn("Rethrowing InterruptedException...")
                        throw ex
                    } else {
                        logger.error("Error while running \"once\" ${listener.javaClass.name}", ex)
                    }
                }
            } else {
                runListener(listener, true)
            }

            toRemoveOnceListeners.add(listener)
        }

        synchronized(onceLock) {
            for(listener in toRemoveOnceListeners) {
                internalOnceListeners.remove(listener)
            }
        }
    }

    fun doOnce(listener: EventListener) {
        if(callRightAway)
            runListener(listener, true)
        else synchronized(onceLock) {
            internalOnceListeners.add(listener)
        }
    }

    fun doOnce(runnable: Runnable) = doOnce { runnable.run() }

    fun doPersistent(listener: EventListener) {
        synchronized(lock) {
            internalListeners.add(listener)
        }

        if(callRightAway) runListener(listener, false)
    }

    fun doPersistent(runnable: Runnable) = doPersistent { runnable.run() }

    fun removePersistentListener(listener: EventListener) {
        if(internalListeners.contains(listener)) {
            synchronized(lock) { internalListeners.remove(listener) }
        }
    }

    fun removeOnceListener(listener: EventListener) {
        if(internalOnceListeners.contains(listener)) {
            synchronized(onceLock) { internalOnceListeners.remove(listener) }
        }
    }

    fun removeAllListeners() {
        removeAllPersistentListeners()
        removeAllOnceListeners()
    }

    fun removeAllPersistentListeners() = synchronized(lock) {
        internalListeners.clear()
    }

    fun removeAllOnceListeners() = synchronized(onceLock) {
        internalOnceListeners.clear()
    }

    operator fun invoke(listener: EventListener) = doPersistent(listener)

    operator fun invoke() = run()

    private fun runListener(listener: EventListener, isOnce: Boolean) =
        listener.run(EventListenerRemover(this, listener, isOnce))

}
