package com.evdayapps.madassistant.common.handshake.permissions

import org.json.JSONObject

data class AnalyticsCallsPermissionModel(
    var enabled: Boolean = false,
    var read: Boolean = false,
    var share: Boolean = false,
    var filterDestination: String? = null,
    var filterEventName: String? = null,
    var filterParamData: String? = null,
) {

    companion object {
        private const val KEY_enabled = "enabled"
        private const val KEY_read = "read"
        private const val KEY_share = "share"
        private const val KEY_filterDestination = "filterDestination"
        private const val KEY_filterEventName = "filterEventName"
        private const val KEY_filterParamData = "filterParamData"
    }

    @Throws(Exception::class)
    constructor(json: JSONObject) : this() {
        json.run {
            enabled = optBoolean(KEY_enabled, false)
            read = optBoolean(KEY_read, false)
            share = optBoolean(KEY_share, false)
            filterDestination = if (has(KEY_filterDestination)) getString(
                KEY_filterDestination
            ) else null
            filterEventName =
                if (has(KEY_filterEventName)) getString(KEY_filterEventName) else null
            filterParamData = if (has(KEY_filterParamData)) getString(KEY_filterParamData) else null
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_enabled, enabled)
            put(KEY_read, read)
            put(KEY_share, share)
            put(KEY_filterDestination, filterDestination)
            put(KEY_filterEventName, filterEventName)
            put(KEY_filterParamData, filterParamData)
        }
    }

}
