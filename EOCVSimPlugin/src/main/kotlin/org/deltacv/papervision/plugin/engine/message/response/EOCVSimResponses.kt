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

package org.deltacv.papervision.plugin.engine.message.response

import org.deltacv.papervision.engine.client.message.PaperVisionEngineMessage
import org.deltacv.papervision.engine.client.response.OkResponse
import org.deltacv.papervision.engine.client.response.PaperVisionEngineMessageResponse
import org.deltacv.papervision.plugin.engine.message.IpcInputSourceData
import org.deltacv.papervision.serialization.PolymorphicSerializable
import kotlinx.serialization.Serializable

@Serializable
@PolymorphicSerializable(PaperVisionEngineMessageResponse::class)
class InputSourcesListResponse(
    val sources: Array<IpcInputSourceData>
) : OkResponse()



