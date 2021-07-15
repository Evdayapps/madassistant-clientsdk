package com.evdayapps.madassistant.clientlib

import android.content.Context
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.constants.ConnectionState
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManagerImpl
import com.evdayapps.madassistant.clientlib.transmission.TransmissionManager
import com.evdayapps.madassistant.clientlib.transmission.TransmissionManagerImpl
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.encryption.MADAssistantCipher
import com.evdayapps.madassistant.common.encryption.MADAssistantCipherImpl
import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

/**
 * An implementation of [MADAssistantClient]
 * @param applicationContext [required] The application context
 *
 * @param passphrase [required] The encryption passphrase for the client
 *
 * @param logUtils [optional] Instance of [LogUtils]
 *
 * @param ignoreDeviceIdCheck [optional]
 *                            Should this instance allow logging even if device id check fails?
 *                            This is utilised to generate a single authtoken for multiple users.
 *                            NOT RECOMMENDED!
 *
 * @param repositorySignature [optional]
 *                            The SHA-256 signature of the MADAssistant repository.
 *                            This is to prevent MITM attacks where a third party could impersonate
 *                            the repository's application Id
 *
 * @param cipher [optional] An instance of the cipher. Auto created, if not provided
 *
 * @param connectionManager [optional] An instance of [ConnectionManager]. Auto-created if not provided
 *
 * @param permissionManager [optional] An instance of [PermissionManager]. Auto-created if not provided
 *
 * @param transmitter [optional] An instance of [TransmissionManager]. Auto-created if not provided
 */
class MADAssistantClientImpl(
    private val applicationContext: Context,
    private val passphrase: String,
    private val logUtils: LogUtils? = null,
    private val repositorySignature: String = DEFAULT_SIGNATURE,
    private val ignoreDeviceIdCheck: Boolean = false,
    // Components
    private val cipher: MADAssistantCipher = MADAssistantCipherImpl(passPhrase = passphrase),
    private val connectionManager: ConnectionManager = ConnectionManagerImpl(
        applicationContext = applicationContext,
        logUtils = logUtils,
        repositorySignature = repositorySignature
    ),
    private val permissionManager: PermissionManager = PermissionManagerImpl(
        cipher = cipher,
        logUtils = logUtils,
        ignoreDeviceIdCheck = ignoreDeviceIdCheck
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
        private const val DEFAULT_SIGNATURE =
            "1B:C0:79:26:82:9E:FB:96:5C:6A:51:6C:96:7C:52:88:42:7E:" +
                    "73:8C:05:7D:60:D8:13:9D:C4:3C:18:3B:E3:63"
    }

    private var exceptionHandler: Thread.UncaughtExceptionHandler? = null

    init {
        connectionManager.setCallback(this)
    }

    override fun logCrashes() {
        if (exceptionHandler == null) {
            val def = Thread.getDefaultUncaughtExceptionHandler()

            exceptionHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
                logCrashReport(throwable)
                def?.uncaughtException(thread, throwable)
            }

            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
        }
    }

    override fun onStateChanged(state: ConnectionState) {
        transmitter.setState(state)
    }

    // region Connection
    override fun bindToService() = connectionManager.bindToService()

    override fun unbindService() = connectionManager.unbindService()

    override fun validateHandshakeReponse(response: HandshakeResponseModel?) {
        val errorMessage: String? = when {
            response?.successful == true -> permissionManager.setAuthToken(
                string = response.authToken,
                deviceIdentifier = response.deviceIdentifier
            )
            response?.errorMessage?.isNotBlank() == true -> response.errorMessage
            else -> "Unknown"
        }

        when (errorMessage) {
            null -> {
                logUtils?.i(TAG, "Handshake successful. Starting session")
                startSession()
                onStateChanged(ConnectionState.Connected)
            }
            else -> {
                logUtils?.i(TAG, "Handshake failed. Reason: $errorMessage")
                disconnect()
                onStateChanged(ConnectionState.Disconnected)
            }
        }
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

    private fun disconnect() {
        transmitter.disconnect(-1)
        connectionManager.unbindService()
    }
    // endregion Session

    // region Logging
    override fun isAssistantEnabled(): Boolean = permissionManager.isLoggingEnabled()

    override fun logNetworkCall(data: NetworkCallLogModel) = transmitter.logNetworkCall(
        data = data
    )

    override fun logCrashReport(throwable: Throwable) = transmitter.logCrashReport(
        throwable = throwable
    )

    override fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) = transmitter.logAnalyticsEvent(
        destination = destination,
        eventName = eventName,
        data = data
    )

    override fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>?
    ) = transmitter.logGenericLog(
        type = type,
        tag = tag,
        message = message,
        data = data
    )

    override fun logException(throwable: Throwable) = transmitter.logException(
        throwable = throwable
    )
    // endregion Logging

}