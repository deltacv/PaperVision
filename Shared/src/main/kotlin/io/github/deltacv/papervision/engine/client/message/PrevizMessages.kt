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

package io.github.deltacv.papervision.engine.client.message

import io.github.deltacv.papervision.engine.message.PaperVisionEngineMessageBase

class PrevizPingMessage(
    var previzName: String
) : PaperVisionEngineMessageBase()

class PrevizStartMessage(
    var previzName: String,
    var sourceCode: String,
    var streamWidth: Int,
    var streamHeight: Int
) : PaperVisionEngineMessageBase()

class PrevizSourceCodeMessage(
    var previzName: String,
    var sourceCode: String
) : PaperVisionEngineMessageBase()

class PrevizStopMessage(
    var previzName: String
) : PaperVisionEngineMessageBase()

class PrevizAskNameMessage : PaperVisionEngineMessageBase()

class TunerChangeValueMessage(
    var label: String,
    var value: Any
) : PaperVisionEngineMessageBase()

class TunerChangeValuesMessage(
    var label: String,
    var values: Array<*>
) : PaperVisionEngineMessageBase()
