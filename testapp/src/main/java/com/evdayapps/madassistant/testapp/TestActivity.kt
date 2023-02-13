package com.evdayapps.madassistant.testapp

import MainScreen
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.MADAssistantClientImpl
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.clientlib.utils.Logger
import com.evdayapps.madassistant.testapp.screens.main.MainScreenViewModel

class TestActivity : AppCompatActivity() {

    private val TAG = "TestActivity"

    private lateinit var madAssistantClient: MADAssistantClient

    val logs: MutableState<List<Triple<String, String, String>>> = mutableStateOf(listOf())
    val connectionState: MutableState<ConnectionManager.State> =
        mutableStateOf(ConnectionManager.State.None)
    val sessionActive = mutableStateOf(false)
    val disconnectReason = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise the instance
        initMADAssistant()

        // Set the view
        setContent {
            MaterialTheme(
                colors = darkColors()
            ) {
                MainScreen(
                    MainScreenViewModel(madAssistantClient),
                    madAssistantClient,
                    logs,
                    connectionState,
                    sessionActive,
                    disconnectReason
                )
            }
        }
    }

    private fun initMADAssistant() {
        val logUtils = object : Logger {
            override fun i(tag: String, message: String) {
                logs.value = logs.value.plus(Triple("INFO", tag, message))
            }

            override fun v(tag: String, message: String) {
                logs.value = logs.value.plus(Triple("VERBOSE", tag, message))
            }

            override fun d(tag: String, message: String) {
                logs.value = logs.value.plus(Triple("DEBUG", tag, message))
            }

            override fun w(tag: String, message: String) {
                logs.value = logs.value.plus(Triple("WARN", tag, message))
            }

            override fun e(throwable: Throwable) {
                logs.value = logs.value.plus(
                    Triple(
                        "ERROR",
                        throwable::class.java.simpleName,
                        throwable.message ?: ""
                    )
                )
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
                    sessionActive.value = true
                }

                override fun onSessionEnded(sessionId: Long) {
                    sessionActive.value = false
                }

                override fun onConnectionStateChanged(state: ConnectionManager.State) {
                    connectionState.value = state
                    if (state != ConnectionManager.State.Disconnected) {
                        disconnectReason.value = ""
                    }
                }

                override fun onDisconnected(code: Int, message: String) {
                    disconnectReason.value = "[$code] $message"
                    logs.value = logs.value.plus(
                        Triple("WARN", "Disconnected", "Code: $code\n$message")
                    )
                }

            }
        )

        madAssistantClient.logCrashes()
    }

}

