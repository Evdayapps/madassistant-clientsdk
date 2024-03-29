package com.evdayapps.madassistant.clientlib.connection

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import com.evdayapps.madassistant.clientlib.permission.PermissionManager
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.common.MADAssistantClientAIDL
import com.evdayapps.madassistant.common.MADAssistantConstants
import com.evdayapps.madassistant.common.MADAssistantRepositoryAIDL
import com.evdayapps.madassistant.common.models.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.models.transmission.TransmissionModel
import java.security.MessageDigest

class ConnectionManagerImpl(
    private val applicationContext: Context,
    private val logger: Logger? = null,
    private val repositorySignature: String = DEFAULT_REPO_SIGNATURE,
    private val permissionManager: PermissionManager,
) : ConnectionManager, ServiceConnection, MADAssistantClientAIDL.Stub() {

    companion object {
        const val REPO_SERVICE_PACKAGE = "com.evdayapps.madassistant.repository"
        const val REPO_SERVICE_CLASS = "$REPO_SERVICE_PACKAGE.service.MADAssistantService"
        const val DEFAULT_REPO_SIGNATURE = "1B:C0:79:26:82:9E:FB:96:5C:6A:51:6C:96:7C:52:88:42:" +
                "7E:73:8C:05:7D:60:D8:13:9D:C4:3C:18:3B:E3:63"
    }

    private val TAG = "MADAssist:ConnectionManagerImpl"

    private var currentState: ConnectionManager.State = ConnectionManager.State.None

    private var callback: ConnectionManager.Callback? = null
    private var repositoryServiceAIDL: MADAssistantRepositoryAIDL? = null

    override fun getState(): ConnectionManager.State = currentState

    override fun setCallback(callback: ConnectionManager.Callback) {
        this.callback = callback
    }

    /**
     * Attempt a connection to the repository service
     */
    override fun connect() {
        logger?.i(
            TAG,
            "Attempting binding to service $REPO_SERVICE_CLASS"
        )

        setConnectionState(ConnectionManager.State.Connecting)

        val intent = Intent()
        intent.setClassName(REPO_SERVICE_PACKAGE, REPO_SERVICE_CLASS)
        intent.putExtra(
            MADAssistantConstants.KEY_EXTRA_INFO,
            PendingIntent.getActivity(
                applicationContext,
                10,
                Intent(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            )
        )
        intent.putExtra(
            MADAssistantConstants.KEY_EXTRA_AIDL_VERSION,
            MADAssistantConstants.AIDLVersion
        )

        val success = applicationContext.bindService(
            intent,
            this,
            Service.BIND_AUTO_CREATE
        )

        logger?.i(TAG, "bindToService: ${if (success) "Successful" else "Failed"}")

        if (!success) {
            disconnect(
                code = 404,
                message = "Service not found",
                hasPendingLogs = false
            )
        }
    }

    private fun setConnectionState(state: ConnectionManager.State) {
        if (state != currentState) {
            this.currentState = state
            callback?.onConnectionStateChanged(currentState)
            logger?.i(TAG, "Connection State changed to $state")
        }
    }

    override fun isConnected(): Boolean = currentState == ConnectionManager.State.Connected

    override fun isConnecting(): Boolean = currentState == ConnectionManager.State.Connecting

    override fun isConnectedOrConnecting(): Boolean = isConnected() || isConnecting()

    override fun isDisconnecting(): Boolean = currentState == ConnectionManager.State.Disconnecting

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
        logger?.i(TAG, "Service connected")
        onServiceConnected(name?.packageName, service)
    }

    private fun onServiceConnected(packageName: String?, service: IBinder?) {
        val errorMessage: String? = validateRepositorySignature(
            servicePackageName = packageName,
            repositorySignature = repositorySignature,
            serviceSignatures = getSignatureList(applicationContext, packageName)
        )

        when {
            errorMessage.isNullOrBlank() -> {
                logger?.i(TAG, "Repository validated")
                repositoryServiceAIDL = MADAssistantRepositoryAIDL.Stub.asInterface(service)
                initHandshake()
            }
            else -> {
                logger?.i(TAG, "Repository invalid. Error: $errorMessage")
                unbindService()
            }
        }
    }

    /**
     * Check that the repository service signature is legit
     * This ensures that no malicious app can be used to impersonate the MADAssistant repository app
     * and consume the logs meant for it
     *
     * @param repositorySignature The repository signature
     * @param serviceSignatures The list of signatures retrieved from the bound service
     */
    private fun validateRepositorySignature(
        servicePackageName: String?,
        repositorySignature: String,
        serviceSignatures: List<String>
    ): String? {
        return when {
            servicePackageName.isNullOrBlank() -> "Invalid package name"
            repositorySignature.isBlank() -> null
            serviceSignatures.any { it.equals(repositorySignature, ignoreCase = true) } -> null
            else -> "Invalid repository signature"
        }
    }

    override fun disconnect(code: Int, message: String, hasPendingLogs: Boolean) {
        when {
            hasPendingLogs && currentState == ConnectionManager.State.Connected -> {
                setConnectionState(ConnectionManager.State.Disconnecting)
            }
            else -> {
                setConnectionState(ConnectionManager.State.Disconnected)
                repositoryServiceAIDL?.disconnect(code, message)
                unbindService()
                callback?.onDisconnected(code, message)
            }
        }
    }

    private fun unbindService() {
        logger?.i(TAG, "Unbinding service")
        setConnectionState(ConnectionManager.State.Disconnected)
        applicationContext.unbindService(this)
    }

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
        logger?.i(TAG, "Service disconnected")
        setConnectionState(ConnectionManager.State.Disconnected)
        repositoryServiceAIDL = null
    }

    // region Handshake
    /**
     * Performs the handshake with the repository
     */
    private fun initHandshake() {
        try {
            logger?.i(TAG, "initialising handshake...")
            repositoryServiceAIDL?.initiateHandshake(
                MADAssistantConstants.AIDLVersion,
                this
            )
        } catch (ex: Exception) {
            logger?.e(ex)
        }
    }

    /**
     * Async callback for the repository to return the handshake response
     * @param response Instance of [HandshakeResponseModel]
     */
    override fun onHandshakeResponse(response: HandshakeResponseModel?) {
        val errorMessage: String? = when {
            response?.successful == true -> permissionManager.setAuthToken(
                string = response.authToken,
                deviceIdentifier = response.deviceIdentifier
            )

            response?.errorMessage?.isNotBlank() == true -> response.errorMessage

            else -> "Unknown"
        }

        when (errorMessage) {
            null -> setConnectionState(ConnectionManager.State.Connected)
            else -> disconnect(code = 401, message = "AuthToken Failed")
        }
    }
    // endregion Handshake

    // region Session Management
    override fun startSession(sessionId: Long) {
        if (currentState == ConnectionManager.State.Connected) {
            repositoryServiceAIDL?.startSession(sessionId)
        }
    }

    override fun endSession(sessionId: Long) {
        repositoryServiceAIDL?.endSession(sessionId)
    }
    // endregion Session Management

    // region Logging
    /**
     * Send a log to the repository
     */
    override fun transmit(segment: TransmissionModel) {
        repositoryServiceAIDL?.let {
            try {
                it.log(segment)
            } catch (ex: Exception) {
                logger?.e(ex)
            }
        }
    }
    // endregion Logging

    // region Utils
    /**
     * Retrieve a list of signatures (in SHA-256) for a package
     */
    private fun getSignatureList(
        applicationContext: Context,
        packageName: String?
    ): List<String> {

        return if (packageName.isNullOrBlank()) {
            emptyList()
        } else {
            // Retrieve Package Information
            val flags = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> PackageManager.GET_SIGNING_CERTIFICATES
                else -> PackageManager.GET_SIGNATURES
            }
            val pkgInfo = applicationContext.packageManager.getPackageInfo(
                packageName,
                flags
            )

            // Retrieve Signatures
            val signatures = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                pkgInfo.signatures
            } else when {
                pkgInfo.signingInfo.hasMultipleSigners() -> pkgInfo.signingInfo.apkContentsSigners
                else -> pkgInfo.signingInfo.signingCertificateHistory
            }

            signatures.map { signature ->
                try {
                    val md = MessageDigest.getInstance("SHA256")
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

                    toRet.toString()
                } catch (ex: Exception) {
                    "Failed"
                }
            }
        }
    }
    // endregion Utils
}