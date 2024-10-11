package io.github.deltacv.papervision.serialization.data.adapter

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

object SerializeIgnoreExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes): Boolean {
        // Check if the field or its class has the SerializeIgnore annotation
        return f.declaredClass.isAnnotationPresent(SerializeIgnore::class.java) ||
                f.annotations.any { it.annotationClass.java.isAnnotationPresent(SerializeIgnore::class.java) }
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        // Check if the class has the SerializeIgnore annotation
        return clazz.isAnnotationPresent(SerializeIgnore::class.java)
    }
}