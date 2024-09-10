package io.github.deltacv.papervision.serialization.data.adapter

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import io.github.deltacv.papervision.serialization.data.SerializeIgnore

object SerializeIgnoreExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipField(f: FieldAttributes?) =
        f?.declaredClass?.isAnnotationPresent(SerializeIgnore::class.java) ?: false

    override fun shouldSkipClass(clazz: Class<*>?) =
        clazz?.isAnnotationPresent(SerializeIgnore::class.java) ?: false
}