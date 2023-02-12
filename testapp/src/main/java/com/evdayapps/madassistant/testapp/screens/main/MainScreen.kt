import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.connection.ConnectionManager
import com.evdayapps.madassistant.testapp.screens.main.MainScreenViewModel
import com.evdayapps.madassistant.testapp.screens.main.widgets.AbsSectionCardWidget
import com.evdayapps.madassistant.testapp.screens.main.widgets.CustomTextField
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    madAssistantClient: MADAssistantClient,
    logs: MutableState<List<Triple<String, String, String>>>,
    connectionState: MutableState<ConnectionManager.State>,
    sessionActive: MutableState<Boolean>,
) {
    val logsVisible = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MADAssistant Demo Client") },
                actions = {
                    IconButton(onClick = { logsVisible.value = !logsVisible.value }) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("Connection State", style = MaterialTheme.typography.h6)
                    Text(connectionState.value.name)
                }

                Button(
                    onClick = {
                        when (connectionState.value) {
                            ConnectionManager.State.None,
                            ConnectionManager.State.Disconnected -> madAssistantClient.connect()
                            ConnectionManager.State.Connected -> madAssistantClient.disconnect()
                            else -> {}
                        }
                    },
                    enabled = when (connectionState.value) {
                        ConnectionManager.State.None,
                        ConnectionManager.State.Connected,
                        ConnectionManager.State.Disconnected -> true
                        else -> false
                    }
                ) {
                    Text(
                        text = when (connectionState.value) {
                            ConnectionManager.State.None,
                            ConnectionManager.State.Disconnected -> "Connect"
                            ConnectionManager.State.Connected -> "Disconnect"
                            else -> ""
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("Session State", style = MaterialTheme.typography.h6)
                    Text(if (sessionActive.value) "Active" else "Inactive")
                }
                Button(
                    onClick = {
                        when {
                            sessionActive.value -> madAssistantClient.endSession()
                            else -> madAssistantClient.startSession()
                        }
                    },
                ) {
                    Text(
                        text = when {
                            sessionActive.value -> "End Session"
                            else -> "Start Session"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            AbsSectionCardWidget(
                title = "Network Call",
                config = viewModel.networkCallConfig.value,
                headerBuilder = { config ->
                    Text(
                        text = config.url,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (config.headers.isNotEmpty()) {
                        Text(
                            text = config.headers.joinToString(", ") {
                                "${it.key}:${it.value}"
                            },
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                        )
                    }
                },
                formFields = { config, updateConfig ->
                    CustomTextField(
                        value = config.url,
                        maxLines = 1,
                        label = "Url",
                        onStrValueChange = {
                            updateConfig(viewModel.networkCallConfig.value.copy(url = it))
                        }
                    )
                },
                paramsTitle = "Headers",
                paramsAddBtnLabel = "Add Header",
                paramsList = { it.headers },
                updateParams = { config, params -> config.copy(headers = params) },
                onUpdateConfig = { config -> viewModel.networkCallConfig.value = config },
                onRequestButtonClicked = { viewModel.testApiCall() }
            )
            Spacer(Modifier.height(16.dp))
            AbsSectionCardWidget(
                title = "Analytics",
                config = viewModel.analyticsConfig.value,
                headerBuilder = { config ->
                    Text(
                        text = "${config.destination}:${config.eventName}",
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (config.parameters.isNotEmpty()) {
                        Text(
                            text = config.parameters.joinToString(", ") {
                                "${it.key}:${it.value}"
                            },
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                        )
                    }
                },
                formFields = { config, updateConfig ->
                    CustomTextField(
                        value = config.destination,
                        maxLines = 1,
                        label = "Destination",
                        onStrValueChange = {
                            updateConfig(viewModel.analyticsConfig.value.copy(destination = it))
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CustomTextField(
                        value = config.eventName,
                        maxLines = 1,
                        label = "Event Name",
                        onStrValueChange = {
                            updateConfig(viewModel.analyticsConfig.value.copy(eventName = it))
                        }
                    )
                },
                paramsTitle = "Parameters",
                paramsAddBtnLabel = "Add Parameter",
                paramsList = { it.parameters },
                updateParams = { config, params -> config.copy(parameters = params) },
                onUpdateConfig = { config -> viewModel.analyticsConfig.value = config },
                onRequestButtonClicked = { viewModel.testAnalytics() }
            )
            Spacer(Modifier.height(16.dp))
            AbsSectionCardWidget(
                title = "Log",
                config = viewModel.logsConfig.value,
                headerBuilder = { config ->
                    Text(
                        text = config.message,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (config.parameters.isNotEmpty()) {
                        Text(
                            text = config.parameters.joinToString(", ") {
                                "${it.key}:${it.value}"
                            },
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                        )
                    }
                },
                formFields = { config, updateConfig ->
                    CustomTextField(
                        value = config.message,
                        maxLines = 1,
                        label = "Message",
                        onStrValueChange = {
                            updateConfig(viewModel.logsConfig.value.copy(message = it))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        mainAxisSpacing = 16.dp,
                        crossAxisSpacing = 8.dp
                    ) {
                        listOf(Log.ERROR, Log.WARN, Log.DEBUG, Log.INFO, Log.VERBOSE).map {
                            Row(modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable {
                                    updateConfig(config.copy(type = it))
                                }
                            ) {
                                RadioButton(selected = config.type == it, onClick = null)
                                Text(
                                    text = when (it) {
                                        Log.ERROR -> "Error"
                                        Log.WARN -> "Warn"
                                        Log.DEBUG -> "Debug"
                                        Log.INFO -> "Info"
                                        else -> "Verbose"
                                    },
                                    modifier = Modifier.padding(start = 16.dp)
                                )

                            }
                        }
                    }
                },
                paramsTitle = "Parameters",
                paramsAddBtnLabel = "Add Parameter",
                paramsList = { it.parameters },
                updateParams = { config, params -> config.copy(parameters = params) },
                onUpdateConfig = { config -> viewModel.logsConfig.value = config },
                onRequestButtonClicked = { viewModel.testGenericLog() }
            )
            Spacer(Modifier.height(16.dp))
            AbsSectionCardWidget(
                title = "Exception",
                config = viewModel.exceptionConfig.value,
                headerBuilder = { config ->
                    Text(
                        text = config.message,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (config.data.isNotEmpty()) {
                        Text(
                            text = config.data.joinToString(", ") {
                                "${it.key}:${it.value}"
                            },
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                        )
                    }
                },
                formFields = { config, updateConfig ->
                    CustomTextField(
                        value = config.message,
                        maxLines = 1,
                        label = "Message",
                        onStrValueChange = {
                            updateConfig(viewModel.exceptionConfig.value.copy(message = it))
                        }
                    )
                },
                paramsTitle = "Data",
                paramsAddBtnLabel = "Add Data item",
                paramsList = { it.data },
                updateParams = { config, params -> config.copy(data = params) },
                onUpdateConfig = { config -> viewModel.exceptionConfig.value = config },
                onRequestButtonClicked = { viewModel.testNonFatalException() }
            )
            Spacer(Modifier.height(32.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFE91E63),
                    contentColor = Color.White
                ),
                onClick = { viewModel.testCrashReport() }
            ) {
                Text("Crash the App!")
            }
        }
        AnimatedVisibility(visible = logsVisible.value, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.background(MaterialTheme.colors.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
            ) {
                items(logs.value) {
                    Text("${it.first}\n${it.second}\n${it.third}")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

