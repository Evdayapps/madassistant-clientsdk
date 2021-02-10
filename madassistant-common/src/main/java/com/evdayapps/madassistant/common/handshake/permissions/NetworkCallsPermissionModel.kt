package com.evdayapps.madassistant.common.handshake.permissions

import org.json.JSONException
import org.json.JSONObject

data class NetworkCallsPermissionModel(
    var enabled: Boolean = false,
    var read: Boolean = false,
    var share: Boolean = false,
    var filterMethod: String? = null,
    var filterUrl: String? = null
) {

    companion object {
        private const val KEY_enabled = "enabled"
        private const val KEY_read = "read"
        private const val KEY_share = "share"
        private const val KEY_filterMethod = "filterMethod"
        private const val KEY_filterUrl = "filterUrl"
    }

    @Throws(JSONException::class)
    constructor(json: JSONObject) : this() {
        json.run {
            enabled = optBoolean(KEY_enabled, false)
            read = optBoolean(KEY_read, false)
            share = optBoolean(KEY_share, false)
            filterMethod = if (has(KEY_filterMethod)) getString(KEY_filterMethod) else null
            filterUrl = if (has(KEY_filterUrl)) getString(KEY_filterUrl) else null
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_enabled, enabled)
            put(KEY_read, read)
            put(KEY_share, share)
            put(KEY_filterMethod, filterMethod)
            put(KEY_filterUrl, filterUrl)
        }
    }
}