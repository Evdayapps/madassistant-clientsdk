package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.HandlerThread
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import com.evdayapps.madassistant.common.encryption.MADAssistantCipher
import com.evdayapps.madassistant.common.models.analytics.AnalyticsEventModel
import com.evdayapps.madassistant.common.models.exceptions.ExceptionModel
import com.evdayapps.madassistant.common.models.genericlog.GenericLogModel
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel
import com.evdayapps.madassistant.common.transmission.TransmissionModel
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

    private var sessionId: Long? = null

    private var _clientThreadHandler: HandlerThread =
        HandlerThread("MADAssistantConnection").apply {
            start()
        }

    private var _clientHandler: Handler
    private val _handlerCallback = Handler.Callback { msg ->
        when (msg.what) {
            MADAssistantTransmissionType.NetworkCall -> {
                (msg.obj as? MessageData)?.let {
                    _processNetworkCall(it)
                }
            }

            MADAssistantTransmissionType.Analytics -> {
                (msg.obj as? MessageData)?.let {
                    _processAnalyticsEvent(it)
                }
            }

            MADAssistantTransmissionType.Exception -> {
                (msg.obj as? MessageData)?.let {
                    _processException(it)
                }
            }

            MADAssistantTransmissionType.GenericLogs -> {
                (msg.obj as? MessageData)?.let {
                    _processGenericLog(it)
                }
            }
        }

        true
    }

    init {
        _clientHandler = Handler(_clientThreadHandler.looper, _handlerCallback)
    }


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
    override fun logNetworkCall(data: NetworkCallLogModel) {
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.NetworkCall,
                    MessageData(
                        threadName = Thread.currentThread().name,
                        first = data
                    )
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }

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
        _internalLogException(throwable, true)
    }

    override fun logException(throwable: Throwable) {
        _internalLogException(throwable, false)
    }

    private fun _internalLogException(throwable: Throwable, crashReport : Boolean) {
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.Exception,
                    MessageData(
                        threadName = Thread.currentThread().name,
                        first = throwable,
                        second = crashReport
                    )
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
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
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.Analytics,
                    MessageData(
                        threadName = Thread.currentThread().name,
                        first = destination,
                        second = eventName,
                        third = data
                    )
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
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
    override fun logGenericLog(
        type: Int,
        tag: String,
        message: String,
        data: Map<String, Any?>?
    ) {
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.GenericLogs,
                    MessageData(
                        threadName = Thread.currentThread().name,
                        first = type,
                        second = tag,
                        third = message,
                        fourth = data
                    )
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
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