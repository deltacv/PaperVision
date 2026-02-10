package io.github.deltacv.papervision.codegen.resolve

import io.github.deltacv.papervision.codegen.build.Scope
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.util.loggerFor

class PlaceholderResolver(
    private val importScope: Scope
) {

    private val logger by loggerFor<PlaceholderResolver>()

    fun resolve(
        preprocessed: String,
        placeholders: Collection<Resolvable.Placeholder<*>>
    ): String {
        var resolved = preprocessed

        logger.info("Resolving active placeholders: ${placeholders.size}")
        placeholders.forEach { logger.debug("{} = {}", it.placeholder, it.resolve()) }

        val stack = mutableListOf<Int>() // single stack per pass

        resolved = resolvePass(resolved, placeholders.filter { !it.resolveLast }, stack)
        resolved = resolvePass(resolved, placeholders.filter { it.resolveLast }, stack)

        return resolved
    }

    private fun resolvePass(
        initial: String,
        placeholders: Collection<Resolvable.Placeholder<*>>,
        stack: MutableList<Int>
    ): String {
        var current = initial
        placeholders.forEach { it.resolve() }

        do {
            val (text, changed) = replaceOnce(current, placeholders, stack)
            current = text
        } while (changed)

        return current
    }

    private fun replaceOnce(
        input: String,
        placeholders: Collection<Resolvable.Placeholder<*>>,
        stack: MutableList<Int>
    ): Pair<String, Boolean> {
        val sb = StringBuilder()
        var i = 0
        var changed = false

        while (i < input.length) {
            val start = input.indexOf(Resolvable.RESOLVER_PREFIX, i)
            if (start == -1) {
                sb.append(input, i, input.length)
                break
            }

            val end = input.indexOf(Resolvable.RESOLVER_SUFFIX, start)
            if (end == -1) {
                sb.append(input, i, input.length)
                break
            }

            sb.append(input, i, start)

            val id = input.substring(start + Resolvable.RESOLVER_PREFIX.length, end).toIntOrNull()

            val replacement = id?.let { pid ->
                val placeholder = placeholders.find { it.id == pid } ?: return@let null

                if (pid in stack) {
                    logger.warn("Cycle detected on placeholder ID $pid, stack: ${stack.joinToString(" -> ")}")
                    return@let null
                }

                stack.add(pid)
                try {
                    placeholder.resolve()?.let { resolvedValueToString(it) }
                } finally {
                    stack.removeLast()
                }
            }

            if (replacement != null) {
                sb.append(replacement)
                changed = true
            } else {
                sb.append(input, start, end + 1)
            }

            i = end + 1
        }

        return sb.toString() to changed
    }

    private fun resolvedValueToString(value: Any?): String =
        when (value) {
            is Value -> {
                importScope.importType(value.type)
                value.value ?: ""
            }
            null -> ""
            else -> value.toString()
        }
}
