package io.github.deltacv.papervision.node

import io.github.classgraph.ClassGraph
import io.github.deltacv.papervision.gui.CategorizedNodes
import io.github.deltacv.papervision.util.loggerForThis
import kotlinx.coroutines.*

object NodeScanner {

    val logger by loggerForThis()

    val ignoredPackages = arrayOf(
        "java",
        "org.opencv",
        "imgui",
        "io.github.classgraph",
        "org.lwjgl"
    )

    var result: CategorizedNodes? = null
        private set

    @Suppress("UNCHECKED_CAST") //shut
    fun scan(useCache: Boolean = true): CategorizedNodes {
        if(result != null && useCache) return result!!

        val nodes = mutableMapOf<Category, MutableList<Class<out Node<*>>>>()

        logger.info("Scanning for nodes...")

        val classGraph = ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .overrideClassLoaders(NodeScanner::class.java.classLoader)
            .rejectPackages(*ignoredPackages)

        val scanResult = classGraph.scan()
        val nodeClasses = scanResult.getClassesWithAnnotation(PaperNode::class.java.name)

        for(nodeClass in nodeClasses) {
            val clazz = Class.forName(nodeClass.name)

            val regAnnotation = clazz.getDeclaredAnnotation(PaperNode::class.java)

            if(hasSuperclass(clazz, Node::class.java)) {
                val nodeClazz = clazz as Class<out Node<*>>

                var list = nodes[regAnnotation.category]

                if(list == null) {
                    list = mutableListOf(nodeClazz)
                    nodes[regAnnotation.category] = list
                } else {
                    list.add(nodeClazz)
                }

                logger.trace("Found node ${nodeClazz.typeName}")
            }
        }

        logger.info("Found ${nodeClasses.size} nodes")

        result = nodes
        return nodes
    }

    private var job: Job? = null

    val hasFinishedAsyncScan get() = job == null && result != null

    @OptIn(DelicateCoroutinesApi::class)
    fun startAsyncScan() {
        job = GlobalScope.launch(Dispatchers.IO) {
            scan()
            job = null
        }
    }

    fun waitAsyncScan(): CategorizedNodes {
        if(job != null) {
            println("${Thread.currentThread().name} is waiting for async scan")

            runBlocking {
                job!!.join()
            }
        }

        return result!!
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