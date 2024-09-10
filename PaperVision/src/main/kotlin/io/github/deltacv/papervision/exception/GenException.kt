package io.github.deltacv.papervision.exception

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.node.Node

class NodeGenException(val node: Node<*>, override val message: String) : RuntimeException(message)

class AttributeGenException(val attribute: Attribute, override val message: String) : RuntimeException(message)