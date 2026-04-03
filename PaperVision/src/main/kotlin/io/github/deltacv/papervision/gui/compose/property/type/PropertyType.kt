package io.github.deltacv.papervision.gui.compose.property.type

import io.github.deltacv.papervision.gui.compose.property.ConstantProperty

interface PropertyType

fun <T: PropertyType> T.asProperty() = ConstantProperty(this)