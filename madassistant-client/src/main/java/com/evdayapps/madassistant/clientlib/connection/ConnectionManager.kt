package com.evdayapps.madassistant.clientlib.connection

import com.evdayapps.madassistant.common.models.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.models.transmission.TransmissionModel

/**
 * Encapsulates logic to connect with and transmit final payload to the repository
 */
interface ConnectionManager {

    interface Callback {

        /**
         * The connection manager performs all the actions for the handshake with the repository
         * It then returns the response, if successful, to the client class
         *
         * @param response The handshake response model, null if failed
         */
        fun validateHandshakeReponse(response: HandshakeResponseModel?)
    }

    /**
     * Return the current state of the connection
     */
    var currentState : ConnectionState

    /**
     * Sets a callback implementation that handles the handshake response
     */
    fun setCallback(callback: Callback)

    /**
     * Attempt a connection to the repository service
     */
    fun bindToService()

    /**
     * Inform the repository that the client wishes to disconnect for [message]
     */
    fun disconnect(code: Int, message: String?)

    /**
     * Unbind from the currently connected service
     */
    fun unbindService()

    // region Session Management
    /**
     * Begins a new session
     */
    fun startSession(): Long

    /**
     * Ends the current session
     */
    fun endSession()
    // endregion Session Management

    // region Logging
    /**
     * Send a log to the repository
     */
    fun transmit(transmission: TransmissionModel)
    // endregion Logging

}