package com.evdayapps.madassistant.testapp.screens.main.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evdayapps.madassistant.testapp.screens.MainScreenViewModel

@Preview()
@Composable()
fun ApiCallWidget(
    config: MainScreenViewModel.NetworkCallConfig = MainScreenViewModel.NetworkCallConfig(
        "https://api.github.com/users/google/repos",
        headers = mapOf(
            Pair("content-type", "application-data/json")
        )
    ),
    onRequestButtonClicked: () -> Unit = {},
    onSettingsButtonClicked: () -> Unit = {},
) {
    Card(
        modifier = Modifier.shadow(elevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f, true)
                ) {
                    Text("Network Call", style = MaterialTheme.typography.h6)
                    Text(
                        text = config.url,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (config.headers.isNotEmpty()) {
                        Text(
                            text = config.headers.map { "${it.key}:${it.value}" }
                                .joinToString(", "),
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Api Call Settings",
                    modifier = Modifier.clickable {

                    }
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestButtonClicked
            ) {
                Text("Make Request")
            }
        }
    }
}