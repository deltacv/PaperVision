package io.github.deltacv.papervision.engine.client.response

import com.google.gson.JsonElement

class BooleanResponse(
    var value: Boolean
) : OkResponse()

class StringResponse(
    var value: String
) : OkResponse()

class JsonElementResponse(
    var value: JsonElement
) : OkResponse()