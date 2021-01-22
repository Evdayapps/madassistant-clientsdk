package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.HandlerThread
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import com.evdayapps.madassistant.common.encryption.MADAssistantCipher
import com.evdayapps.madassistant.common.models.AnalyticsEventModel
import com.evdayapps.madassistant.common.models.ExceptionModel
import com.evdayapps.madassistant.common.models.GenericLogModel
import com.evdayapps.madassistant.common.models.NetworkCallLogModel
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
                (msg.obj as? NetworkCallLogModel)?.let {
                    _logNetworkCall(it)
                }
            }

            MADAssistantTransmissionType.Analytics -> {
                (msg.obj as? Triple<String, String, Map<String, Any?>>)
                    ?.let {
                        _logAnalyticsEvent(
                            destination = it.first,
                            eventName = it.second,
                            data = it.third
                        )
                    }
            }

            MADAssistantTransmissionType.Exception -> {
                (msg.obj as? Pair<Throwable, Boolean>)
                    ?.let {
                        _logException(
                            throwable = it.first,
                            crashReport = it.second
                        )
                    }
            }

            MADAssistantTransmissionType.GenericLogs -> {
                (msg.obj as? Triple<Int, String, String>)
                    ?.let {
                        _logGenericLog(
                            type = it.first,
                            tag = it.second,
                            message = it.third,
                        )
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

    private fun transmit(json: String, type: Int, encrypt: Boolean) {
        val transmitJson = if (encrypt) cipher.encrypt(json) else json
        jsonToSegments(transmitJson, type, encrypt).forEach {
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
                _clientHandler.obtainMessage(MADAssistantTransmissionType.NetworkCall, data)
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }

    }

    private fun _logNetworkCall(data: NetworkCallLogModel) {
        try {
            if (permissionManager.shouldLogNetworkCall(data)) {
                transmit(
                    json = data.toJsonObject().toString(0),
                    type = MADAssistantTransmissionType.NetworkCall,
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
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.Exception,
                    Pair(throwable, true)
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }

    override fun logException(throwable: Throwable) {
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.Exception,
                    Pair(throwable, false)
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }

    private fun _logException(throwable: Throwable, crashReport: Boolean) {
        try {
            if (permissionManager.shouldLogExceptions(throwable)) {
                transmit(
                    json = ExceptionModel(
                        throwable = throwable,
                        isCrash = crashReport
                    ).toJsonObject().toString(0),
                    type = MADAssistantTransmissionType.Exception,
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
                    Triple(destination, eventName, data)
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }

    private fun _logAnalyticsEvent(
        destination: String,
        eventName: String,
        data: Map<String, Any?>
    ) {
        try {
            if (permissionManager.shouldLogAnalytics(destination, eventName)) {
                transmit(
                    type = MADAssistantTransmissionType.Analytics,
                    encrypt = permissionManager.shouldEncryptAnalytics(),
                    json = AnalyticsEventModel(
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
    override fun logGenericLog(type: Int, tag: String, message: String) {
        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(
                    MADAssistantTransmissionType.GenericLogs,
                    Triple(type, tag, message)
                )
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }

    private fun _logGenericLog(type: Int, tag: String, message: String) {
        try {
            if (permissionManager.shouldLogGenericLog(type, tag, message)) {
                transmit(
                    type = MADAssistantTransmissionType.GenericLogs,
                    encrypt = permissionManager.shouldEncryptGenericLogs(),
                    json = GenericLogModel(
                        type = type,
                        tag = tag,
                        message = message
                    ).toJsonObject().toString(0)
                )
            }
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }
    // endregion Logging: Generic Logs
}