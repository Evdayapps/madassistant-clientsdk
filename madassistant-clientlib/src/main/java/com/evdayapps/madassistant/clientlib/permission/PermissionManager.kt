package com.evdayapps.madassistant.clientlib.permission

import com.evdayapps.madassistant.common.models.NetworkCallLogModel

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
     * @return null string if no issues, else a string explaining the issue
     * @since 0.0.1
     */
    fun setAuthToken(string: String?): String?

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
     * Should api call logs be encrypted?
     * @return true if read is false
     */
    fun shouldEncryptApiLog(): Boolean
    // endregion Api Logs

    // region Crash Logs
    /**
     * Test if [exception] should be logged to the repository
     */
    fun shouldLogExceptions(throwable: Throwable): Boolean

    /**
     * Should crash logs be encrypted?
     * @return true if read is false
     */
    fun shouldEncryptCrashReports(): Boolean
    // endregion Crash Logs

    // region Analytics
    fun shouldLogAnalytics(destination: String, eventName: String): Boolean

    fun shouldEncryptAnalytics() : Boolean
    // endregion Analytics

    // region Generic Logs
    fun shouldLogGenericLog(type : Int, tag : String, message : String): Boolean

    fun shouldEncryptGenericLogs() : Boolean
    // endregion Generic Logs


}