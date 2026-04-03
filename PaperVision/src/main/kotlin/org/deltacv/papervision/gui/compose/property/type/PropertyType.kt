package org.deltacv.papervision.gui.compose.property.type

import org.deltacv.papervision.gui.compose.property.ConstantProperty

interface PropertyType

fun <T: PropertyType> T.asProperty() = ConstantProperty(this)
