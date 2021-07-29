package com.evdayapps.madassistant.clientlib.transmission

import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

interface TransmissionManager {

    /**
     * Initiates a new session
     * Retrieves the current timestamp as the session id
     */
    fun startSession(sessionId: Long)

    /**
     * Ends a session
     */
    fun endSession()

    /**
     * Notify the MADAssistant repository service that the client is disconnecting
     * This would be used by the repository to trigger the appropriate notification to open
     * settings
     */
    fun disconnect(code: Int, message: String?)

    fun logNetworkCall(data: NetworkCallLogModel)

    fun logCrashReport(throwable: Throwable)

    fun logAnalyticsEvent(destination: String, eventName: String, data: Map<String, Any?>)

    fun logGenericLog(type: Int, tag: String, message: String, data : Map<String, Any?>?)

    fun logException(throwable: Throwable)

}