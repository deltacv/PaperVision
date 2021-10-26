package io.github.deltacv.easyvision.node

import com.github.serivesmejia.eocvsim.util.Log
import io.github.classgraph.ClassGraph

typealias CategorizedNodes = Map<Category, MutableList<Class<out Node<*>>>>

object NodeScanner {

    val TAG = "NodeScanner"

    val ignoredPackages = arrayOf(
        "java",
        "org.opencv",
        "imgui",
        "io.github.classgraph",
        "org.lwjgl"
    )

    @Suppress("UNCHECKED_CAST") //shut
    fun scan(): CategorizedNodes {
        val nodes = mutableMapOf<Category, MutableList<Class<out Node<*>>>>()

        Log.info(TAG, "Scanning for nodes...")

        val classGraph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .rejectPackages(*ignoredPackages)

        val scanResult = classGraph.scan()
        val nodeClasses = scanResult.getClassesWithAnnotation(RegisterNode::class.java.name)

        for(nodeClass in nodeClasses) {
            val clazz = Class.forName(nodeClass.name)

            val regAnnotation = clazz.getDeclaredAnnotation(RegisterNode::class.java)

            if(hasSuperclass(clazz, Node::class.java)) {
                val nodeClazz = clazz as Class<out Node<*>>

                var list = nodes[regAnnotation.category]

                if(list == null) {
                    list = mutableListOf(nodeClazz)
                    nodes[regAnnotation.category] = list
                } else {
                    list.add(nodeClazz)
                }

                Log.info(TAG, "Found node ${nodeClazz.typeName}")
            }
        }

        Log.info(TAG, "Found ${nodeClasses.size} nodes")
        Log.blank()

        return nodes
    }

}

fun hasSuperclass(clazz: Class<*>, superClass: Class<*>): Boolean {
    return try {
        clazz.asSubclass(superClass)
        true
    } catch (ex: ClassCastException) {
        false
    }
}