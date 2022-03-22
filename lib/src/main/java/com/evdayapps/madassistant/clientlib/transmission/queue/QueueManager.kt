package com.evdayapps.madassistant.clientlib.transmission.queue

import com.evdayapps.madassistant.clientlib.transmission.MessageData

interface QueueManager {

    interface Callback {
        fun processQueuedMessage(
            type: Int,
            data: MessageData
        )
    }

    fun addMessageToQueue(
        type: Int,
        timestamp: Long = System.currentTimeMillis(),
        sessionId: Long,
        first: Any? = null,
        second: Any? = null,
        third: Any? = null,
        fourth: Any? = null
    )

    fun setCallback(callback: Callback)

    fun isQueueEmpty(): Boolean
}