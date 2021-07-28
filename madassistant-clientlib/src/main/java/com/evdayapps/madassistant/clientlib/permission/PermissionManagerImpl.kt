package com.evdayapps.madassistant.clientlib.permission

import android.util.Log
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.cipher.MADAssistantCipher
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel
import com.evdayapps.madassistant.common.models.permissions.MADAssistantPermissions
import org.json.JSONException
import org.json.JSONObject
import java.io.InvalidObjectException
import java.util.regex.Pattern

class PermissionManagerImpl(
    private val cipher: MADAssistantCipher,
    private val logUtils: LogUtils? = null,
    private val ignoreDeviceIdCheck : Boolean = false
) : PermissionManager {

    private val TAG = "PermissionManagerImpl"

    private var permissions: MADAssistantPermissions? = null

    private var patternNetworkCallMethod: Pattern? = null
    private var patternNetworkCallUrl: Pattern? = null
    private var patternAnalyticsDestination: Pattern? = null
    private var patternAnalyticsName: Pattern? = null
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
    override fun setAuthToken(string: String?, deviceIdentifier: String): String? {
        if (string.isNullOrBlank()) {
            return "AuthToken was null or blank"
        } else {
            try {
                val deciphered = cipher.decrypt(string)
                val json = JSONObject(deciphered)

                val permissions = MADAssistantPermissions(json)
                this.permissions = permissions
                logUtils?.i(
                    TAG,
                    "permissions: $permissions"
                )

                if(!ignoreDeviceIdCheck) {
                    if(!deviceIdentifier.equals(permissions.deviceId, ignoreCase = true)) {
                        throw Exception("Invalid device identifier")
                    }
                }

                // Network Calls Regex
                val flags = Pattern.MULTILINE and Pattern.CASE_INSENSITIVE
                patternNetworkCallMethod =
                    permissions.networkCalls.filterMethod
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)
                patternNetworkCallUrl =
                    permissions.networkCalls.filterUrl
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)

                patternAnalyticsDestination =
                    permissions.analytics.filterDestination
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)
                patternAnalyticsName =
                    permissions.analytics.filterEventName
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)

                patternAnalyticsParams =
                    permissions.analytics.filterParamData
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)
                patternGenericLogsTag =
                    permissions.genericLogs.filterTag
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)
                patternGenericLogsMessage =
                    permissions.genericLogs.filterMessage
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)

                patternExceptionsType =
                    permissions.exceptions.filterType
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)
                patternExceptionsMessage =
                    permissions.exceptions.filterMessage
                        ?.takeIf { it.isNotBlank() }
                        ?.toPattern(flags)

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

        if (patternNetworkCallMethod != null) {
            if (patternNetworkCallMethod?.matcher(networkCallLogModel.method ?: "")
                    ?.matches() != true
            ) {
                return false
            }
        }

        if (patternNetworkCallUrl != null) {
            if (patternNetworkCallUrl?.matcher(networkCallLogModel.url ?: "")
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

        if (patternAnalyticsDestination != null) {
            if (patternAnalyticsDestination?.matcher(destination)?.matches() != true
            ) {
                return false
            }
        }

        patternAnalyticsName?.let {
            if (patternAnalyticsName?.matcher(destination)?.matches() != true) {
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
            Log.VERBOSE -> permissions?.genericLogs?.logVerbose == true
            Log.DEBUG -> permissions?.genericLogs?.logDebug == true
            Log.WARN -> permissions?.genericLogs?.logWarning == true
            Log.ERROR -> permissions?.genericLogs?.logError == true
            Log.INFO -> permissions?.genericLogs?.logInfo == true
            else -> false
        }
        if (!subtype) {
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