import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evdayapps.madassistant.testapp.screens.main.MainScreenViewModel
import com.evdayapps.madassistant.testapp.screens.main.widgets.AbsSectionCardWidget
import com.evdayapps.madassistant.testapp.screens.main.widgets.CustomTextField

@Composable()
fun MainScreen(viewModel: MainScreenViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MADAssistant Demo Client") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                        label = "Destination",
                        onStrValueChange = {
                            updateConfig(viewModel.logsConfig.value.copy(message = it))
                        }
                    )
                },
                paramsTitle = "Parameters",
                paramsAddBtnLabel = "Add Parameter",
                paramsList = { it.parameters },
                updateParams = { config, params -> config.copy(parameters = params) },
                onUpdateConfig = { config -> viewModel.logsConfig.value = config },
                onRequestButtonClicked = { viewModel.testAnalytics() }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// region Api Call
