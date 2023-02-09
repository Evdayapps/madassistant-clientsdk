package com.evdayapps.madassistant.clientlib.permission

import android.util.Log
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.clientlib.utils.matches
import com.evdayapps.madassistant.common.cipher.MADAssistantCipher
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel
import com.evdayapps.madassistant.common.models.permissions.MADAssistantPermissions
import org.json.JSONException
import org.json.JSONObject
import java.io.InvalidObjectException
import java.util.regex.Pattern

class PermissionManagerImpl(
    private val cipher: MADAssistantCipher,
    private val logger: Logger? = null,
    private val ignoreDeviceIdCheck: Boolean = false
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
     * This function is used to set the authorization token for the system.
     * @param string The encrypted authorization token
     * @param deviceIdentifier is the identifier for the device.
     * @return an error message if an error is encountered, else null
     */
    override fun setAuthToken(string: String?, deviceIdentifier: String): String? {
        // Check if the input string is null or blank.
        return when {
            string.isNullOrBlank() -> "AuthToken was null or blank"
            else -> try {
                // Decrypt the input string and parse it into a JSON object.
                val deciphered = cipher.decrypt(string)
                val json = JSONObject(deciphered)
                val permissions = MADAssistantPermissions(json)

                // Store the permissions object in a class property.
                this.permissions = permissions
                logger?.i(TAG, "permissions: $permissions")

                // If the device identifier check is not ignored and the device identifier does not match the one in the permissions object, throw an exception.
                if (!ignoreDeviceIdCheck && deviceIdentifier != permissions.deviceId) {
                    throw Exception("Invalid device identifier")
                }

                // Set the permission patterns based on the permissions object.
                setPermissionPatterns(permissions)

                // Return null if there are no errors.
                null
            } catch (ex: JSONException) {
                // Return an error message if the JSON parsing fails.
                "Authtoken decryption failed ${ex.message}"
            } catch (ex: InvalidObjectException) {
                // Return an error message if the auth token is invalid.
                "Invalid authToken"
            } catch (ex: Exception) {
                // Return an error message for any other exceptions.
                "Unknown exception processing authToken: ${ex.message}"
            }
        }
    }


    private fun setPermissionPatterns(permissions: MADAssistantPermissions) {
        val flags = Pattern.MULTILINE and Pattern.CASE_INSENSITIVE
        listOf(
            permissions.networkCalls.filterMethod to ::patternNetworkCallMethod,
            permissions.networkCalls.filterUrl to ::patternNetworkCallUrl,
            permissions.analytics.filterDestination to ::patternAnalyticsDestination,
            permissions.analytics.filterEventName to ::patternAnalyticsName,
            permissions.analytics.filterParamData to ::patternAnalyticsParams,
            permissions.genericLogs.filterTag to ::patternGenericLogsTag,
            permissions.genericLogs.filterMessage to ::patternGenericLogsMessage,
            permissions.exceptions.filterType to ::patternExceptionsType,
            permissions.exceptions.filterMessage to ::patternExceptionsMessage
        ).forEach { (value, property) ->
            if (!value.isNullOrBlank()) {
                property.set(value.toPattern(flags))
            }
        }
    }


    /**
     * The base logging check
     * This checks the following:
     * - Have we got a permission model from the repository?
     * - Is the current time within the timestamps?
     * @since 0.0.1
     *
     * @return True if logging is enabled and timestamps check out, else false
     */
    override fun isLoggingEnabled(): Boolean = permissions?.let { perms ->
        val start = perms.timestampStart ?: 0
        val end = perms.timestampEnd ?: Long.MAX_VALUE
        System.currentTimeMillis() in start until end
    } ?: false

    /**
     * Should api call logs be encrypted?
     * @return true if read is false
     */
    override fun shouldEncryptLogs(): Boolean = permissions?.encrypted == true

    /**
     * Test if [networkCallLogModel] should be logged
     * Checks the api permissions within permission model
     *
     * @param networkCallLogModel The api call to log
     * @return true or false
     */
    override fun shouldLogNetworkCall(networkCallLogModel: NetworkCallLogModel): Boolean {
        if (!isLoggingEnabled() || (permissions?.networkCalls?.enabled != true)) {
            return false
        }

        val chkMethod = patternNetworkCallMethod?.matches(networkCallLogModel.method) ?: true
        val chkUrl = patternNetworkCallUrl?.matches(networkCallLogModel.url) ?: true

        return chkMethod && chkUrl
    }

    /**
     * Some api call headers need to be redacted to avoid leaks of confidential information
     */
    override fun shouldRedactNetworkHeader(header: String): Boolean {
        return permissions?.networkCalls?.redactHeaders?.contains(header) ?: false
    }

    /**
     * Test if [exception] should be logged to the repository
     */
    override fun shouldLogExceptions(throwable: Throwable): Boolean {
        if (!isLoggingEnabled() || (permissions?.exceptions?.enabled != true)) {
            return false
        }

        val chkType = patternExceptionsType?.matches(throwable.javaClass.simpleName) ?: true
        val chkMessage = patternExceptionsMessage?.matches(throwable.message) ?: true

        return chkType && chkMessage
    }

    // region Analytics
    override fun shouldLogAnalytics(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ): Boolean {
        if (!isLoggingEnabled() || permissions?.analytics?.enabled != true) {
            return false
        }

        val chkDest = patternAnalyticsDestination?.matches(destination) ?: true
        val checkName = patternAnalyticsName?.matches(eventName) ?: true
        val checkParams = patternAnalyticsParams?.matches(data.toString()) ?: true

        return chkDest && checkName && checkParams
    }
    // endregion Analytics


    /**
     * Determine if logging should occur for generic logs
     *
     * @param type int representing the log type (e.g. Log.VERBOSE)
     * @param tag String representing the log tag
     * @param message String representing the log message
     *
     * @return Boolean indicating if the log should occur
     */
    override fun shouldLogGenericLog(type: Int, tag: String, message: String): Boolean {
        if (!isLoggingEnabled() || permissions?.genericLogs?.enabled != true) {
            return false
        }

        val subtypeEnabled = when (type) {
            Log.VERBOSE -> permissions?.genericLogs?.logVerbose == true
            Log.DEBUG -> permissions?.genericLogs?.logDebug == true
            Log.WARN -> permissions?.genericLogs?.logWarning == true
            Log.ERROR -> permissions?.genericLogs?.logError == true
            Log.INFO -> permissions?.genericLogs?.logInfo == true
            else -> false
        }
        if (!subtypeEnabled) {
            return false
        }

        val checkTag = patternGenericLogsTag?.matches(tag) ?: true
        val checkMessage = patternGenericLogsMessage?.matches(message) ?: true

        return checkTag && checkMessage
    }
    // endregion Generic Logs
}