package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.Message
import android.util.Log
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionState
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class TransmissionQueueManagerTest {

    @MockK
    lateinit var connectionManager: ConnectionManager

    lateinit var logger: Logger

    lateinit var queueManager: TransmissionQueueManager

    @MockK
    lateinit var callback: TransmissionQueueManager.Callback

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

            override fun e(throwable: Throwable) {
                throwable.printStackTrace()
            }

        }

        queueManager = spyk(
            objToCopy = TransmissionQueueManager(
                connectionManager = connectionManager,
                handler = handler,
                logUtils = logger
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
        every { connectionManager.currentState } returns ConnectionState.None

        queueManager.addMessageToQueue(MADAssistantTransmissionType.Analytics, first = "Test")
        verify(exactly = 0) { queueManager.queueMessage(any(), any()) }
    }

    /**
     * Given: ConnectionManager state is connecting
     * When: addMessage called
     * Expect: Call should be queued but not processed
     */
    @Test(timeout = 20000)
    fun queueHandlingWhenConnectingState() {
        every { connectionManager.currentState } returns ConnectionState.Connecting
        queueManager.addMessageToQueue(MADAssistantTransmissionType.Analytics, first = "Test")
        verify(atLeast = 1) { queueManager.addMessageToQueue(any(), any(), any(), any(), any()) }
        verify(atLeast = 1) { queueManager.queueMessage(any(), any()) }
        verify(exactly = 0) { callback.processMessage(any(), any()) }
    }

    /**
     * Given: ConnectionManager state is connected
     * When: addMessage called
     * Expect: callback processMessage should be called
     */
    @Test
    fun queueHandlingWhenConnectedState() {
        every { connectionManager.currentState } returns ConnectionState.Connected

        queueManager.addMessageToQueue(MADAssistantTransmissionType.Analytics, first = "Test")

        verify(exactly = 1) { queueManager.handleMessage(any()) }
        verify(exactly = 1) { queueManager.queueMessage(any(), any()) }
        verify(exactly = 1) { callback.processMessage(any(), any()) }
    }

}