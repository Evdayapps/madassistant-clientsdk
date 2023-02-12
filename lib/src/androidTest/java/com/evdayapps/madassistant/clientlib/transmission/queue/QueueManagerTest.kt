package com.evdayapps.madassistant.clientlib.transmission.queue

import android.os.Handler
import android.os.Message
import android.util.Log
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    @MockK
    lateinit var connectionManager: ConnectionManagerImpl

    lateinit var logger: Logger

    lateinit var queueManager: QueueManagerImpl

    @MockK
    lateinit var callback: QueueManager.Callback

    lateinit var handler: Handler

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        handler = mockkClass(Handler::class)

        val slot = slot<Message>()
        every { handler.sendMessage(capture(slot)) } answers { queueManager.handleMessage(slot.captured) }

        val slotType = slot<Int>()
        val slotObject = slot<Any>()
        every { handler.obtainMessage(capture(slotType), capture(slotObject)) } answers {
            Message.obtain(handler, slotType.captured, slotObject.captured)
        }

        logger = object : Logger {
            override fun i(tag: String, message: String) {
                Log.i(tag, message)
            }

            override fun v(tag: String, message: String) {
                Log.v(tag, message)
            }

            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun w(tag: String, message: String) {
                Log.w(tag, message)
            }

            override fun e(throwable: Throwable) {
                throwable.printStackTrace()
            }

        }

        queueManager = spyk(
            objToCopy = QueueManagerImpl(
                connectionManager = connectionManager,
                handler = handler,
                logger = logger
            ),
            recordPrivateCalls = true
        )

        queueManager.setCallback(callback)
    }

    /**
     * Given: Connection Manager State is none
     * When: addMessage called
     * Expect: Call should not be queued
     */
    @Test
    fun queueHandlingForNoneState() {
        queueManager.addMessageToQueue(
            MADAssistantTransmissionType.Analytics,
            first = "Test",
            sessionId = System.currentTimeMillis()
        )
        verify(exactly = 0) { queueManager.queueMessage(any(), any()) }
    }

    /**
     * Given: ConnectionManager state is connecting
     * When: addMessage called
     * Expect: Call should be queued but not processed
     */
    @Test(timeout = 20000)
    fun queueHandlingWhenConnectingState() {
        queueManager.addMessageToQueue(
            MADAssistantTransmissionType.Analytics,
            first = "Test",
            sessionId = System.currentTimeMillis()
        )
        verify(atLeast = 1) { queueManager.addMessageToQueue(any(), any(), any(), any(), any()) }
        verify(atLeast = 1) { queueManager.queueMessage(any(), any()) }
        verify(exactly = 0) { callback.processQueuedMessage(any(), any()) }
    }

    /**
     * Given: ConnectionManager state is connected
     * When: addMessage called
     * Expect: callback processMessage should be called
     */
    @Test
    fun queueHandlingWhenConnectedState() {

        queueManager.addMessageToQueue(
            MADAssistantTransmissionType.Analytics,
            first = "Test",
            sessionId = System.currentTimeMillis()
        )

        verify(exactly = 1) { queueManager.handleMessage(any()) }
        verify(exactly = 1) { queueManager.queueMessage(any(), any()) }
        verify(exactly = 1) { callback.processQueuedMessage(any(), any()) }
    }

}