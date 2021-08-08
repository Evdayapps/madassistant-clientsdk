package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import androidx.annotation.VisibleForTesting
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionState
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.MADAssistantTransmissionType

/**
 * Usecase for queue management.
 *
 * Responsibilities:
 * - Add logs to the queue (with proper checks prior)
 * - Deque logs from queue and check if they need to be transmitted, requeued or discarded
 * - Call [Callback.processMessage] as required
 */
class QueueManager(
    private val connectionManager: ConnectionManager,
    private val logUtils: LogUtils? = null
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
        Handler(_clientThreadHandler.looper, this)
    }

    private var callback: Callback? = null

    /**
     * Add a new message to the queue
     * @param type The type of the log. One of [MADAssistantTransmissionType]
     * @param data The payload for the log
     */
    internal fun addMessageToQueue(
        type: Int,
        timestamp: Long = System.currentTimeMillis(),
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
                        timestamp = timestamp,
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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun queueMessage(type: Int, data: Any) {
        logUtils?.v(
            TAG,
            "queueMessage: state: ${connectionManager.currentState} type: $type data: ${
                data.toString().take(256)
            }"
        )
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
        logUtils?.v(
            TAG,
            "handleMessage: state: ${connectionManager.currentState} message: $message"
        )
        when (connectionManager.currentState) {
            // If the client is connected/disconnecting, send the message
            ConnectionState.Connected,
            ConnectionState.Disconnecting -> callback?.processMessage(
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

    fun setCallback(callback: Callback) {
        this.callback = callback
    }


}