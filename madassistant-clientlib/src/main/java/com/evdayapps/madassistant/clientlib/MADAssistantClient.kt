package com.evdayapps.madassistant.clientlib

import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

interface MADAssistantClient {

    fun bindToService()

    fun unbindService()

    fun connectExceptionHandler()

    fun isAssistantEnabled(): Boolean

    /**
     * Helper method to start a new session
     * This should ideally not be called since its called when the handshake is successful
     * Should only be used after a preceeding [endSession] call
     */
    fun startSession()

    /**
     * Helper method to end a session
     * Stops all logging and marks a session as ended
     * Need not be used in general usage
     */
    fun endSession()

    fun logNetworkCall(data: NetworkCallLogModel)

    fun logCrashReport(throwable: Throwable)

    fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    )

    fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>? = null
    )

    fun logException(throwable: Throwable)

}