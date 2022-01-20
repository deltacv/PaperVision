package io.github.deltacv.easyvision.serialization.data.adapter

import com.google.gson.GsonBuilder
import io.github.deltacv.easyvision.serialization.data.DataSerializable
import io.github.deltacv.easyvision.serialization.data.adapter.DataSerializableAdapter

val gson = GsonBuilder()
    .registerTypeHierarchyAdapter(DataSerializable::class.java, DataSerializableAdapter)
    .addSerializationExclusionStrategy(SerializeIgnoreExclusionStrategy)
    .create()