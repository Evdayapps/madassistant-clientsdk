package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionState
import com.evdayapps.madassistant.clientlib.transmission.TransmissionQueueManager.Callback
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import java.util.*

/**
 * Usecase for queue management.
 *
 * Responsibilities:
 * - Add logs to the queue (with proper checks prior)
 * - Deque logs from queue and check if they need to be transmitted, requeued or discarded
 * - Call [Callback.processMessage] as required
 */
class TransmissionQueueManager(
    private val connectionManager: ConnectionManager,
    private val handler: Handler? = null,
    private val logger: Logger? = null
) : Handler.Callback {

    private val TAG = "MADAssist.QueueManager"

    interface Callback {
        fun processMessage(
            type: Int,
            data: MessageData
        )
    }

    private var _clientThreadHandler: HandlerThread = HandlerThread(TAG).apply { start() }

    private val _clientHandler: Handler by lazy {
        handler ?: Handler(_clientThreadHandler.looper, this)
    }

    private var callback: Callback? = null

    private val cacheTable : Hashtable<Int, MessageData> = Hashtable()

    /**
     * Add a new message to the queue
     * @param type The type of the log. One of [MADAssistantTransmissionType]
     * @param data The payload for the log
     */
    internal fun addMessageToQueue(
        type: Int,
        timestamp: Long = System.currentTimeMillis(),
        sessionId: Long,
        first: Any? = null,
        second: Any? = null,
        third: Any? = null,
        fourth: Any? = null
    ) {
        when (connectionManager.currentState) {
            ConnectionState.Connecting,
            ConnectionState.Connected -> {
                try {
                    val data = MessageData(
                        timestamp = timestamp,
                        threadName = Thread.currentThread().name,
                        sessionId = sessionId,
                        first = first,
                        second = second,
                        third = third,
                        fourth = fourth
                    )
                    val key = "$type:$timestamp".hashCode()
                    cacheTable.put(key, data)

                    queueMessage(
                        type = type,
                        key = key
                    )
                } catch (ex: Exception) {
                    logger?.e(ex)
                }
            }

            ConnectionState.None,
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
    internal fun queueMessage(type: Int, key: Int) {
        logger?.v(
            TAG,
            "queueMessage: state: ${connectionManager.currentState} type: $type data: ${
                key.toString().take(256)
            }"
        )

        try {
            _clientHandler.sendMessage(
                _clientHandler.obtainMessage(type).apply { arg1 = key }
            )
        } catch (ex: Exception) {
            logger?.e(ex)
        }
    }

    /**
     * Handles the message received from the Handler Callback
     * Does one of 3 things, depending on connection state:
     * - Sends the message if the state is Connected or Disconnecting (to clear the queue)
     * - Requeue the message if state is None or Connecting
     * - Drops the message if the state is Disconnected
     */
    override fun handleMessage(message: Message): Boolean {
        logger?.v(
            TAG,
            "handleMessage: state: ${connectionManager.currentState} message: $message"
        )

        when (connectionManager.currentState) {
            // If the client is connected/disconnecting, send the message
            ConnectionState.Connected,
            ConnectionState.Disconnecting -> {
                val data = cacheTable[message.arg1]
                callback?.processMessage(
                    type = message.what,
                    data = data as MessageData
                )

                cacheTable.remove(message.arg1)
            }

            // The client is not yet ready to send messages, requeue the message
            ConnectionState.Connecting -> queueMessage(
                type = message.what,
                key = message.arg1
            )

            // If the client is disconnected or none, drop the message
            ConnectionState.None,
            ConnectionState.Disconnected -> {
                // Drop the message
            }
        }

        return true
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun isQueueEmpty(): Boolean {
        return when {
            handler?.hasMessages(MADAssistantTransmissionType.NetworkCall) == true -> false
            handler?.hasMessages(MADAssistantTransmissionType.Analytics) == true -> false
            handler?.hasMessages(MADAssistantTransmissionType.Exception) == true -> false
            handler?.hasMessages(MADAssistantTransmissionType.GenericLogs) == true -> false
            else -> true
        }
    }


}