package com.evdayapps.madassistant.clientlib.connection.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

object ConnectionManagerUtils {

    /**
     * Verify that the certificate signature for the repository is legit
     *
     * This is to avoid an MITM attack where another app could be used to intercept logs
     * meant for MADAssistant Repository only
     */
    internal fun isServiceLegit(
        applicationContext: Context,
        repositorySignature : String,
        packageName: String
    ): Boolean {
        val pkgInfo = applicationContext.packageManager.getPackageInfo(
            packageName,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
        )

        val signatures = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            pkgInfo.signatures
        } else if (pkgInfo.signingInfo.hasMultipleSigners()) {
            pkgInfo.signingInfo.apkContentsSigners
        } else {
            pkgInfo.signingInfo.signingCertificateHistory
        }

        return signatures.any {
            getSig(it, "SHA256").equals(repositorySignature, ignoreCase = true)
        }
    }

    private fun getSig(signature: Signature, key: String): String {
        try {
            val md = MessageDigest.getInstance(key)
            md.update(signature.toByteArray())
            val digest = md.digest()
            val toRet = StringBuilder()
            for (i in digest.indices) {
                if (i != 0) toRet.append(":")
                val b = digest[i].toInt() and 0xff
                val hex = Integer.toHexString(b)
                if (hex.length == 1) toRet.append("0")
                toRet.append(hex)
            }

            return toRet.toString()
        } catch (ex: Exception) {
            return "Failed"
        }
    }

}