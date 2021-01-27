package com.evdayapps.madassistant.clientlib

import android.content.Context
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.transmission.TransmissionManager
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.models.NetworkCallLogModel

/**
 * An implementation of [MADAssistantClient]
 * @param applicationContext The application context
 * @param
 */
class MADAssistantClientImpl(
    private val applicationContext: Context,
    private val connectionManager: ConnectionManager,
    private val permissionManager: PermissionManager,
    private val transmitter: TransmissionManager,
    private val logUtils: LogUtils? = null
) : MADAssistantClient, ConnectionManager.Callback {

    private val TAG = "MADAssistantClientImpl"

    private var exceptionHandler: Thread.UncaughtExceptionHandler? = null

    init {
        connectionManager.setCallback(this)
    }

    override fun connectExceptionHandler() {
        if (exceptionHandler == null) {
            val def = Thread.getDefaultUncaughtExceptionHandler()
            exceptionHandler = Thread.UncaughtExceptionHandler { p0, p1 ->
                logCrashReport(p1)
                def?.uncaughtException(p0, p1)
            }
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
        }
    }

    // region Connection
    override fun bindToService() = connectionManager.bindToService()

    override fun unbindService() = connectionManager.unbindService()

    override fun handleHandshakeResponse(response: HandshakeResponseModel?): Boolean {
        when {
            response?.successful == true -> {
                val error = permissionManager.setAuthToken(response.authToken)
                if (error?.isNotBlank() == true) {
                    logUtils?.d(TAG, "Handshake failed. Cause: $error")
                } else {
                    logUtils?.i(TAG, "Handshake successful")
                    startSession()
                }
            }
            response?.errorMessage?.isNotBlank() == true -> {
                logUtils?.d(TAG, "Handshake Failed. Cause: ${response.errorMessage}")
            }
            else -> {
                logUtils?.d(TAG, "Handshake failed for an unknown reason")
            }
        }

        if (!permissionManager.isLoggingEnabled()) {
            connectionManager.unbindService()
        }

        return isAssistantEnabled()
    }
    // endregion Connection

    // region Session
    override fun startSession() {
        val sessionId = connectionManager.startSession()
        transmitter.startSession(sessionId)
    }

    override fun endSession() {
        transmitter.endSession()
        connectionManager.endSession()
    }
    // endregion Session

    // region Logging
    override fun isAssistantEnabled(): Boolean = permissionManager.isLoggingEnabled()

    override fun logNetworkCall(data: NetworkCallLogModel) = transmitter.logNetworkCall(data)

    override fun logCrashReport(throwable: Throwable) = transmitter.logCrashReport(throwable)

    override fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) = transmitter.logAnalyticsEvent(destination, eventName, data)

    override fun logGenericLog(type: Int, tag: String, message: String) =
        transmitter.logGenericLog(type, tag, message)

    override fun logException(throwable: Throwable) =
        transmitter.logException(throwable)
    // endregion Logging

}