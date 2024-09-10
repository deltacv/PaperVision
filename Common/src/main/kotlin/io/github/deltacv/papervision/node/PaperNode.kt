package io.github.deltacv.papervision.node

@Target(AnnotationTarget.CLASS)
annotation class PaperNode(
    val name: String,
    val category: Category,
    val description: String = "",
    val showInList: Boolean = true
)