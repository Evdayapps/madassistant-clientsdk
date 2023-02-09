package com.evdayapps.madassistant.testapp

import MainScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.MADAssistantClientImpl
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.testapp.screens.MainScreenViewModel

class TestActivity : AppCompatActivity() {

    private val TAG = "TestActivity"

    lateinit var madAssistantClient: MADAssistantClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise the instance
        initMADAssistant()

        // Set the view
        setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    primary = Color(0xff212121)
                )
            ) {
                MainScreen(MainScreenViewModel(madAssistantClient))
            }
        }
    }

    private fun initMADAssistant() {
        val logUtils = object : Logger {
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

        madAssistantClient = MADAssistantClientImpl(
            applicationContext = applicationContext,
            repositorySignature = "",
            passphrase = "test",
            logger = logUtils,
            callback = object : MADAssistantClient.Callback {
                override fun onSessionStarted(sessionId: Long) {
                    Log.i("MADAssistant", "Session Started")
                }

                override fun onSessionEnded(sessionId: Long) {}

                override fun onConnectionStateChanged(state: ConnectionManager.State) {

                }

                override fun onDisconnected(code: Int, message: String) {}

            }
        )

        // Bind the client to the remote service
        madAssistantClient.connect()

        // Start a session
        madAssistantClient.startSession()

        madAssistantClient.logCrashes()
    }

}

