package com.evdayapps.madassistant.common.handshake.permissions

import org.json.JSONObject

data class ExceptionsPermissionModel(
    var enabled: Boolean = false,
    var read: Boolean = false,
    var share: Boolean = false,
    var filterType: String? = null,
    var filterMessage: String? = null,

    var crashesEnabled: Boolean = false,
) {

    companion object {
        private const val KEY_enabled = "enabled"
        private const val KEY_read = "read"
        private const val KEY_share = "share"
        private const val KEY_filterType = "filterType"
        private const val KEY_filterMessage = "filterMessage"
        private const val KEY_logCrashes = "logCrashes"
    }

    @Throws(Exception::class)
    constructor(json: String) : this() {
        JSONObject(json).run {
            enabled = optBoolean(KEY_enabled, false)
            read = optBoolean(KEY_read, false)
            share = optBoolean(KEY_share, false)
            crashesEnabled = optBoolean(KEY_logCrashes, false)
            filterType = if (has(KEY_filterType)) getString(KEY_filterType) else null
            filterMessage = if (has(KEY_filterMessage)) getString(KEY_filterMessage) else null
        }
    }

    fun toJsonObject() : JSONObject {
        return JSONObject().apply {
            put(KEY_enabled, enabled)
            put(KEY_read, read)
            put(KEY_share, share)
            put(KEY_filterType, filterType)
            put(KEY_filterMessage, filterMessage)
        }
    }

}