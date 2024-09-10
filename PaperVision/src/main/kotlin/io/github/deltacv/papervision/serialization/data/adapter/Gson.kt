package io.github.deltacv.papervision.serialization.data.adapter

import com.google.gson.GsonBuilder
import io.github.deltacv.papervision.serialization.data.DataSerializable

val gson = GsonBuilder()
    .registerTypeHierarchyAdapter(DataSerializable::class.java, DataSerializableAdapter)
    .addSerializationExclusionStrategy(SerializeIgnoreExclusionStrategy)
    .create()