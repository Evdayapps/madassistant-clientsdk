package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.constants.ConnectionState
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
) : TransmissionManager {

    companion object {
        const val MAX_PAYLOAD_SIZE = 900 * 1024
    }

    private val TAG = "TransmissionSenderImpl"

    private var state: ConnectionState = ConnectionState.None
    private var sessionId: Long? = null

    private var _clientThreadHandler: HandlerThread =
        HandlerThread("MADAssistantConnection").apply {
            start()
        }

    private var _clientHandler: Handler
    private val _handlerCallback = Handler.Callback { msg ->
        when (state) {
            ConnectionState.Connected -> processMessage(msg.what, msg.obj as MessageData)
            ConnectionState.Disconnected -> {
                // Nothing to do here. Drop the message
            }

            else -> requeueMessage(msg)
        }

        true
    }

    init {
        _clientHandler = Handler(_clientThreadHandler.looper, _handlerCallback)
    }

    override fun setState(state: ConnectionState) {
        this.state = state
    }
    // endregion State

    // region Session Management
    override fun startSession(sessionId: Long) {
        this.sessionId = sessionId
    }

    override fun disconnect(reason: Int) {
        connectionManager.disconnect(reason)
    }

    override fun endSession() {
        this.sessionId = null
    }
    // endregion Session Management

    // region Queue Handler
    /**
     * Add a new message to the queue
     * @param type The type of the log. One of [MADAssistantTransmissionType]
     * @param data The payload for the log
     */
    private fun addMessage(
        type: Int,
        first: Any? = null,
        second: Any? = null,
        third: Any? = null,
        fourth: Any? = null
    ) {
        if (state != ConnectionState.Disconnected) {
            try {
                val data = MessageData(
                    timestamp = System.currentTimeMillis(),
                    threadName = Thread.currentThread().name,
                    first = first,
                    second = second,
                    third = third,
                    fourth = fourth
                )

                _clientHandler.sendMessage(
                    _clientHandler.obtainMessage(
                        type,
                        data
                    )
                )
            } catch (ex: Exception) {
                logUtils?.e(ex)
            }
        }
    }

    /**
     * Since the system is not yet ready to send the message (and not disconnected)
     * Queue the message again so its sent when the system is ready
     */
    private fun requeueMessage(message: Message) {
        _clientHandler.sendMessage(
            _clientHandler.obtainMessage(
                message.what,
                message.obj
            )
        )
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
    // endregion Queue Handler

    // region Logging: Common
    /**
     * Converts the json string (encrypted or not) to a list of [TransmissionModel] for transmission
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
     * Encrypt (if reqd), split (if reqd) and transmit [json]
     */
    private fun transmit(json: String, type: Int, timestamp: Long, encrypt: Boolean) {
        val transmitJson = if (encrypt) cipher.encrypt(json) else json
        jsonToSegments(transmitJson, type, encrypt).forEach {
            it.timestamp = timestamp

            logUtils?.i(
                TAG,
                "transmit: thread: ${Thread.currentThread().name} type: $type, enc: $encrypt json: $json"
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
        addMessage(
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
        addMessage(
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
    override fun logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) {
        addMessage(
            type = MADAssistantTransmissionType.Analytics,
            first = destination,
            second = eventName,
            third = data
        )
    }

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
        addMessage(
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