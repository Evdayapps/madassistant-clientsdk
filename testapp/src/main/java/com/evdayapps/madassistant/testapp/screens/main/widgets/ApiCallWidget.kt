package com.evdayapps.madassistant.testapp.screens.main.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evdayapps.madassistant.testapp.screens.MainScreenViewModel

@Preview
@Composable
fun ApiCallWidget(
    config: MainScreenViewModel.NetworkCallConfig = MainScreenViewModel.NetworkCallConfig(
        "https://api.github.com/users/google/repos",
        headers = listOf(
            Triple("content-type", "application-data/json", false)
        )
    ),
    onRequestButtonClicked: () -> Unit = {},
    onUpdateConfig: (url: String, headers: List<Triple<String, String, Boolean>>) -> Unit = { _, _ -> }
) {
    val expanded = remember { mutableStateOf(true) }
    val rotateState = animateFloatAsState(targetValue = if (expanded.value) 180F else 0F)

    Card(
        modifier = Modifier.shadow(elevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
            Row {
                Column(
                    modifier = Modifier.weight(1f, true)
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
                            text = config.headers.joinToString(", ") {
                                "${it.first}:${it.second}"
                            },
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                        )
                    }
                }
                Icon(
                    Icons.Default.ArrowDropDown, "",
                    modifier = Modifier
                        .rotate(rotateState.value)
                        .clickable {
                            expanded.value = !expanded.value
                        }
                )
            }
            AnimatedVisibility(visible = expanded.value) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.url,
                        singleLine = true,
                        label = { Text("Url") },
                        onValueChange = { onUpdateConfig(it, config.headers) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Headers",
                        style = MaterialTheme.typography.h6
                    )
                    config.headers.map {
                        ConfigItemWidget(it, onDelete = { item ->
                            onUpdateConfig(config.url, config.headers.minus(item))
                        })
                    }
                    TextButton(onClick = {
                        onUpdateConfig(config.url, config.headers.plus(Triple("", "", true)))
                    }) {
                        Text("Add Header")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
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

@Preview
@Composable
fun ConfigItemWidget(
    item: Triple<String, String, Boolean> = Triple("Test Key", "Test Value", false),
    onDelete: (item: Triple<String, String, Boolean>) -> Unit = { _ -> }
) {
    val inEditMode = remember { mutableStateOf(item.third) }
    val textFieldKey = remember { mutableStateOf(TextFieldValue(item.first)) }
    val textFieldValue = remember { mutableStateOf(TextFieldValue(item.second)) }

    return Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            AnimatedVisibility(visible = !inEditMode.value) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.first, fontWeight = FontWeight.Medium)
                        Text(item.second, fontWeight = FontWeight.Light)
                    }
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = Color.Red,
                        modifier = Modifier.clickable { onDelete(item) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Item",
                        modifier = Modifier.clickable { inEditMode.value = true }
                    )
                }
            }
            AnimatedVisibility(visible = inEditMode.value) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = textFieldKey.value,
                            label = { Text("Key") },
                            onValueChange = { textFieldKey.value = it }
                        )
                        OutlinedTextField(
                            value = textFieldValue.value,
                            label = { Text("Value") },
                            onValueChange = {
                                textFieldValue.value = it
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Edit Item",
                        modifier = Modifier.clickable {
                            inEditMode.value = false
                        }
                    )
                }
            }
        }
    }

}