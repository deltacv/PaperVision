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

package io.github.deltacv.papervision.node

import io.github.deltacv.papervision.serialization.v1.data.SerializeData
import io.github.deltacv.papervision.serialization.v2.CodecType
import io.github.deltacv.papervision.serialization.v2.DataReader
import io.github.deltacv.papervision.serialization.v2.DataWriter

@PaperNode(
    name = "Flags",
    description = "A node that holds flags",
    category = NodeCategory.MISC,
    showInList = false
)
@CodecType
class FlagsNode : InvisibleNode() {

    override val requestedId = 171

    @SerializeData
    val flags = mutableMapOf<String, Boolean>()

    @SerializeData
    val numFlags = mutableMapOf<String, Double>()

    override fun encode(encoder: DataWriter) {
        super.encode(encoder)
        encoder.unignore() // InvisibleNode requests ignore by default, we dont really want that anymore

        for((key, value) in flags) {
            encoder.bool("f_$key", value)
        }

        for((key, value) in numFlags) {
            encoder.double("n_$key", value)
        }
    }

    override fun decode(decoder: DataReader) {
        super.decode(decoder)

        val flagsKeys = decoder.boolEntries()
        for((key, value) in flagsKeys) {
            val flagKey = key.removePrefix("f_")
            flags[flagKey] = value
        }

        val numFlagsKeys = decoder.doubleEntries()
        for((key, value) in numFlagsKeys) {
            val flagKey = key.removePrefix("n_")
            numFlags[flagKey] = value
        }
    }

}
