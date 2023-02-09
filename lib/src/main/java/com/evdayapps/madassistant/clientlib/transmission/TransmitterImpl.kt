package com.evdayapps.madassistant.clientlib.transmission

import androidx.annotation.VisibleForTesting
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.transmission.queue.QueueManager
import com.evdayapps.madassistant.clientlib.transmission.queue.QueueManagerImpl
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import com.evdayapps.madassistant.common.cipher.MADAssistantCipher
import com.evdayapps.madassistant.common.models.analytics.AnalyticsEventModel
import com.evdayapps.madassistant.common.models.exceptions.ExceptionModel
import com.evdayapps.madassistant.common.models.genericlog.GenericLogModel
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel
import com.evdayapps.madassistant.common.models.transmission.TransmissionModel
import org.json.JSONArray
import java.util.*

/**
 * Responsibilities:
 * - Session management
 * - Pseudo queue-management (using [queueManager])
 * - Validating transmissions using [permissionManager]
 * - Converting transmissions to [TransmissionModel]
 * -
 */
class TransmitterImpl(
    private val cipher: MADAssistantCipher,
    private val permissionManager: PermissionManager,
    private val connectionManager: ConnectionManager,
    private val logger: Logger? = null,
    private val verboseLogging: Boolean = false,
    private val queueManager: QueueManager = QueueManagerImpl(
        connectionManager = connectionManager,
        logger = logger
    )
) : Transmitter, QueueManager.Callback {

    private val TAG = "MADAssist.Transmitter"

    companion object {
        const val MAX_PAYLOAD_SIZE = 900 * 1024
    }

    private var sessionId: Long = -1L

    private var _callback: Transmitter.Callback? = null

    init {
        queueManager.setCallback(this)
    }

    // endregion State

    override fun setCallback(callback: Transmitter.Callback) {
        this._callback = callback
    }

    // region Session Management
    /**
     * Initiate the disconnection process
     */
    override fun disconnect(code: Int, message: String) {
        connectionManager.disconnect(
            code = code,
            message = message,
            hasPendingLogs = !queueManager.isQueueEmpty()
        )
    }

    // region Session Management
    /**
     * Begins a new session
     *
     * - Ends any ongoing session
     * - Creates a new session id
     * - Informs the repository
     */
    override fun startSession() {
        // End any existing session
        if (sessionId != -1L) {
            endSession()
        }

        // Only start a new session if the connection is connected or connecting
        if (connectionManager.isConnectedOrConnecting()) {
            // Set a new session Id
            this.sessionId = System.currentTimeMillis()

            // Inform the repository
            if (connectionManager.isConnected()) {
                connectionManager.startSession(sessionId)
            }

            _callback?.onSessionStarted(sessionId)
        }
    }

    override fun endSession() {
        connectionManager.endSession(sessionId)
        _callback?.onSessionEnded(sessionId)
        sessionId = -1
    }
    // endregion Session Management

    /**
     * Invokes the correct processing method based on [type]
     */
    override fun processQueuedMessage(
        type: Int,
        data: MessageData
    ) {
        when (type) {
            MADAssistantTransmissionType.NetworkCall -> _processNetworkCall(data)
            MADAssistantTransmissionType.Analytics -> _processAnalyticsEvent(data)
            MADAssistantTransmissionType.Exception -> _processException(data)
            MADAssistantTransmissionType.GenericLogs -> _processGenericLog(data)
        }
    }

    // region Logging: Common
    /**
     * Converts the json string (encrypted or not) to a list of [TransmissionModel] for transmission
     * Performs the following:
     * - Generates a transmissionId and timestamp for the entire log
     * - Splits the transmission into chunks if required
     * - Returns a [TransmissionModel] for each chunk
     *
     * @param json String to send
     * @param type One of [MADAssistantTransmissionType]
     */
    private fun jsonToSegments(
        json: String,
        type: Int,
        sessionId: Long,
        encrypted: Boolean
    ): List<TransmissionModel> {
        val transmissionId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val bytes = json.toByteArray(Charsets.UTF_16)

        val numSegments = bytes.size / MAX_PAYLOAD_SIZE + when {
            bytes.size % MAX_PAYLOAD_SIZE > 0 -> 1
            else -> 0
        }

        val list = mutableListOf<TransmissionModel>()
        for (segmentIndex in 0 until numSegments) {
            val segmentStart = segmentIndex * MAX_PAYLOAD_SIZE
            val segmentEnd = minOf(
                segmentStart + MAX_PAYLOAD_SIZE,
                bytes.size
            )
            val payload = bytes.sliceArray(
                IntRange(segmentStart, segmentEnd - 1)
            )

            val model = TransmissionModel(
                transmissionId = transmissionId,
                sessionId = sessionId,
                timestamp = timestamp,
                encrypted = encrypted,
                numTotalSegments = numSegments,
                currentSegmentIndex = segmentIndex,
                type = type,
                payload = payload
            )

            list.add(model)
        }

        return list
    }

    /**
     * Internal method that does the actual transmission
     * @param json the payload serialized into a JSON string
     * @param type The type of log
     * @param timestamp The time at which this log was added to the handler
     * @param encrypt Whether to encrypt the payload or not
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun transmit(
        json: String,
        type: Int,
        sessionId: Long,
        timestamp: Long,
        encrypt: Boolean
    ) {
        // Encrypt the payload if required
        val transmitJson = when {
            encrypt -> cipher.encrypt(json)
            else -> json
        }

        // Split the payload into segments to accommodate maximum Bundle limit
        val segments = jsonToSegments(
            json = transmitJson,
            type = type,
            sessionId = sessionId,
            encrypted = encrypt
        )

        // Transmit each segment
        segments.forEachIndexed { index, segment ->
            // Set the timestamp for the first segment
            if (index == 0) {
                segment.timestamp = timestamp
            }

            connectionManager.transmit(segment)

            if (verboseLogging) {
                logger?.v(
                    TAG,
                    "transmit: type: $type, enc: $encrypt json: ${json.take(128)}"
                )
            }
        }

        // If the connection state is DISCONNECTING and the queue is clear, change the state to
        // DISCONNECTED
        if ((connectionManager.currentState == ConnectionManager.State.Disconnecting) && queueManager.isQueueEmpty()) {
            connectionManager.disconnect(
                code = -1,
                message = "Client Disconnected",
                hasPendingLogs = false
            )
        }
    }
    // endregion Logging: Common

    // region Logging: Network
    /**
     * Add a network call to the processing queue
     */
    override fun logNetworkCall(data: NetworkCallLogModel) {
        if (sessionId != -1L) {
            queueManager.addMessageToQueue(
                type = MADAssistantTransmissionType.NetworkCall,
                timestamp = data.requestTimestamp,
                sessionId = sessionId,
                first = data
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private fun _processNetworkCall(data: MessageData) {

        /**
         * Internal method
         * Trims the list to remove any redacted headers as per the permissions
         */
        fun trimRedactedHeaders(headersArray: JSONArray?): JSONArray? {
            if (headersArray == null) {
                return null
            }

            val trimmed = JSONArray()
            for (i in 0 until headersArray.length()) {
                val item = headersArray.getJSONObject(i)
                if (!permissionManager.shouldRedactNetworkHeader(item.keys().next())) {
                    trimmed.put(item)
                }
            }

            return trimmed
        }

        try {
            val payload = data.first as NetworkCallLogModel
            if (permissionManager.shouldLogNetworkCall(payload)) {
                // Redact headers as required
                payload.requestHeaders = trimRedactedHeaders(payload.requestHeaders)
                payload.responseHeaders = trimRedactedHeaders(payload.responseHeaders)

                val json = payload.toJsonObject().toString(0)

                transmit(
                    json = json,
                    type = MADAssistantTransmissionType.NetworkCall,
                    sessionId = data.sessionId,
                    timestamp = data.timestamp,
                    encrypt = permissionManager.shouldEncryptLogs()
                )
            }
        } catch (ex: Exception) {
            logger?.e(ex)
        }
    }
    // endregion Logging: Network

    // region Logging: Crash Reports
    /**
     * Immediately send the crash report to the repository. This method bypasses the
     * TODO: Process the message queue
     */
    override fun logCrashReport(throwable: Throwable) {
        if (sessionId != -1L) {
            _processException(
                MessageData(
                    timestamp = System.currentTimeMillis(),
                    threadName = Thread.currentThread().name,
                    sessionId = sessionId,
                    first = throwable,
                    second = true
                )
            )
        }
    }

    /**
     * Adds an exception to the queue
     */
    override fun logException(throwable: Throwable) {
        if (sessionId != -1L) {
            queueManager.addMessageToQueue(
                type = MADAssistantTransmissionType.Exception,
                sessionId = sessionId,
                first = throwable,
                second = false,
            )
        }
    }

    /**
     * Handles an exception from the queue
     */
    private fun _processException(messageData: MessageData) {
        try {
            val throwable: Throwable = messageData.first as Throwable
            if (permissionManager.shouldLogExceptions(throwable)) {
                val isCrash = messageData.second as Boolean
                val json = ExceptionModel(
                    threadName = messageData.threadName,
                    throwable = throwable,
                    isCrash = isCrash
                ).toJsonObject().toString(0)

                transmit(
                    json = json,
                    type = MADAssistantTransmissionType.Exception,
                    sessionId = messageData.sessionId,
                    timestamp = messageData.timestamp,
                    encrypt = permissionManager.shouldEncryptLogs()
                )
            }
        } catch (ex: Exception) {
            logger?.e(ex)
        }
    }
    // endregion Logging: Crash Reports

    // region Logging: Analytics
    /**
     * Adds an analytics log to the queue for processing
     */
    override fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) {
        if (sessionId != -1L) {
            queueManager.addMessageToQueue(
                type = MADAssistantTransmissionType.Analytics,
                first = destination,
                sessionId = sessionId,
                second = eventName,
                third = data
            )
        }
    }

    /**
     * Process the analytics log in the handler thread
     */
    private fun _processAnalyticsEvent(messageData: MessageData) {
        try {
            val destination = messageData.first as String
            val eventName = messageData.second as String
            val data = messageData.third as Map<String, Any?>
            if (permissionManager.shouldLogAnalytics(destination, eventName, data)) {
                transmit(
                    type = MADAssistantTransmissionType.Analytics,
                    sessionId = messageData.sessionId,
                    encrypt = permissionManager.shouldEncryptLogs(),
                    timestamp = messageData.timestamp,
                    json = AnalyticsEventModel(
                        threadName = messageData.threadName,
                        destination = destination,
                        name = eventName,
                        params = data
                    ).toJsonObject().toString(0),
                )
            }
        } catch (ex: Exception) {
            logger?.e(ex)
        }
    }
    // endregion Logging: Analytics

    // region Logging: Generic Logs
    /**
     * Enqueue a generic log in the message queue
     */
    override fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>?
    ) {
        if (this.sessionId != -1L) {
            queueManager.addMessageToQueue(
                type = MADAssistantTransmissionType.GenericLogs,
                sessionId = sessionId,
                first = type,
                second = tag,
                third = message,
                fourth = data,
            )
        }
    }

    private fun _processGenericLog(messageData: MessageData) {
        try {
            val type = messageData.first as Int
            val tag = messageData.second as String
            val message = messageData.third as String

            if (permissionManager.shouldLogGenericLog(type, tag, message)) {
                val payload = GenericLogModel(
                    threadName = messageData.threadName,
                    type = type,
                    tag = tag,
                    message = message,
                    data = messageData.fourth as? Map<String, Any?>
                )

                transmit(
                    type = MADAssistantTransmissionType.GenericLogs,
                    sessionId = messageData.sessionId,
                    encrypt = permissionManager.shouldEncryptLogs(),
                    timestamp = messageData.timestamp,
                    json = payload.toJsonObject().toString(0)
                )
            }
        } catch (ex: Exception) {
            logger?.e(ex)
        }
    }
    // endregion Logging: Generic Logs
}