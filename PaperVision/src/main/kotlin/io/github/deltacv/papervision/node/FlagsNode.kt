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

import io.github.deltacv.papervision.serialization.data.SerializeData

@PaperNode(
    name = "Flags",
    description = "A node that holds flags",
    category = Category.MISC,
    showInList = false
)
class FlagsNode : InvisibleNode() {

    override val requestedId = 171

    @SerializeData
    val flags = mutableMapOf<String, Boolean>()

    @SerializeData
    val numFlags = mutableMapOf<String, Double>()

}
