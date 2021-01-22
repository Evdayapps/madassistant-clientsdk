package com.evdayapps.madassistant.common.handshake

import org.json.JSONObject

class MADAssistantPermissions {

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

    // Timestamps
    var timestampStart: Long?
    var timestampEnd: Long?

    // Device Information
    var deviceId: String?

    var networkCalls: NetworkCallsPermissionModel
    var analytics: AnalyticsPermissionModel
    var crashReports: ExceptionsPermissionModel
    var genericLogs: GenericLogsPermissionModel

    var randomString: String?

    constructor() {
        deviceId = null
        timestampStart = null
        timestampEnd = null
        networkCalls = NetworkCallsPermissionModel()
        analytics = AnalyticsPermissionModel()
        crashReports = ExceptionsPermissionModel()
        genericLogs = GenericLogsPermissionModel()
        randomString = null
    }

    constructor(json: JSONObject) {
        randomString = json.getOr(KEY_randomString, "")

        timestampStart = json.getOr(KEY_timestampStart, null)
        timestampEnd = json.getOr(KEY_timestampEnd, null)

        deviceId = json.getOr(KEY_deviceId, "")

        networkCalls = try {
            json.getOr(KEY_networkCalls, JSONObject()).run { NetworkCallsPermissionModel(this) }
        } catch (ex: Exception) {
            NetworkCallsPermissionModel()
        }

        analytics = try {
            json.getOr(KEY_analytics, JSONObject())
                .run { AnalyticsPermissionModel(this) }
        } catch (ex: Exception) {
            AnalyticsPermissionModel()
        }

        crashReports = try {
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

    constructor(
        deviceId: String,
        timestampStart: Long,
        timestampEnd: Long,
        networkCalls: NetworkCallsPermissionModel,
        analytics: AnalyticsPermissionModel,
        crashReports: ExceptionsPermissionModel,
        genericLogs: GenericLogsPermissionModel
    ) {
        this.deviceId = deviceId
        this.randomString = System.currentTimeMillis().toString()
        this.timestampStart = timestampStart
        this.timestampEnd = timestampEnd
        this.networkCalls = networkCalls
        this.analytics = analytics
        this.crashReports = crashReports
        this.genericLogs = genericLogs
    }

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put(KEY_deviceId, deviceId)

        json.put(KEY_randomString, randomString)

        json.put(KEY_timestampStart, timestampStart)
        json.put(KEY_timestampEnd, timestampEnd)

        json.put(KEY_networkCalls, networkCalls.toJSONObject())
        json.put(KEY_genericLogs, genericLogs.toJSONObject())
        json.put(KEY_exceptions, crashReports.toJSONObject())
        json.put(KEY_analytics, analytics.toJSONObject())
        return json
    }

    override fun toString(): String {
        return "MADAssistantPermissions(" +
                "timestampStart=$timestampStart, " +
                "timestampEnd=$timestampEnd, " +
                "deviceId='$deviceId', " +
                "networkCalls=$networkCalls, " +
                "analytics=$analytics, " +
                "crashReports=$crashReports, " +
                "genericLogs=$genericLogs, " +
                "randomString='$randomString'" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MADAssistantPermissions) return false

        if (timestampStart != other.timestampStart) return false
        if (timestampEnd != other.timestampEnd) return false
        if (deviceId != other.deviceId) return false
        if (networkCalls != other.networkCalls) return false
        if (analytics != other.analytics) return false
        if (crashReports != other.crashReports) return false
        if (genericLogs != other.genericLogs) return false
        if (randomString != other.randomString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestampStart?.hashCode() ?: 0
        result = 31 * result + (timestampEnd?.hashCode() ?: 0)
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + networkCalls.hashCode()
        result = 31 * result + analytics.hashCode()
        result = 31 * result + crashReports.hashCode()
        result = 31 * result + genericLogs.hashCode()
        result = 31 * result + randomString.hashCode()
        return result
    }

    fun clear() {
        this.deviceId = null
        this.randomString = null
        this.timestampStart = null
        this.timestampEnd = null

        this.networkCalls.clear()
        this.analytics.clear()
        this.crashReports.clear()
        this.genericLogs.clear()
    }
}

open class BasePermissionModel {

    companion object {
        private const val KEY_enabled = "enabled"
        private const val KEY_read = "read"
        private const val KEY_share = "share"
        private const val KEY_filterSubject = "filterSubject"
        private const val KEY_filterData = "filterData"
    }

    var enabled: Boolean
    var read: Boolean
    var share: Boolean
    var filterSubject: String?
    var filterData: String?

    constructor() {
        this.enabled = false
        this.read = false
        this.share = false
        this.filterData = null
        this.filterSubject = null
    }

    constructor(
        enabled: Boolean,
        read: Boolean,
        share: Boolean,
        filterSubject: String?,
        filterData: String?
    ) {
        this.enabled = enabled
        this.read = read
        this.share = share
        this.filterSubject = filterSubject
        this.filterData = filterData
    }

    constructor(json: JSONObject) {
        enabled = json.getOr(KEY_enabled, false)
        read = json.getOr(KEY_read, false)
        share = json.getOr(KEY_share, false)
        filterSubject = json.getOr(KEY_filterSubject, null)
        filterData = json.getOr(KEY_filterData, null)
    }

    open fun toJSONObject(): JSONObject {
        return JSONObject().let {
            it.put(KEY_enabled, this.enabled)
            it.put(KEY_read, this.read)
            it.put(KEY_share, this.share)
            it.put(KEY_filterSubject, this.filterSubject)
            it.put(KEY_filterData, this.filterData)

            it
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BasePermissionModel) return false

        if (enabled != other.enabled) return false
        if (read != other.read) return false
        if (share != other.share) return false
        if (filterSubject != other.filterSubject) return false
        if (filterData != other.filterData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + read.hashCode()
        result = 31 * result + share.hashCode()
        result = 31 * result + (filterSubject?.hashCode() ?: 0)
        result = 31 * result + (filterData?.hashCode() ?: 0)
        return result
    }

    open fun clear() {
        this.enabled = false
        this.read = false
        this.share = false
        this.filterSubject = null
        this.filterData = null
    }
}

class NetworkCallsPermissionModel : BasePermissionModel {

    constructor() : super()

    constructor(jsonObject: JSONObject) : super(jsonObject)

    constructor(
        enabled: Boolean = false,
        read: Boolean = false,
        share: Boolean = false,
        filterSubject: String? = null,
        filterData: String? = null
    ) : super(enabled, read, share, filterSubject, filterData)

    override fun toString(): String {
        return "ApiPermissionModel(" +
                "enabled=$enabled, " +
                "read=$read, " +
                "share=$share, " +
                "filterSubject=$filterSubject, " +
                "filterData=$filterData" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NetworkCallsPermissionModel) return false
        if (!super.equals(other)) return false

        if (enabled != other.enabled) return false
        if (read != other.read) return false
        if (share != other.share) return false
        if (filterSubject != other.filterSubject) return false
        if (filterData != other.filterData) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

}

class AnalyticsPermissionModel : BasePermissionModel {

    constructor() : super()

    constructor(jsonObject: JSONObject) : super(jsonObject)

    constructor(
        enabled: Boolean = false,
        read: Boolean = false,
        share: Boolean = false,
        filterSubject: String? = null,
        filterData: String? = null
    ) : super(enabled, read, share, filterSubject, filterData)

    override fun toString(): String {
        return "AnalyticsPermissionModel(" +
                "enabled=$enabled, " +
                "read=$read, " +
                "share=$share, " +
                "filterSubject=$filterSubject, " +
                "filterData=$filterData" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyticsPermissionModel) return false
        if (!super.equals(other)) return false

        if (enabled != other.enabled) return false
        if (read != other.read) return false
        if (share != other.share) return false
        if (filterSubject != other.filterSubject) return false
        if (filterData != other.filterData) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

class ExceptionsPermissionModel : BasePermissionModel {

    companion object {
        private const val KEY_logCrashes = "logCrashes"
    }

    var logCrashes: Boolean = false

    constructor() : super()

    constructor(jsonObject: JSONObject) : super(jsonObject) {
        logCrashes = jsonObject.getOr(KEY_logCrashes, false)
    }

    constructor(
        enabled: Boolean = false,
        read: Boolean = false,
        share: Boolean = false,
        filterSubject: String? = null,
        filterData: String? = null,
        logCrashes: Boolean = false,
    ) : super(enabled, read, share, filterSubject, filterData) {
        this.logCrashes = logCrashes
    }

    override fun toString(): String {
        return "ExceptionsPermissionModel(" +
                "enabled=$enabled, " +
                "read=$read, " +
                "share=$share, " +
                "filterSubject=$filterSubject, " +
                "filterData=$filterData" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExceptionsPermissionModel) return false
        if (!super.equals(other)) return false

        if (enabled != other.enabled) return false
        if (read != other.read) return false
        if (share != other.share) return false
        if (filterSubject != other.filterSubject) return false
        if (filterData != other.filterData) return false
        if (logCrashes != other.logCrashes) return false

        return true
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toJSONObject(): JSONObject {
        return super.toJSONObject().apply {
            put(KEY_logCrashes, logCrashes)
        }
    }
}

class GenericLogsPermissionModel : BasePermissionModel {
    
    companion object {
        private const val KEY_debug = "debug"
        private const val KEY_info = "info"
        private const val KEY_verbose = "verbose"
        private const val KEY_warning = "warning"
        private const val KEY_error = "error"
    }

    var debug: Boolean
    var info: Boolean
    var verbose: Boolean
    var warning: Boolean
    var error: Boolean

    constructor() : super() {
        this.debug = false
        this.info = false
        this.verbose = false
        this.warning = false
        this.error = false
    }

    constructor(
        enabled: Boolean = false,
        read: Boolean = false,
        share: Boolean = false,
        filterSubject: String? = null,
        filterData: String? = null,
        debug: Boolean = false,
        info: Boolean = false,
        verbose: Boolean = false,
        warning: Boolean = false,
        error: Boolean = false
    ) : super(enabled, read, share, filterSubject, filterData) {
        this.debug = debug
        this.info = info
        this.verbose = verbose
        this.warning = warning
        this.error = error
    }

    constructor(jsonObject: JSONObject) : super(jsonObject) {
        debug = jsonObject.getOr(KEY_debug, false)
        verbose = jsonObject.getOr(KEY_verbose, false)
        info = jsonObject.getOr(KEY_info, false)
        warning = jsonObject.getOr(KEY_warning, false)
        error = jsonObject.getOr(KEY_error, false)
    }

    override fun toJSONObject(): JSONObject {
        return super.toJSONObject().also {
            it.put(KEY_verbose, this.verbose)
            it.put(KEY_info, this.info)
            it.put(KEY_debug, this.debug)
            it.put(KEY_warning, this.warning)
            it.put(KEY_error, this.error)
        }
    }

    override fun toString(): String {
        return "GenericLogsPermissionModel(" +
                "enabled=$enabled, " +
                "read=$read, " +
                "share=$share, " +
                "filterSubject=$filterSubject, " +
                "filterData=$filterData, " +
                "debug=$debug, " +
                "info=$info, " +
                "verbose=$verbose, " +
                "warning=$warning, " +
                "error=$error" +
                ")"
    }

    override fun hashCode(): Int {
        var result = debug.hashCode()
        result = 31 * result + info.hashCode()
        result = 31 * result + verbose.hashCode()
        result = 31 * result + warning.hashCode()
        result = 31 * result + error.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenericLogsPermissionModel) return false
        if (!super.equals(other)) return false

        if (debug != other.debug) return false
        if (info != other.info) return false
        if (verbose != other.verbose) return false
        if (warning != other.warning) return false
        if (error != other.error) return false

        return true
    }

    override fun clear() {
        super.clear()
        this.warning = false
        this.error = false
        this.info = false
        this.debug = false
        this.verbose = false
    }
}

fun <T> JSONObject.getOr(key: String, defaultValue: T): T {
    return try {
        if (has(key)) (get(key) as T) else defaultValue
    } catch (ex: Exception) {
        defaultValue
    }
}