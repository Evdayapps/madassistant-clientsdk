package com.evdayapps.madassistant.common.handshake.permissions

import com.evdayapps.madassistant.common.handshake.getOr
import org.json.JSONObject
import java.lang.Exception

data class GenericLogsPermissionModel(
    var enabled : Boolean = false,
    var read : Boolean = false,
    var share : Boolean = false,
    var filterTag : String? = null,
    var filterMessage : String? = null,
    var logDebug : Boolean = false,
    var logInfo : Boolean = false,
    var logWarning : Boolean = false,
    var logError : Boolean = false,
    var logVerbose : Boolean = false
) {

    companion object {
        private const val KEY_enabled = "enabled"
        private const val KEY_read = "read"
        private const val KEY_share = "share"
        private const val KEY_filterTag = "filterTag"
        private const val KEY_filterMessage = "filterMessage"
        private const val KEY_logVerbose = "logVerbose"
        private const val KEY_logWarning = "logWarning"
        private const val KEY_logInfo = "logInfo"
        private const val KEY_logError = "logError"
        private const val KEY_logDebug = "logDebug"
    }

    @Throws(Exception::class)
    constructor(json : String) : this() {
        JSONObject(json).apply {
            enabled = getOr(KEY_enabled, false)
            read = getOr(KEY_read, false)
            share = getOr(KEY_share, false)
            filterTag = if(has(KEY_filterTag)) getString(KEY_filterTag) else null
            filterMessage = if(has(KEY_filterMessage)) getString(KEY_filterMessage) else null
            logVerbose = getOr(KEY_logVerbose, false)
            logInfo = getOr(KEY_logInfo, false)
            logWarning = getOr(KEY_logWarning, false)
            logError = getOr(KEY_logError, false)
        }
    }

    fun toJsonObject() : JSONObject {
        return JSONObject().apply {
            put(KEY_enabled, enabled)
            put(KEY_read, read)
            put(KEY_share, share)
            put(KEY_filterTag, filterTag)
            put(KEY_filterMessage, filterMessage)
            put(KEY_logVerbose, logVerbose)
            put(KEY_logInfo, logInfo)
            put(KEY_logWarning, logWarning)
            put(KEY_logError, logError)
            put(KEY_logDebug, logDebug)
        }
    }

}