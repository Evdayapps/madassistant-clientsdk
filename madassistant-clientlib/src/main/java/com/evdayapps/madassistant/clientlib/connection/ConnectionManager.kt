package com.evdayapps.madassistant.clientlib.connection

import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.transmission.TransmissionModel

/**
 * Encapsulates logic to connect with the repository for the client
 */
interface ConnectionManager {

    interface Callback {
        /**
         * The connection manager performs all the actions for the handshake with the repository
         * It then returns the response, if successful, to the client class
         *
         * @param response The handshake response model, null if failed
         * @return true if the authToken is valid, false otherwise
         */
        fun handleHandshakeResponse(response: HandshakeResponseModel?) : Boolean
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
     * Unbind from the currently connected service
     */
    fun unbindService()

    /**
     * Returns whether this client is currently bound to a logging service or not
     * @return true if bound, else false
     */
    fun isBound() : Boolean

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