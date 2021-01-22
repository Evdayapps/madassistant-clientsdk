package com.evdayapps.madassistant.common.encryption

import com.evdayapps.madassistant.common.handshake.*
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test
import java.util.concurrent.TimeUnit

class MADAssistantCipherImplTest {

    @Test
    fun encryptionTest() {
        val cipher = MADAssistantCipherImpl(
            passPhrase = "Thisisatest"
        )

        val plainText = "ThisisaplainText"
        val cipherText = cipher.encrypt(plainText)
        val deciphered = cipher.decrypt(cipherText)

        println("MADAssistantCipherImplTest::encryptionTest: plain: $plainText ciphered: $cipherText deciphered: $deciphered")

        assert(plainText == deciphered)
    }

    @Test
    fun testApiPermissionEncryption() {
        val cipher = MADAssistantCipherImpl(
            passPhrase = "Thisisatest"
        )

        val plainPermission = NetworkCallsPermissionModel(
            enabled = true,
            read = true,
            share = true,
            filterData = null,
            filterSubject = "Test"
        )

        val plain2 = AnalyticsPermissionModel(
            enabled = true,
            read = true,
            share = true,
            filterData = null,
            filterSubject = "Test"
        )

        val ciphered = cipher.encrypt(plainPermission.toJSONObject().toString())

        val deciphered = cipher.decrypt(ciphered)
            ?.run { NetworkCallsPermissionModel(JSONObject(this)) }

        println(
            "MADAssistantCipherImplTest::testApiPermissionEncryption: " +
                    "\n\tplain: $plainPermission " +
                    "\n\tciphered: $ciphered " +
                    "\n\tdeciphered: $deciphered " +
                    "\n\tplain2: $plain2"
        )

        assertTrue(plainPermission == deciphered)

        // Negative test
        assertFalse(plain2 == (deciphered as Any))
    }

    @Test
    fun testAnalyticsPermissionEncryption() {
        val cipher = MADAssistantCipherImpl(
            passPhrase = "Thisisatest"
        )

        val plainPermission = AnalyticsPermissionModel(
            enabled = true,
            read = true,
            share = true,
            filterData = null,
            filterSubject = "Test"
        )

        val ciphered = cipher.encrypt(plainPermission.toJSONObject().toString())

        val deciphered = cipher.decrypt(ciphered)
            ?.run { AnalyticsPermissionModel(JSONObject(this)) }

        println(
            "MADAssistantCipherImplTest::testAnalyticsPermissionEncryption: " +
                    "\n\tplain: $plainPermission " +
                    "\n\tciphered: $ciphered " +
                    "\n\tdeciphered: $deciphered"
        )

        assertTrue(plainPermission == deciphered)
    }

    @Test
    fun testGenericLogsPermissionEncryption() {
        val cipher = MADAssistantCipherImpl(
            passPhrase = "Thisisatest"
        )

        val plainPermission = GenericLogsPermissionModel(
            enabled = true,
            read = true,
            share = true,
            filterData = null,
            filterSubject = "Test"
        )

        val ciphered = cipher.encrypt(plainPermission.toJSONObject().toString())

        val deciphered = cipher.decrypt(ciphered)
            ?.run { GenericLogsPermissionModel(JSONObject(this)) }

        println(
            "MADAssistantCipherImplTest::testGenericLogsPermissionEncryption: " +
                    "\n\tplain: $plainPermission " +
                    "\n\tciphered: $ciphered " +
                    "\n\tdeciphered: $deciphered"
        )

        assertTrue(plainPermission == deciphered)
    }

    @Test
    fun completePermissionStringEncryption() {
        val cipher = MADAssistantCipherImpl(
            passPhrase = "Thisisatest"
        )

        val plain = MADAssistantPermissions(
            deviceId = "TestDeviceId",
            timestampStart = System.currentTimeMillis(),
            timestampEnd = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(90)),
            networkCalls = NetworkCallsPermissionModel(),
            analytics = AnalyticsPermissionModel(),
            crashReports = ExceptionsPermissionModel(),
            genericLogs = GenericLogsPermissionModel()
        )

        val ciphered = cipher.encrypt(plain.toJsonObject().toString())

        val deciphered = cipher.decrypt(ciphered)
            ?.run { MADAssistantPermissions(JSONObject(this)) }

        println(
            "MADAssistantCipherImplTest::jsonifiedPermissionTest: " +
                    "\n\tplain: $plain " +
                    "\n\tciphered: $ciphered " +
                    "\n\tdeciphered: $deciphered"
        )

        assertTrue(plain == deciphered)
    }

}