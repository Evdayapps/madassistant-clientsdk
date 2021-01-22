package com.evdayapps.madassistant.clientlib.client

import com.evdayapps.madassistant.clientlib.transmission.Transmitter

interface MADAssistantClient : Transmitter {

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

}