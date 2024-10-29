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

package io.github.deltacv.papervision.plugin.ipc.serialization

import com.github.serivesmejia.eocvsim.util.serialization.PolymorphicAdapter
import com.google.gson.GsonBuilder
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessage
import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageResponse

object IpcGson {
    object IpcMessageAdapter : PolymorphicAdapter<PaperVisionEngineMessage>("message", IpcGson::class.java.classLoader)
    object IpcMessageResponseAdapter : PolymorphicAdapter<PaperVisionEngineMessageResponse>("messageResponse", IpcGson::class.java.classLoader)

    object AnyAdapter : PolymorphicAdapter<Any>("mack")

    val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(PaperVisionEngineMessage::class.java, IpcMessageAdapter)
        .registerTypeHierarchyAdapter(PaperVisionEngineMessageResponse::class.java, IpcMessageResponseAdapter)
        .create()
}

val ipcGson = IpcGson.gson