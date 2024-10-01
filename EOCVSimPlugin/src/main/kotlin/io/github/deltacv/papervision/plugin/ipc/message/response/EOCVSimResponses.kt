package io.github.deltacv.papervision.plugin.ipc.message.response

import io.github.deltacv.papervision.engine.client.response.OkResponse
import io.github.deltacv.papervision.plugin.ipc.message.InputSourceData

class InputSourcesListResponse(
    var sources: Array<InputSourceData>
) : OkResponse()