package com.evdayapps.madassistant.clientlib.transmission

import com.evdayapps.madassistant.common.models.exceptions.ExceptionModel
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

/**
 * Interface for all transmission tasks
 *
 * Default implementation is [TransmitterImpl]
 *
 *
 */
interface Transmitter {

    interface Callback {

        /**
         * Callback for when a new session is started
         *
         * [sessionId] is the timestamp of the new session
         */
        fun onSessionStarted(sessionId: Long)

        /**
         * Callback for when an ongoing session is ended
         */
        fun onSessionEnded(sessionId: Long)
    }

    fun setCallback(callback: Callback)

    /**
     * Start a new session
     * Default implementation ends any ongoing session before starting a new session
     */
    fun startSession()

    /**
     * End an ongoing session
     */
    fun endSession()

    /**
     * Return true if there is an active session
     */
    fun hasActiveSession(): Boolean

    /**
     * Disconnect from the repository.
     *
     * This method should be called instead of [ConnectionManager.disconnect] because it ensures processing of the log queue
     */
    fun disconnect(code: Int, message: String)

    /**
     * Log an api call in the repository
     */
    fun logNetworkCall(data: NetworkCallLogModel)

    /**
     * Log a fatal exception in the repository
     */
    fun logCrashReport(
        throwable: Throwable,
        message: String? = null,
        data: Map<String, Any>? = null
    )

    /**
     * Log an exception (non-fatal) in the repository
     */
    fun logException(
        throwable: Throwable,
        message: String? = null,
        data: Map<String, Any>? = null
    )

    /**
     * Log an exception model (non-fatal) in the repository
     *
     * Required when dealing with exceptions that come from incompatible sources, like Flutter
     */
    fun logException(
        exception: ExceptionModel
    )

    /**
     * Log a generic log in the repository
     */
    fun logGenericLog(type: Int, tag: String, message: String, data: Map<String, Any?>?)

    /**
     * Log an analytics event in the repository
     */
    fun logAnalyticsEvent(destination: String, eventName: String, data: Map<String, Any?>)
}