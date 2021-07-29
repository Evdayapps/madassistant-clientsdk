package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
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

class TransmissionManagerImpl(
    private val cipher: MADAssistantCipher,
    private val permissionManager: PermissionManager,
    private val connectionManager: ConnectionManager,
    private val logUtils: LogUtils? = null
) : TransmissionManager, Handler.Callback {

    companion object {
        const val MAX_PAYLOAD_SIZE = 900 * 1024
    }

    private val TAG = "TransmissionSenderImpl"

    private var sessionId: Long? = null

    private var _clientThreadHandler: HandlerThread =
        HandlerThread("MADAssistantConnection").apply {
            start()
        }

    private var _clientHandler: Handler = Handler(_clientThreadHandler.looper, this)

    // endregion State

    // region Session Management
    override fun startSession(sessionId: Long) {
        this.sessionId = sessionId
    }

    /**
     * Initiate the disconnection process
     */
    override fun disconnect(code: Int, message: String?) {
        connectionManager.currentState = ConnectionState.Disconnecting
        endSession()
        connectionManager.disconnect(code = code, message = message)
    }

    override fun endSession() {
        connectionManager.endSession()
        this.sessionId = null
    }
    // endregion Session Management

    // region Queue Management
    /**
     * Add a new message to the queue
     * @param type The type of the log. One of [MADAssistantTransmissionType]
     * @param data The payload for the log
     */
    private fun addMessageToQueue(
        type: Int,
        first: Any? = null,
        second: Any? = null,
        third: Any? = null,
        fourth: Any? = null
    ) {
        when (connectionManager.currentState) {
            ConnectionState.None,
            ConnectionState.Connecting,
            ConnectionState.Connected -> {
                try {
                    val data = MessageData(
                        timestamp = System.currentTimeMillis(),
                        threadName = Thread.currentThread().name,
                        first = first,
                        second = second,
                        third = third,
                        fourth = fourth
                    )

                    queueMessage(
                        type = type,
                        data = data
                    )
                } catch (ex: Exception) {
                    logUtils?.e(ex)
                }
            }

            ConnectionState.Disconnecting,
            ConnectionState.Disconnected -> {
                // Don't add any new messages to the queue,
                // since we've already received a disconnect signal
            }
        }
    }

    /**
     * Since the system is not yet ready to send the message (and not disconnecting/disconnected),
     * Queue the message again so its sent when the system is ready
     */
    private fun queueMessage(type: Int, data: Any) {
        _clientHandler.sendMessage(
            _clientHandler.obtainMessage(
                type,
                data
            )
        )
    }

    /**
     * Handles the message received from the Handler Callback
     * Does one of 3 things, depending on connection state:
     * - Sends the message if the state is Connected or Disconnecting (to clear the queue)
     * - Requeue the message if state is None or Connecting
     * - Drops the message if the state is Disconnected
     */
    override fun handleMessage(message: Message): Boolean {
        when (connectionManager.currentState) {
            // If the client is connected/disconnecting, send the message
            ConnectionState.Connected,
            ConnectionState.Disconnecting -> processMessage(
                message.what,
                message.obj as MessageData
            )

            // The client is not yet ready to send messages, requeue the message
            ConnectionState.None,
            ConnectionState.Connecting -> queueMessage(
                type = message.what,
                data = message.obj
            )


            // If the client is disconnected, drop the message
            ConnectionState.Disconnected -> {
                // Drop the message
            }
        }

        return true
    }

    /**
     * Processes (validates, serializes and transmits) the log on a background thread
     *
     */
    private fun processMessage(
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
    // endregion Queue Management

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
        for (i in 0 until numSegments) {
            val startPayloadSegment = i * MAX_PAYLOAD_SIZE
            val endPayloadSegment = minOf(
                startPayloadSegment + MAX_PAYLOAD_SIZE,
                bytes.size
            )
            val payload = bytes.sliceArray(IntRange(startPayloadSegment, endPayloadSegment - 1))

            val model = TransmissionModel(
                sessionId!!,
                transmissionId,
                timestamp,
                encrypted,
                numSegments,
                i,
                type,
                payload
            )

            list.add(model)
        }

        return list
    }

    /**
     * Internal method that does the actual transmission
     *
     */
    private fun transmit(json: String, type: Int, timestamp: Long, encrypt: Boolean) {
        val transmitJson = if (encrypt) cipher.encrypt(json) else json
        jsonToSegments(transmitJson, type, encrypt).forEach {
            it.timestamp = timestamp

            logUtils?.i(
                TAG,
                "transmit: type: $type, enc: $encrypt json: ${json.take(10)}"
            )

            connectionManager.transmit(it)
        }
    }
    // endregion Logging: Common

    // region Logging: Network
    /**
     * Add a network call to the processing queue
     */
    override fun logNetworkCall(data: NetworkCallLogModel) {
        addMessageToQueue(
            type = MADAssistantTransmissionType.NetworkCall,
            first = data
        )
    }

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
    override fun logCrashReport(throwable: Throwable) {
        _processException(
            MessageData(
                timestamp = System.currentTimeMillis(),
                threadName = Thread.currentThread().name,
                first = throwable,
                second = true
            )
        )
    }

    override fun logException(throwable: Throwable) {
        addMessageToQueue(
            type = MADAssistantTransmissionType.Exception,
            first = throwable,
            second = false,
        )
    }

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
     * Enqueue an analytics log for processing
     */
    override fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) {
        addMessageToQueue(
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
    override fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>?
    ) {
        addMessageToQueue(
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