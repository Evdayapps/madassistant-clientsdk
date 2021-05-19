package com.evdayapps.madassistant.common.handshake

import com.evdayapps.madassistant.common.handshake.permissions.AnalyticsCallsPermissionModel
import com.evdayapps.madassistant.common.handshake.permissions.ExceptionsPermissionModel
import com.evdayapps.madassistant.common.handshake.permissions.GenericLogsPermissionModel
import com.evdayapps.madassistant.common.handshake.permissions.NetworkCallsPermissionModel
import org.json.JSONObject

data class MADAssistantPermissions(
    var timestampStart: Long? = null,
    var timestampEnd: Long? = null,
    var deviceId: String? = null,
    var networkCalls: NetworkCallsPermissionModel = NetworkCallsPermissionModel(),
    var analytics: AnalyticsCallsPermissionModel = AnalyticsCallsPermissionModel(),
    var exceptions: ExceptionsPermissionModel = ExceptionsPermissionModel(),
    var genericLogs: GenericLogsPermissionModel = GenericLogsPermissionModel(),
    var randomString: String? = System.currentTimeMillis().toString()
) {

    companion object {
        private const val KEY_randomString = "randomString"
        private const val KEY_timestampStart = "timestampStart"
        private const val KEY_timestampEnd = "timestampEnd"
        private const val KEY_deviceId = "deviceId"
        private const val KEY_networkCalls = "networkCalls"
        private const val KEY_genericLogs = "genericLogs"
        private const val KEY_analytics = "analytics"
        private const val KEY_exceptions = "exceptions"
    }

    constructor(json: JSONObject) : this() {
        randomString = json.getOr(KEY_randomString, "")

        timestampStart = json.getOr(KEY_timestampStart, null)
        timestampEnd = json.getOr(KEY_timestampEnd, null)

        deviceId = json.getOr(KEY_deviceId, "")

        networkCalls = try {
            json.getOr(KEY_networkCalls, JSONObject())
                .run { NetworkCallsPermissionModel(this) }
        } catch (ex: Exception) {
            NetworkCallsPermissionModel()
        }

        analytics = try {
            json.getOr(KEY_analytics, JSONObject())
                .run { AnalyticsCallsPermissionModel(this) }
        } catch (ex: Exception) {
            AnalyticsCallsPermissionModel()
        }

        exceptions = try {
            json.getOr(KEY_exceptions, JSONObject())
                .run { ExceptionsPermissionModel(this) }
        } catch (ex: Exception) {
            ExceptionsPermissionModel()
        }

        genericLogs = try {
            json.getOr(KEY_genericLogs, JSONObject())
                .run { GenericLogsPermissionModel(this) }
        } catch (ex: Exception) {
            GenericLogsPermissionModel()
        }
    }

    fun toJsonObject(): JSONObject {
        networkCalls.enabled = networkCalls.share || networkCalls.read
        analytics.enabled = analytics.share || analytics.read
        exceptions.enabled = exceptions.share || exceptions.read
        genericLogs.enabled = genericLogs.share || genericLogs.read

        return JSONObject().apply {
            put(KEY_deviceId, deviceId)

            put(KEY_randomString, randomString)

            put(KEY_timestampStart, timestampStart)
            put(KEY_timestampEnd, timestampEnd)

            put(KEY_networkCalls, networkCalls.toJsonObject())
            put(KEY_genericLogs, genericLogs.toJsonObject())
            put(KEY_exceptions, exceptions.toJsonObject())
            put(KEY_analytics, analytics.toJsonObject())
        }
    }

    override fun toString(): String {
        return "MADAssistantPermissions(" +
                "timestampStart=$timestampStart, " +
                "timestampEnd=$timestampEnd, " +
                "deviceId='$deviceId', " +
                "networkCalls=$networkCalls, " +
                "analytics=$analytics, " +
                "crashReports=$exceptions, " +
                "genericLogs=$genericLogs, " +
                "randomString='$randomString'" +
                ")"
    }
}

fun <T> JSONObject.getOr(key: String, defaultValue: T): T {
    return try {
        if (has(key)) (get(key) as T) else defaultValue
    } catch (ex: Exception) {
        defaultValue
    }
}