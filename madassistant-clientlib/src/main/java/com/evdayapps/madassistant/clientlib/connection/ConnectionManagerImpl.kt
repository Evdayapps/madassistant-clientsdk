package com.evdayapps.madassistant.clientlib.connection

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.evdayapps.madassistant.clientlib.connection.utils.ConnectionManagerUtils
import com.evdayapps.madassistant.clientlib.constants.ConnectionState
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.BuildConfig
import com.evdayapps.madassistant.common.MADAssistantClientAIDL
import com.evdayapps.madassistant.common.MADAssistantConstants
import com.evdayapps.madassistant.common.MADAssistantRepositoryAIDL
import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel
import com.evdayapps.madassistant.common.transmission.TransmissionModel

class ConnectionManagerImpl(
    private val applicationContext: Context,
    private val logUtils: LogUtils? = null,
    private val repositorySignature : String = "1B:C0:79:26:82:9E:FB:96:5C:6A:51:6C:96:7C:52:88:42:7E:" +
            "73:8C:05:7D:60:D8:13:9D:C4:3C:18:3B:E3:63"
) : ConnectionManager, ServiceConnection {

    companion object {
        const val REPO_SERVICE_PACKAGE = "com.evdayapps.madassistant.repository"
        const val REPO_SERVICE_CLASS = "$REPO_SERVICE_PACKAGE.service.MADAssistantService"
        const val TAG = "ConnectionManagerImpl"
    }

    private var callback: ConnectionManager.Callback? = null
    private var repositoryServiceAIDL: MADAssistantRepositoryAIDL? = null

    private val clientAIDL : MADAssistantClientAIDL = object : MADAssistantClientAIDL.Stub() {
        override fun onHandshakeResponse(data: HandshakeResponseModel?) {
            callback?.validateHandshakeReponse(data)
        }
    }

    override fun setCallback(callback: ConnectionManager.Callback) {
        this.callback = callback
    }

    /**
     * Attempt a connection to the repository service
     */
    override fun bindToService() {
        if (BuildConfig.DEBUG) {
            logUtils?.i(
                TAG, "bindToService: $REPO_SERVICE_CLASS package: $REPO_SERVICE_PACKAGE"
            )
        }

        callback?.onStateChanged(ConnectionState.Connecting)

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

        val success = applicationContext.bindService(
            intent,
            this,
            Service.BIND_AUTO_CREATE
        )

        logUtils?.i(TAG, "bindToService: Successful? $success")
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
        val errorMessage : String? = when {
            pkgName.isNullOrBlank() -> "Invalid package name"

            repositorySignature.isNotBlank() && !
            ConnectionManagerUtils.isServiceLegit(
                applicationContext, repositorySignature, pkgName
            ) -> "Invalid repository signature"

            else -> null
        }

        if(errorMessage == null) {
            logUtils?.i(TAG, "Repository valid. Initiating handshake..")
            repositoryServiceAIDL = MADAssistantRepositoryAIDL.Stub.asInterface(service)
            initHandshake()
        } else {
            logUtils?.i(TAG, "Repository invalid. Disconnecting.")
            unbindService()
        }
    }

    override fun disconnect(reason: Int) {
        repositoryServiceAIDL?.disconnect(reason)
    }

    override fun unbindService() {
        callback?.onStateChanged(ConnectionState.Disconnected)
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
        logUtils?.i(TAG, "Service disconnected")
        repositoryServiceAIDL = null
        callback?.onStateChanged(ConnectionState.Disconnected)
    }

    /**
     * Performs the handshake with the repository
     */
    private fun initHandshake() {
        try {
            logUtils?.i(TAG, "initialising handshake...")
            repositoryServiceAIDL?.initiateHandshake(
                MADAssistantConstants.LibraryVersion,
                clientAIDL
            )
        } catch (ex: Exception) {
            logUtils?.e(ex)
            callback?.validateHandshakeReponse(null)
        }
    }

    // region Session Management
    override fun startSession(): Long {
        logUtils?.i(TAG, "Starting new session")

        val sessionId: Long = repositoryServiceAIDL?.startSession()!!
        repositoryServiceAIDL?.updateChangelog(false, sessionId)

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
}