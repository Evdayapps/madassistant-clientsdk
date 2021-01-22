package com.evdayapps.madassistant.common.encryption

import android.util.Base64
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MADAssistantCipherImpl(
    private val passPhrase: String
) : MADAssistantCipher {

    private val charset = Charsets.UTF_8
    private val cipherTransformation = "AES/CBC/PKCS5Padding"
    private val aesEncryptionAlgorithm = "AES"

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class
    )
    private fun decrypt(
        cipherText: ByteArray?,
        key: ByteArray?,
        initialVector: ByteArray?
    ): ByteArray? {
        val cipher: Cipher = Cipher.getInstance(cipherTransformation)
        val secretKeySpecy = SecretKeySpec(key, aesEncryptionAlgorithm)
        val ivParameterSpec = IvParameterSpec(initialVector)

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpecy, ivParameterSpec)
        return cipher.doFinal(cipherText)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class
    )
    private fun encrypt(
        plainText: ByteArray?,
        key: ByteArray?,
        initialVector: ByteArray?
    ): ByteArray? {
        val cipher: Cipher = Cipher.getInstance(cipherTransformation)
        val secretKeySpec = SecretKeySpec(key, aesEncryptionAlgorithm)
        val ivParameterSpec = IvParameterSpec(initialVector)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        return cipher.doFinal(plainText)
    }

    @Throws(UnsupportedEncodingException::class)
    private fun getKeyBytes(key: String): ByteArray {
        val keyBytes = ByteArray(16)
        val parameterKeyBytes: ByteArray = key.toByteArray(charset = charset)
        System.arraycopy(
            parameterKeyBytes,
            0,
            keyBytes,
            0,
            Math.min(parameterKeyBytes.size, keyBytes.size)
        )
        return keyBytes
    }


    @Throws(
        UnsupportedEncodingException::class,
        InvalidKeyException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class
    )
    override fun encrypt(
        plainText: String
    ): String {
        val plainTextbytes: ByteArray = plainText.toByteArray(charset = charset)
        val keyBytes = getKeyBytes(passPhrase)
        return Base64.encodeToString(
            encrypt(plainTextbytes, keyBytes, keyBytes),
            Base64.NO_WRAP
        )
    }


    @Throws(
        KeyException::class,
        GeneralSecurityException::class,
        InvalidAlgorithmParameterException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        IOException::class
    )
    override fun decrypt(cipherText: String): String? {
        val cipheredBytes: ByteArray = Base64.decode(cipherText, Base64.NO_WRAP)
        val keyBytes = getKeyBytes(passPhrase)
        return String(decrypt(cipheredBytes, keyBytes, keyBytes)!!, charset)
    }
}