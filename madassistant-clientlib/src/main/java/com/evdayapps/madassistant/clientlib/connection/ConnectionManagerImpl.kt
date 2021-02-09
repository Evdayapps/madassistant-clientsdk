package com.evdayapps.madassistant.clientlib.connection

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.IBinder
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.BuildConfig
import com.evdayapps.madassistant.common.MADAssistantConstants
import com.evdayapps.madassistant.common.MADAssistantRepositoryAIDL
import com.evdayapps.madassistant.common.transmission.TransmissionModel
import java.security.MessageDigest

class ConnectionManagerImpl(
    private val applicationContext: Context,
    private val logUtils: LogUtils? = null,
    private val repoKey : String = "1B:C0:79:26:82:9E:FB:96:5C:6A:51:6C:96:7C:52:88:42:7E:" +
            "73:8C:05:7D:60:D8:13:9D:C4:3C:18:3B:E3:63"
) : ConnectionManager, ServiceConnection {

    companion object {
        const val REPO_SERVICE_PACKAGE = "com.evdayapps.madassistant.repository"
        const val REPO_SERVICE_CLASS = "$REPO_SERVICE_PACKAGE.service.MADAssistantRepositoryService"
        const val TAG = "ConnectionManagerImpl"
    }

    private var repositoryServiceAIDL: MADAssistantRepositoryAIDL? = null
    private var callback: ConnectionManager.Callback? = null

    override fun setCallback(callback: ConnectionManager.Callback) {
        this.callback = callback
    }

    /**
     * Attempt a connection to the repository service
     */
    override fun bindToService() {
        if (BuildConfig.DEBUG) {
            logUtils?.i(
                TAG, "Attempting binding to service $REPO_SERVICE_CLASS " +
                        "in package $REPO_SERVICE_PACKAGE"
            )
        }

        val intent = Intent()
        intent.setClassName(REPO_SERVICE_PACKAGE, REPO_SERVICE_CLASS)
        intent.putExtra(
            "info",
            PendingIntent.getActivity(
                applicationContext,
                10,
                Intent(),
                0
            )
        )

        applicationContext.bindService(
            intent,
            this,
            Service.BIND_AUTO_CREATE
        )
    }

    override fun unbindService() = applicationContext.unbindService(this)

    /**
     * Called when a connection to the Service has been lost.  This typically
     * happens when the process hosting the service has crashed or been killed.
     * This does *not* remove the ServiceConnection itself -- this
     * binding to the service will remain active, and you will receive a call
     * to [.onServiceConnected] when the Service is next running.
     *
     * @param name The concrete component name of the service whose
     * connection has been lost.
     */
    override fun onServiceDisconnected(name: ComponentName?) {
        if (BuildConfig.DEBUG) {
            logUtils?.i(TAG, "Service disconnected")
        }
        repositoryServiceAIDL = null
    }

    /**
     * Called when a connection to the Service has been established, with
     * the [android.os.IBinder] of the communication channel to the
     * Service.
     *
     *
     * **Note:** If the system has started to bind your
     * client app to a service, it's possible that your app will never receive
     * this callback. Your app won't receive a callback if there's an issue with
     * the service, such as the service crashing while being created.
     *
     * @param name The concrete component name of the service that has
     * been connected.
     *
     * @param service The IBinder of the Service's communication channel,
     * which you can now make calls on.
     */
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        logUtils?.i(TAG, "Service connected")

        val pkgName = name?.packageName
        if (!pkgName.isNullOrBlank() && isServiceLegit(pkgName)) {
            repositoryServiceAIDL = MADAssistantRepositoryAIDL.Stub.asInterface(service)
            Thread.sleep(300)
            performHandshake()
        } else {
            logUtils?.d(TAG, "Connection failed. Suspicious repository")
            applicationContext.unbindService(this)
        }
    }

    private fun isServiceLegit(packageName: String): Boolean {
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
            getSig(it, "SHA256").equals(repoKey, ignoreCase = true)
        }
    }

    /**
     * Returns whether this client is currently bound to a logging service or not
     * @return true if bound, else false
     */
    override fun isBound(): Boolean = repositoryServiceAIDL != null

    /**
     * Performs the handshake with the repository
     */
    private fun performHandshake() {
        try {
            val response = repositoryServiceAIDL?.performHandshake(
                MADAssistantConstants.LibraryVersion
            )
            callback?.handleHandshakeResponse(response)
        } catch (ex: Exception) {
            logUtils?.e(ex)
            callback?.handleHandshakeResponse(null)
        }
    }

    // region Session Management
    override fun startSession(): Long {
        logUtils?.i(TAG, "Starting new session")
        val sessionId: Long = repositoryServiceAIDL?.startSession()!!
        logUtils?.i(TAG, "Started new session $sessionId")
        return sessionId
    }

    override fun endSession() {
        logUtils?.i(TAG, "Ending session")
        repositoryServiceAIDL?.endSession()
    }
    // endregion Session Management

    // region Logging
    /**
     * Send a log to the repository
     */
    override fun transmit(transmission: TransmissionModel) {
        repositoryServiceAIDL?.let {
            try {
                it.log(transmission)
            } catch (ex: Exception) {
                logUtils?.e(ex)
            }
        }
    }
    // endregion Logging

    // region Utils
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
    // endregion Utils
}