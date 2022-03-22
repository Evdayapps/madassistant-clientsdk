package com.evdayapps.madassistant.clientlib

import android.content.Context
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManagerImpl
import com.evdayapps.madassistant.clientlib.transmission.Transmitter
import com.evdayapps.madassistant.clientlib.transmission.TransmitterImpl
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.cipher.MADAssistantCipher
import com.evdayapps.madassistant.common.cipher.MADAssistantCipherImpl
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel

/**
 * An implementation of [MADAssistantClient]
 * @property applicationContext The application context
 * @property passphrase The encryption passphrase for the client
 * @property logger Instance of [Logger]
 * @property ignoreDeviceIdCheck Should this instance allow logging even if device id check fails?
 *                            This is utilised to generate a single auth-token for multiple users.
 *                            NOT RECOMMENDED!
 * @property repositorySignature The SHA-256 signature of the MADAssistant repository.
 *                            This is to prevent MITM attacks where a third party could impersonate
 *                            the repository's application Id
 * @property cipher And optional implementation of [MADAssistantCipher]
 * @property connectionManager Optional implementation of [ConnectionManagerImpl]
 * @property permissionManager An instance of [PermissionManager]. Auto-created if not provided
 * @property transmitter An instance of [TransmissionManager]. Auto-created if not provided
 */
class MADAssistantClientImpl(
    private val applicationContext: Context,
    private val passphrase: String,
    private val logger: Logger? = null,
    private val repositorySignature: String = DEFAULT_SIGNATURE,
    private val ignoreDeviceIdCheck: Boolean = false,
    // Components
    private val cipher: MADAssistantCipher = MADAssistantCipherImpl(
        passPhrase = passphrase
    ),
    private val permissionManager: PermissionManager = PermissionManagerImpl(
        cipher = cipher,
        logger = logger,
        ignoreDeviceIdCheck = ignoreDeviceIdCheck
    ),
    private val connectionManager: ConnectionManager = ConnectionManagerImpl(
        applicationContext = applicationContext,
        logger = logger,
        repositorySignature = repositorySignature,
        permissionManager = permissionManager,
    ),
    private val transmitter: Transmitter = TransmitterImpl(
        cipher = cipher,
        permissionManager = permissionManager,
        connectionManager = connectionManager,
        logger = logger
    ),
    private val callback: MADAssistantClient.Callback
) : MADAssistantClient, ConnectionManager.Callback, Transmitter.Callback {

    companion object {
        private const val TAG = "MADAssistantClientImpl"
        private const val DEFAULT_SIGNATURE =
            "1B:C0:79:26:82:9E:FB:96:5C:6A:51:6C:96:7C:52:88:42:7E:" +
                    "73:8C:05:7D:60:D8:13:9D:C4:3C:18:3B:E3:63"

    }

    private var exceptionHandler: Thread.UncaughtExceptionHandler? = null

    init {
        connectionManager.setCallback(this)
        transmitter.setCallback(this)
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

    // region Connection Management
    /**
     * - Initiates a connection to the repository service
     * - Also starts a new session, so that logs are tagged accordingly
     */
    override fun connect() = connectionManager.connect()

    override fun disconnect() = transmitter.disconnect(
        code = 200,
        message = "Client requested disconnection"
    )

    override fun onConnected() {
        callback.onConnected()
    }

    override fun onDisconnected(code: Int, message: String) {
        callback.onDisconnected(code = code, message = message)
    }
    // endregion Connection Management

    // region Session Management
    /**
     * Start a new session
     * All logs need to be encapsulated within a session
     */
    override fun startSession() = transmitter.startSession()

    override fun onSessionStarted(sessionId: Long) {
        callback.onSessionStarted(sessionId = sessionId)
    }

    /**
     * End an ongoing session
     */
    override fun endSession() = transmitter.endSession()

    override fun onSessionEnded(sessionId: Long) {
        callback.onSessionEnded(sessionId = sessionId)
    }
    // endregion Session Management

    // region Logging
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