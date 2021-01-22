package com.evdayapps.madassistant.common.encryption

interface MADAssistantCipher {

    fun decrypt(
        cipherText: String
    ): String?

    fun encrypt(
        plainText: String
    ): String
}