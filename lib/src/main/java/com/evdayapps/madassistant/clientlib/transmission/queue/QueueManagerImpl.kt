package com.evdayapps.madassistant.clientlib.transmission.queue

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.transmission.MessageData
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import java.util.*

/**
 * Log queue management
 *
 * Responsibilities:
 * - Add logs to the queue (with proper checks prior)
 * - Deque logs from queue and check if they need to be transmitted, requeued or discarded
 * - Call [Callback.processMessage] as required
 */
class QueueManagerImpl(
    private val connectionManager: ConnectionManager,
    private val handler: Handler? = null,
    private val logger: Logger? = null
) : QueueManager, Handler.Callback {

    private val TAG = "MADAssist.QueueManager"

    private var _clientThreadHandler: HandlerThread = HandlerThread(TAG).apply { start() }

    private val _clientHandler: Handler by lazy {
        handler ?: Handler(_clientThreadHandler.looper, this)
    }

    private var callback: QueueManager.Callback? = null

    private val cacheTable: Hashtable<Int, MessageData> = Hashtable()

    /**
     * Add a new message to the queue
     * @param type The type of the log. One of [MADAssistantTransmissionType]
     * @param data The payload for the log
     */
    override fun addMessageToQueue(
        type: Int,
        timestamp: Long,
        sessionId: Long,
        first: Any?,
        second: Any?,
        third: Any?,
        fourth: Any?
    ) {
        if (connectionManager.isConnectedOrConnecting()) {
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
                cacheTable[key] = data

                queueMessage(type = type, key = key)
            } catch (ex: Exception) {
                logger?.e(ex)
            }
        }
    }

    /**
     * Since the system is not yet ready to send the message (and not disconnecting/disconnected),
     * Queue the message again so its sent when the system is ready
     */
    internal fun queueMessage(type: Int, key: Int) {
        /*logger?.v(
            TAG,
            "queueMessage: state: ${connectionManager.currentState} type: $type data: ${
                key.toString().take(256)
            }"
        )*/

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
     *
     * Does one of 3 things, depending on connection state:
     * - Sends the message if the state is Connected or Disconnecting (to clear the queue)
     * - Requeue the message if state is None or Connecting
     * - Drops the message if the state is Disconnected
     */
    override fun handleMessage(message: Message): Boolean {
        /*logger?.v(
            TAG,
            "handleMessage: state: ${connectionManager.currentState} message: $message"
        )*/

        when {
            connectionManager.isConnected() || connectionManager.isDisconnecting() -> {
                val data = cacheTable[message.arg1]
                callback?.processQueuedMessage(type = message.what, data = data as MessageData)

                cacheTable.remove(message.arg1)
            }

            connectionManager.isConnecting() -> queueMessage(
                type = message.what,
                key = message.arg1
            )
        }

        return true
    }

    override fun setCallback(callback: QueueManager.Callback) {
        this.callback = callback
    }

    override fun isQueueEmpty(): Boolean {
        return when {
            handler?.hasMessages(MADAssistantTransmissionType.NetworkCall) == true -> false
            handler?.hasMessages(MADAssistantTransmissionType.Analytics) == true -> false
            handler?.hasMessages(MADAssistantTransmissionType.Exception) == true -> false
            handler?.hasMessages(MADAssistantTransmissionType.GenericLogs) == true -> false
            else -> true
        }
    }

}