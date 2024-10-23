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

import com.google.gson.*
import java.lang.reflect.Type

private val gson = Gson()

open class PolymorphicAdapter<T>(val name: String) : JsonSerializer<T>, JsonDeserializer<T> {

    override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        obj.addProperty("${name}Class", src!!::class.java.name)
        obj.add(name, gson.toJsonTree(src))

        return obj
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
        val className = json.asJsonObject.get("${name}Class").asString
        val clazz = Class.forName(className)

        return gson.fromJson(json.asJsonObject.get(name), clazz) as T
    }

}