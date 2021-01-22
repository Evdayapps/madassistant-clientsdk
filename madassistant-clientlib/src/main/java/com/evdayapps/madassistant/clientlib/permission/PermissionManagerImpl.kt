package com.evdayapps.madassistant.clientlib.permission

import android.util.Log
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.encryption.MADAssistantCipher
import com.evdayapps.madassistant.common.handshake.MADAssistantPermissions
import com.evdayapps.madassistant.common.models.NetworkCallLogModel
import org.json.JSONException
import org.json.JSONObject
import java.io.InvalidObjectException
import java.util.regex.Pattern

class PermissionManagerImpl(
    private val cipher: MADAssistantCipher,
    private val logUtils: LogUtils? = null
) : PermissionManager {

    private val TAG = "PermissionManagerImpl"

    private var permissions: MADAssistantPermissions? = null

    private var patternNetworkCallSubject: Pattern? = null
    private var patternNetworkCallData: Pattern? = null
    private var patternAnalyticsDestinationEventname: Pattern? = null
    private var patternAnalyticsParams: Pattern? = null
    private var patternGenericLogsTag: Pattern? = null
    private var patternGenericLogsMessage: Pattern? = null
    private var patternExceptionsType: Pattern? = null
    private var patternExceptionsMessage: Pattern? = null

    /**
     * Sets the permission string that's received from the repository
     * This method should:
     * - Decipher the string
     * - Convert to a JSONObject, with JSONException test
     * - Convert to a [com.evdayapps.madassistant.common.handshake.MADAssistantPermissions] model
     * - Store the permission model for later checks
     *
     * @param string The string received from the repository
     * @return null string if no issues, else a string explaining the issue
     * @since 0.0.1
     */
    override fun setAuthToken(string: String?): String? {
        if (string.isNullOrBlank()) {
            return "AuthToken was null or blank"
        } else {
            try {
                val deciphered = cipher.decrypt(string)
                val json = JSONObject(deciphered)
                val permissions = MADAssistantPermissions(json)
                logUtils?.i(
                    TAG,
                    "permissions: $permissions"
                )
                this.permissions = permissions

                // Network Calls Regex
                val flags = Pattern.MULTILINE and Pattern.CASE_INSENSITIVE
                patternNetworkCallSubject = permissions.networkCalls.filterSubject?.toPattern(flags)
                patternNetworkCallData = permissions.networkCalls.filterData?.toPattern(flags)
                patternAnalyticsDestinationEventname = permissions.analytics.filterSubject?.toPattern(flags)
                patternAnalyticsParams = permissions.analytics.filterData?.toPattern(flags)
                patternGenericLogsTag = permissions.genericLogs.filterSubject?.toPattern(flags)
                patternGenericLogsMessage = permissions.genericLogs.filterData?.toPattern(flags)
                patternExceptionsType = permissions.exceptions.filterSubject?.toPattern(flags)
                patternExceptionsMessage = permissions.exceptions.filterData?.toPattern(flags)

            } catch (ex: JSONException) {
                return "Authtoken decryption failed ${ex.message}"
            } catch (ex: InvalidObjectException) {
                return "Invalid authToken"
            } catch (ex: Exception) {
                return "Unknown exception processing authToken: ${ex.message}"
            }
        }

        return null
    }

    /**
     * The base logging check
     * This checks the following:
     * - Have we got a permission model from the repository?
     * - Is the current time within the timestamps?
     * @since 0.0.1
     *
     * @return True if logging is enabled, else false
     */
    override fun isLoggingEnabled(): Boolean {
        return when {
            permissions == null -> false
            System.currentTimeMillis() < (permissions?.timestampStart ?: 0) -> false
            System.currentTimeMillis() > (permissions?.timestampEnd ?: Long.MAX_VALUE) -> false
            else -> true
        }
    }

    /**
     * Test if [networkCallLogModel] should be logged
     * Checks the api permissions within permission model
     *
     * @param networkCallLogModel The api call to log
     * @return true or false
     */
    override fun shouldLogNetworkCall(networkCallLogModel: NetworkCallLogModel): Boolean {
        if (!isLoggingEnabled()) {
            return false
        }

        if (permissions?.networkCalls?.enabled != true) {
            return false
        }

        if (patternNetworkCallSubject != null) {
            if (patternNetworkCallSubject?.matcher(networkCallLogModel.url ?: "")
                    ?.matches() != true
            ) {
                return false
            }
        }

        if (patternNetworkCallData != null) {
            if (patternNetworkCallData?.matcher(networkCallLogModel.requestBody ?: "")
                    ?.matches() != true
            ) {
                return false
            }
        }

        return true
    }

    /**
     * Should api call logs be encrypted?
     * @return true if read is false
     */
    override fun shouldEncryptApiLog(): Boolean = permissions?.networkCalls?.read != true

    /**
     * Test if [exception] should be logged to the repository
     */
    override fun shouldLogExceptions(throwable: Throwable): Boolean {
        if (!isLoggingEnabled()) {
            return false
        }

        if (permissions?.exceptions?.enabled != true) {
            return false
        }

        if (patternExceptionsType != null) {
            if (patternExceptionsType?.matcher(throwable.javaClass.simpleName)?.matches() != true) {
                return false
            }
        }

        if (patternExceptionsMessage != null) {
            if (patternExceptionsMessage?.matcher(throwable.message)?.matches() != true) {
                return false
            }
        }

        return true
    }

    /**
     * Should crash logs be encrypted?
     * @return true if read is false
     */
    override fun shouldEncryptCrashReports(): Boolean = false

    // region Analytics
    override fun shouldLogAnalytics(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ): Boolean {
        if (!isLoggingEnabled()) {
            return false
        }

        if (permissions?.analytics?.enabled != true) {
            return false
        }

        if (patternAnalyticsDestinationEventname != null) {
            if (patternAnalyticsDestinationEventname?.matcher("$destination::$eventName")
                    ?.matches() != true
            ) {
                return false
            }
        }

        if (patternAnalyticsParams != null) {
            if (patternAnalyticsParams?.matcher(eventName)?.matches() != true) {
                return false
            }
        }

        return true
    }

    override fun shouldEncryptAnalytics(): Boolean = permissions?.analytics?.read != true
    // endregion Analytics

    // region Generic Logs
    override fun shouldLogGenericLog(type: Int, tag: String, message: String): Boolean {
        if (!isLoggingEnabled()) {
            return false
        }

        if (permissions?.genericLogs?.enabled != true) {
            return false
        }

        val subtype = when (type) {
            Log.VERBOSE -> permissions?.genericLogs?.verbose == true
            Log.DEBUG -> permissions?.genericLogs?.debug == true
            Log.WARN -> permissions?.genericLogs?.warning == true
            Log.ERROR -> permissions?.genericLogs?.error == true
            Log.INFO -> permissions?.genericLogs?.info == true
            else -> false
        }
        if(!subtype) {
            return false
        }

        if (patternGenericLogsTag != null) {
            if (patternGenericLogsTag?.matcher(tag)?.matches() != true) {
                return false
            }
        }

        if (patternGenericLogsMessage != null) {
            if (patternGenericLogsMessage?.matcher(message)?.matches() != true) {
                return false
            }
        }

        return true
    }

    override fun shouldEncryptGenericLogs(): Boolean = permissions?.genericLogs?.read != true
    // endregion Generic Logs
}