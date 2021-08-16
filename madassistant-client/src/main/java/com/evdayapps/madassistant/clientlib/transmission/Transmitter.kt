package com.evdayapps.madassistant.clientlib.transmission

import androidx.annotation.VisibleForTesting
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionState
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import com.evdayapps.madassistant.common.cipher.MADAssistantCipher
import com.evdayapps.madassistant.common.models.analytics.AnalyticsEventModel
import com.evdayapps.madassistant.common.models.exceptions.ExceptionModel
import com.evdayapps.madassistant.common.models.genericlog.GenericLogModel
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel
import com.evdayapps.madassistant.common.models.transmission.TransmissionModel
import java.util.*

/**
 * Responsibilities:
 * - Session management
 * - Pseudo queue-management (using [queueManager])
 * - Validating transmissions using [permissionManager]
 * - Converting transmissions to [TransmissionModel]
 * -
 */
class Transmitter(
    private val cipher: MADAssistantCipher,
    private val permissionManager: PermissionManager,
    private val connectionManager: ConnectionManager,
    private val logUtils: LogUtils? = null,
    private val queueManager: TransmissionQueueManager = TransmissionQueueManager(
        connectionManager = connectionManager,
        logUtils = logUtils
    )
) : TransmissionQueueManager.Callback {

    private val TAG = "MADAssist.Transmitter"

    companion object {
        const val MAX_PAYLOAD_SIZE = 900 * 1024
    }

    private var sessionId: Long? = null

    init {
        queueManager.setCallback(this)
    }

    // endregion State

    // region Session Management
    fun startSession(sessionId: Long) {
        this.sessionId = sessionId
    }

    /**
     * Initiate the disconnection process
     */
    fun disconnect(code: Int, message: String?) {
        connectionManager.currentState = ConnectionState.Disconnecting
        endSession()
        connectionManager.disconnect(code = code, message = message)
    }

    fun endSession() {
        connectionManager.endSession()
        this.sessionId = null
    }
    // endregion Session Management

    /**
     * Invokes the correct processing method based on [type]
     */
    override fun processMessage(
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
     * - Generate a transmission id
     * -
     *
     * @param json String to send
     * @param type One of [com.evdayapps.madassistant.common.MADAssistantTransmissionType]
     */
    private fun jsonToSegments(
        json: String,
        type: Int,
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
                sessionId!!,
                transmissionId,
                timestamp,
                encrypted,
                numSegments,
                segmentIndex,
                type,
                payload
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
    internal fun transmit(json: String, type: Int, timestamp: Long, encrypt: Boolean) {
        // Encrypt the payload if required
        val transmitJson = when {
            encrypt -> cipher.encrypt(json)
            else -> json
        }

        // Split the payload into segments to accomodate maximum Bundle limit
        val segments = jsonToSegments(
            json = transmitJson,
            type = type,
            encrypted = encrypt
        )

        // Transmit each segment
        segments.forEachIndexed { index, segment ->
            // Set the timestamp for the first segment
            if (index == 0) {
                segment.timestamp = timestamp
            }

            logUtils?.v(
                TAG,
                "transmit: type: $type, enc: $encrypt json: ${json.take(128)}"
            )

            connectionManager.transmit(segment)
        }
    }
    // endregion Logging: Common

    // region Logging: Network
    /**
     * Add a network call to the processing queue
     */
    fun logNetworkCall(data: NetworkCallLogModel) {
        queueManager.addMessageToQueue(
            type = MADAssistantTransmissionType.NetworkCall,
            timestamp = data.requestTimestamp,
            first = data
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private fun _processNetworkCall(data: MessageData) {
        try {
            val payload = data.first as NetworkCallLogModel
            if (permissionManager.shouldLogNetworkCall(payload)) {
                val json = payload.toJsonObject().toString(0)

                transmit(
                    json = json,
                    type = MADAssistantTransmissionType.NetworkCall,
                    timestamp = data.timestamp,
                    encrypt = permissionManager.shouldEncryptApiLog()
                )
            }
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }
    // endregion Logging: Network

    // region Logging: Crash Reports
    fun logCrashReport(throwable: Throwable) {
        _processException(
            MessageData(
                timestamp = System.currentTimeMillis(),
                threadName = Thread.currentThread().name,
                first = throwable,
                second = true
            )
        )
    }

    /**
     * Adds an exception to the queue
     */
    fun logException(throwable: Throwable) {
        queueManager.addMessageToQueue(
            type = MADAssistantTransmissionType.Exception,
            first = throwable,
            second = false,
        )
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
                    timestamp = messageData.timestamp,
                    encrypt = permissionManager.shouldEncryptCrashReports()
                )
            }
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }
    // endregion Logging: Crash Reports

    // region Logging: Analytics
    /**
     * Adds an analytics log to the queue for processing
     */
    fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) {
        queueManager.addMessageToQueue(
            type = MADAssistantTransmissionType.Analytics,
            first = destination,
            second = eventName,
            third = data
        )
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
                    encrypt = permissionManager.shouldEncryptAnalytics(),
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
            logUtils?.e(ex)
        }
    }
    // endregion Logging: Analytics

    // region Logging: Generic Logs
    /**
     * Enqueue a generic log in the message queue
     */
    fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>?
    ) {
        queueManager.addMessageToQueue(
            type = MADAssistantTransmissionType.GenericLogs,
            first = type,
            second = tag,
            third = message,
            fourth = data,
        )
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
                    encrypt = permissionManager.shouldEncryptGenericLogs(),
                    timestamp = messageData.timestamp,
                    json = payload.toJsonObject().toString(0)
                )
            }
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }
    // endregion Logging: Generic Logs
}