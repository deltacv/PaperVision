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

package io.github.deltacv.papervision.serialization.data

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.github.deltacv.papervision.serialization.data.adapter.DataSerializableAdapter
import io.github.deltacv.papervision.serialization.data.adapter.SerializeIgnoreExclusionStrategy
import io.github.deltacv.papervision.serialization.data.adapter.dataSerializableGson

object DataSerializer {

    val type = object : TypeToken<Map<String, List<DataSerializable<*>>>>() {}.type

    fun serialize(serializables: Map<String, List<DataSerializable<*>>>): String {
        return dataSerializableGson.toJson(serializables)
    }

    fun serializeToTree(serializables: Map<String, List<DataSerializable<*>>>): JsonElement {
        return dataSerializableGson.toJsonTree(serializables)
    }

    fun deserialize(data: String): Map<String, List<DataSerializable<*>>> {
        return dataSerializableGson.fromJson(data, type)
    }

    fun deserialize(obj: JsonElement): Map<String, List<DataSerializable<*>>> {
        return dataSerializableGson.fromJson(obj, type)
    }

}

@Target(AnnotationTarget.FIELD)
annotation class SerializeData

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class SerializeIgnore
