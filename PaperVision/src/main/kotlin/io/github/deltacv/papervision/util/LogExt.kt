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

package io.github.deltacv.papervision.util

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

internal fun Any.loggerFor(clazz: KClass<*>) = lazy { LoggerFactory.getLogger(clazz.java) }
internal fun Any.loggerForThis() = lazy { LoggerFactory.getLogger(this::class.java) }

internal fun Any.loggerOf(name: String) = lazy { LoggerFactory.getLogger(name) }