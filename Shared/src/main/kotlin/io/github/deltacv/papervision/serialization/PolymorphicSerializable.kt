package io.github.deltacv.papervision.serialization

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PolymorphicSerializable(val baseClass: KClass<*>)