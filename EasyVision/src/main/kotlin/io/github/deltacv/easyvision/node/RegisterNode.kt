package io.github.deltacv.easyvision.node

annotation class RegisterNode(
    val name: String,
    val category: Category,
    val description: String = "",
    val showInList: Boolean = true
)
