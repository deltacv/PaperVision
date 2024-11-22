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