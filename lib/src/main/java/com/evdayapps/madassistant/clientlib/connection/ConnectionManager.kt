package com.evdayapps.madassistant.clientlib.connection

import com.evdayapps.madassistant.common.models.transmission.TransmissionModel

interface ConnectionManager {

    enum class State {
        None,
        Connecting,
        Connected,
        Disconnecting,
        Disconnected
    }

    interface Callback {

        /**
         * Callback to notify the listener that the state has changed
         */
        fun onConnectionStateChanged(state : ConnectionManager.State)

        fun onDisconnected(code: Int, message: String)

    }

    /**
     * Returns the current state of the connection
     */
    fun getState() : ConnectionManager.State

    /**
     * Set the callback, which listens for state changes
     */
    fun setCallback(callback: Callback)

    /**
     * Attempt a connection to the repository service
     */
    fun connect()

    /**
     * Disconnect from the repository service
     *
     * @param code Reason for disconnection
     * @param message User readable message for disconnection
     * @param hasPendingLogs Whether there are untransmitted logs or not.
     * The default implementation changes the connection state to disconnecting instead of disconnected if this is the case
     */
    fun disconnect(code: Int, message: String, hasPendingLogs: Boolean = false)

    /**
     * Send a log to the repository
     */
    fun transmit(segment: TransmissionModel)

    /**
     * Notify the repository that a new session has begun
     * @param sessionId The id of the session, which is just the timestamp
     */
    fun startSession(sessionId: Long)

    /**
     * Notify the repository that [sessionId] has ended
     */
    fun endSession(sessionId: Long)

    /**
     * Check if a connection has been established with the repository service
     */
    fun isConnected(): Boolean

    /**
     * Check if a connection is being created with the repository service
     */
    fun isConnecting(): Boolean

    /**
     * Check if the connection state is [State.Connecting] or [State.Connected]
     */
    fun isConnectedOrConnecting(): Boolean

    /**
     * Check if the current stats is [State.Disconnecting]
     */
    fun isDisconnecting() : Boolean


}