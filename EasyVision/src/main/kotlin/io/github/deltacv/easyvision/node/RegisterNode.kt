package io.github.deltacv.easyvision.node

@Target(AnnotationTarget.CLASS)
annotation class RegisterNode(
    val name: String,
    val category: Category,
    val description: String = "",
    val showInList: Boolean = true
)
