package com.evdayapps.madassistant.clientlib

import android.content.Context
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManagerImpl
import com.evdayapps.madassistant.clientlib.transmission.TransmissionManager
import com.evdayapps.madassistant.clientlib.transmission.TransmissionManagerImpl
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.encryption.MADAssistantCipher
import com.evdayapps.madassistant.common.encryption.MADAssistantCipherImpl
import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.models.NetworkCallLogModel

/**
 * An implementation of [MADAssistantClient]
 * @param applicationContext The application context
 * @param passphrase The encryption passphrase for the client
 * @param
 */
class MADAssistantClientImpl(
    private val applicationContext: Context,
    private val passphrase : String,
    private val logUtils: LogUtils? = null,
    private val repoKey : String = "1B:C0:79:26:82:9E:FB:96:5C:6A:51:6C:96:7C:52:88:42:7E:" +
            "73:8C:05:7D:60:D8:13:9D:C4:3C:18:3B:E3:63",
    private val cipher : MADAssistantCipher = MADAssistantCipherImpl(
        passPhrase = passphrase,
    ),
    private val connectionManager: ConnectionManager = ConnectionManagerImpl(
        applicationContext = applicationContext,
        logUtils = logUtils,
        repoKey = repoKey
    ),
    private val permissionManager: PermissionManager = PermissionManagerImpl(
        cipher = cipher,
        logUtils = logUtils
    ),
    private val transmitter: TransmissionManager = TransmissionManagerImpl(
        cipher = cipher,
        permissionManager = permissionManager,
        connectionManager = connectionManager,
        logUtils = logUtils
    )
) : MADAssistantClient, ConnectionManager.Callback {

    companion object {
        private const val TAG = "MADAssistantClientImpl"
    }

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
                if(error.isNullOrBlank()) {
                    logUtils?.i(TAG, "Handshake successful")
                    startSession()
                } else {
                    logUtils?.d(TAG, "Handshake failed. Cause: $error")
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