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

package io.github.deltacv.papervision.serialization.data.adapter

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

object SerializeIgnoreExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes): Boolean {
        // Check if the field or its class has the SerializeIgnore annotation
        return f.declaredClass.isAnnotationPresent(SerializeIgnore::class.java) ||
                f.annotations.any { it.annotationClass.java.isAnnotationPresent(SerializeIgnore::class.java) }
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        // Check if the class has the SerializeIgnore annotation
        return clazz.isAnnotationPresent(SerializeIgnore::class.java)
    }
}
