package com.evdayapps.madassistant.clientlib.permission

import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

/**
 * Encapsulation of all permission logic for dealing with the repository
 */
interface PermissionManager {

    /**
     * Sets the permission string that's received from the repository
     * This method should:
     * - Decipher the string
     * - Convert to a JSONObject, with JSONException test
     * - Convert to a [com.evdayapps.madassistant.common.handshake.MADAssistantPermissions] model
     * - Store the permission model for later checks
     *
     * @param string The string received from the repository
     * @param deviceIdentifier The unique identifier for the device, extracted from the handshake
     *                         response
     * @return null string if no issues, else a string explaining the issue
     * @since 0.0.1
     */
    fun setAuthToken(string: String?, deviceIdentifier: String): String?

    /**
     * The base logging check
     * This checks the following:
     * - Have we got a permission model from the repository?
     * - Is the current time within the timestamps?
     * @since 0.0.1
     *
     * @return True if logging is enabled, else false
     */
    fun isLoggingEnabled(): Boolean

    /**
     * Should api call logs be encrypted?
     * @return true if read is false
     */
    fun shouldEncryptLogs(): Boolean

    // region Api Logs
    /**
     * Test if [networkCallLogModel] should be logged
     * Checks the api permissions within permission model
     *
     * @param networkCallLogModel The api call to log
     * @return true or false
     */
    fun shouldLogNetworkCall(networkCallLogModel: NetworkCallLogModel): Boolean

    /**
     * Some api call headers need to be redacted to avoid leaks of confidential information
     */
    fun shouldRedactNetworkHeader(header : String) : Boolean
    // endregion Api Logs

    // region Crash Logs
    /**
     * Test if [throwable] should be logged to the repository
     */
    fun shouldLogExceptions(throwable: Throwable): Boolean
    // endregion Crash Logs

    // region Analytics
    /**
     * Tests if an analytics event should be logged by the system
     * Checks:
     * - Common analytics enabled flag
     * - Destination name
     * - Event Name
     * - data map
     */
    fun shouldLogAnalytics(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ): Boolean
    // endregion Analytics

    // region Generic Logs
    fun shouldLogGenericLog(type : Int, tag : String, message : String): Boolean
    // endregion Generic Logs


}