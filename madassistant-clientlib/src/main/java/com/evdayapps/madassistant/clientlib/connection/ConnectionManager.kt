package com.evdayapps.madassistant.clientlib.connection

import com.evdayapps.madassistant.clientlib.constants.ConnectionState
import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.transmission.TransmissionModel

/**
 * Encapsulates logic to connect with and transmit final payload to the repository
 */
interface ConnectionManager {

    interface Callback {

        fun onStateChanged(state: ConnectionState)

        /**
         * The connection manager performs all the actions for the handshake with the repository
         * It then returns the response, if successful, to the client class
         *
         * @param response The handshake response model, null if failed
         */
        fun validateHandshakeReponse(response: HandshakeResponseModel?)
    }

    /**
     * Sets a callback implementation that handles the handshake response
     */
    fun setCallback(callback: Callback)

    /**
     * Attempt a connection to the repository service
     */
    fun bindToService()

    /**
     * Inform the repository that the client wishes to disconnect for [reason]
     */
    fun disconnect(reason: Int)

    /**
     * Unbind from the currently connected service
     */
    fun unbindService()

    // region Session Management
    /**
     * Begins a new session
     */
    fun startSession() : Long

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