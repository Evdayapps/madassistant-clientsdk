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

    fun logNetworkCall(data: NetworkCallLogModel)

    fun logCrashReport(throwable: Throwable)

    fun logAnalyticsEvent(destination: String, eventName: String, data: Map<String, Any?>)

    fun logGenericLog(type: Int, tag: String, message: String, data : Map<String, Any?>?)

    fun logException(throwable: Throwable)

}