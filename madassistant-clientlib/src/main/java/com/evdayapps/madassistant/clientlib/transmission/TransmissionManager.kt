package com.evdayapps.madassistant.clientlib.transmission

interface TransmissionManager : Transmitter {

    /**
     * Initiates a new session
     * Retrieves the current timestamp as the session id
     */
    fun startSession(sessionId: Long)

    /**
     * Ends a session
     */
    fun endSession()

}