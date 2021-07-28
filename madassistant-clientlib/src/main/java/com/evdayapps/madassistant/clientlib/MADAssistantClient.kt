package com.evdayapps.madassistant.clientlib

import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

/**
 * The main interface to interact with the MADAssistant library
 *
 * This interface is just provided for advanced usage
 *
 * For normal usage, use the implementation [MADAssistantClientImpl]
 *
 * This interface, or the implementation, encapsulates all actions to interact with this library
 */
interface MADAssistantClient {

    /**
     * Initialise a normal android Service binding to the repository
     */
    fun connect()

    /**
     * Disconnect from the service
     *
     * The default implementation in [MADAssistantClientImpl] passes the call to a
     * [com.evdayapps.madassistant.clientlib.transmission.TransmissionManager] instance that
     * transmits all logs in the queue and then disconnects from the service
     */
    fun disconnect(message: String?)

    /**
     * Add the client to the tree of Unhandled exception handlers
     *
     * This is required if you wish to log app crashes
     */
    fun logCrashes()

    /**
     * Helper method to start a new session
     *
     * This should ideally not be called explicitly
     * since its called when the handshake is successful
     *
     * Should only be used after a preceding [endSession] call
     */
    fun startSession()

    /**
     * Helper method to end a session
     *
     * Stops all logging and marks a session as ended
     *
     * Need not be used in general usage
     */
    fun endSession()

    /**
     * Log a network api call in the repository
     *
     * @param data An instance of [NetworkCallLogModel].
     *             Recommended usage would be to use the provided adapter, for simplicity
     */
    fun logNetworkCall(data: NetworkCallLogModel)

    /**
     * Log a crash report in the system
     *
     * This is an alternate method to [logCrashes]. You can setup your own crash
     * handler and call this method to log the cause of the crash
     *
     * @param throwable The cause of the crash
     */
    fun logCrashReport(throwable: Throwable)

    /**
     * Log an analytics event in the system
     *
     * Ideal usage: Post sending the payload to the correct destination
     * @param destination The analytics platform that it is being sent to (GA/Firebase/Inhouse)
     * @param eventName The name of the event
     * @param data The payload for the event
     */
    fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    )

    /**
     * Log a normal ... log...
     * @param type One of [android.util.Log.VERBOSE], [android.util.Log.INFO],
     *                    [android.util.Log.WARNING], [android.util.Log.DEBUG],
     *                    [android.util.Log.ERROR]
     * @param tag The tag for the message. Similar to the tag in Log.d(..), Log.i(..) etc.
     * @param message The message to log
     * @param data An optional data map to log
     */
    fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>? = null
    )

    /**
     * Log an exception to the system
     *
     * @param throwable The cause of the exception
     */
    fun logException(throwable: Throwable)

}