package com.evdayapps.madassistant.clientlib.transmission

import android.os.Handler
import android.os.Message
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.connection.ConnectionState
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.MADAssistantTransmissionType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

/**
 * These tests are failing miserably cause of my inability to understand how to handle Handler in tests
 * Will research that and revisit this!
 */
class QueueManagerTest {

    @MockK
    lateinit var connectionManager: ConnectionManager

    @MockK
    lateinit var logUtils: LogUtils

    lateinit var queueManager: QueueManager

    @MockK
    lateinit var callback: QueueManager.Callback

    @MockK
    lateinit var handler: Handler

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val slot = slot<Message>()
        every { handler.sendMessage(capture(slot)) } answers { queueManager.handleMessage(slot.captured) }

        val actualQM = QueueManager(
            connectionManager = connectionManager
        )

        queueManager = spyk(
            objToCopy = actualQM,
            recordPrivateCalls = true
        )

        queueManager.setCallback(callback)
    }

    @Test
    fun addToQueueIfNoConnection() {
        every { connectionManager.currentState } returns ConnectionState.None
        queueManager.addMessageToQueue(MADAssistantTransmissionType.Analytics, first = "Test")
        verify(atLeast = 2) {
            queueManager.queueMessage(
                MADAssistantTransmissionType.Analytics,
                any()
            )
        }
        verify(exactly = 0) {
            callback.processMessage(type = any(), data = any())
        }
    }

}