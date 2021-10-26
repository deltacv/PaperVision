package io.github.deltacv.easyvision.exception

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.Node

class NodeGenException(val node: Node<*>, override val message: String) : RuntimeException(message)

class AttributeGenException(val attribute: Attribute, override val message: String) : RuntimeException(message)