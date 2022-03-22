package com.evdayapps.madassistant.clientlib.transmission

import android.util.Log
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.transmission.queue.QueueManagerImpl
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.cipher.MADAssistantCipher
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class TransmitterTest {

    lateinit var transmitter: TransmitterImpl

    @MockK
    lateinit var connectionManager: ConnectionManagerImpl

    @MockK
    lateinit var transmisionQueueManager: QueueManagerImpl

    @MockK
    lateinit var permissionManager: PermissionManager

    @MockK
    lateinit var cipher: MADAssistantCipher

    lateinit var logger: Logger

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        val slotCipher = slot<String>()
        every { cipher.encrypt(capture(slotCipher)) } answers { slotCipher.captured }

        every { transmisionQueueManager.setCallback(any()) } answers {}

        every { connectionManager.transmit(any()) } answers {}

        val slotType = slot<Int>()
        val slotTimestamp = slot<Long>()
        val slotSessionId = slot<Long>()
        val slotFirst = slot<Any>()
        val slotSecond = mutableListOf<Any?>()
        val slotThird = mutableListOf<Any?>()
        val slotFourth = mutableListOf<Any?>()
        every {
            transmisionQueueManager.addMessageToQueue(
                type = capture(slotType),
                timestamp = capture(slotTimestamp),
                sessionId = capture(slotSessionId),
                first = capture(slotFirst),
                second = captureNullable(slotSecond),
                third = captureNullable(slotThird),
                fourth = captureNullable(slotFourth)
            )
        } answers {
            transmitter.processQueuedMessage(
                type = slotType.captured,
                data = MessageData(
                    timestamp = slotTimestamp.captured,
                    threadName = "Test",
                    sessionId = slotSessionId.captured,
                    first = slotFirst.captured,
                    second = slotSecond.getOrNull(0),
                    third = slotThird.getOrNull(0),
                    fourth = slotFourth.getOrNull(0)
                )
            )
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

        transmitter = spyk(
            TransmitterImpl(
                cipher = cipher,
                connectionManager = connectionManager,
                queueManager = transmisionQueueManager,
                permissionManager = permissionManager,
                logger = logger
            )
        )

        transmitter.startSession()
    }

    @Test
    fun dontTransmitAnalyticsNoLogsWhenPermissionDenied() {
        every { permissionManager.shouldLogAnalytics(any(), any(), any()) } returns false
        transmitter.logAnalyticsEvent(
            destination = "Fake Destination",
            eventName = "Fake Event Name",
            data = mapOf(
                "param1" to true,
                "param2" to "value"
            )
        )
        verify(exactly = 0) {
            transmitter.transmit(any(), any(), any(), any(), any())
        }
    }

    /**
     * Transmit method should be called when analytics are permitted by the permission manager
     */
    @Test
    fun transmitAnalyticsLogsWhenPermissionGranted() {
        every { permissionManager.shouldLogAnalytics(any(), any(), any()) } returns true

        // Unencrypted
        every { permissionManager.shouldEncryptLogs() } returns false
        transmitter.logAnalyticsEvent(
            destination = "Fake Destination",
            eventName = "Fake Event Name",
            data = mapOf(
                "param1" to true,
                "param2" to "value"
            )
        )
        verify(exactly = 0) {
            cipher.encrypt(any())
        }
        verify(exactly = 1) {
            transmitter.transmit(any(), any(), any(), any(), any())
        }

        // Encrypted
        every { permissionManager.shouldEncryptLogs() } returns true
        transmitter.logAnalyticsEvent(
            destination = "Fake Destination",
            eventName = "Fake Event Name",
            data = mapOf(
                "param1" to true,
                "param2" to "value"
            )
        )

        verify(exactly = 1) {
            cipher.encrypt(any())
        }

        verify(exactly = 2) {
            transmitter.transmit(any(), any(), any(), any(), any())
        }
    }

}