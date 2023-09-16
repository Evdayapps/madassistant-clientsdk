package com.evdayapps.madassistant.testapp.screens.main.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evdayapps.madassistant.testapp.screens.main.MainScreenViewModel
import com.evdayapps.madassistant.testapp.screens.main.Parameter
import com.evdayapps.madassistant.testapp.screens.main.ParameterType

@Preview
@Composable
private fun SectionCardWidgetTest() {
    AbsSectionCardWidget<MainScreenViewModel.NetworkCallConfig>(
        title = "Network Call",
        config = MainScreenViewModel.NetworkCallConfig(
            "https://api.github.com/users/google/repos",
            headers = listOf(
                Parameter(key = "content-type", value = "application-data/json", inEditing = false),
                Parameter(key = "password", value = "2423dfsf5$232", inEditing = false)
            )
        ),
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
        formFields = { config, _ ->
            CustomTextField(
                value = config.url,
                maxLines = 1,
                label = "Url",
                onStrValueChange = {}
            )
        },
        paramsTitle = "Headers",
        paramsAddBtnLabel = "Add Header",
        paramsList = { it.headers },
        updateParams = { config, _ -> config },
        onUpdateConfig = {},
        onRequestButtonClicked = {}
    )
}

@Composable
fun <ConfigType> AbsSectionCardWidget(
    title: String,
    config: ConfigType,
    headerBuilder: @Composable (data: ConfigType) -> Unit,
    formFields: @Composable (data: ConfigType, onUpdateConfig: (data: ConfigType) -> Unit) -> Unit,
    onRequestButtonClicked: () -> Unit,
    onUpdateConfig: (data: ConfigType) -> Unit,
    paramsTitle: String,
    paramsAddBtnLabel: String,
    paramsList: (data: ConfigType) -> List<Parameter<Any>>,
    updateParams: (config: ConfigType, params: List<Parameter<Any>>) -> ConfigType
) {
    val expanded = remember { mutableStateOf(false) }
    val rotateState = animateFloatAsState(targetValue = if (expanded.value) 180F else 0F)

    Card(
        modifier = Modifier.shadow(elevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f, true)) {
                    Text(title, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(4.dp))
                    headerBuilder(config)
                }
                Icon(
                    Icons.Default.ArrowDropDown, "",
                    modifier = Modifier
                        .rotate(rotateState.value)
                        .clickable { expanded.value = !expanded.value }
                )
            }
            AnimatedVisibility(visible = expanded.value) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    formFields(config, onUpdateConfig)
                    Spacer(modifier = Modifier.height(8.dp))
                    ParamsListWidget(
                        title = paramsTitle,
                        labelAddButton = paramsAddBtnLabel,
                        list = paramsList(config),
                        newItemBuilder = {
                            Parameter(type = ParameterType.String, value = "", inEditing = true)
                        },
                        onUpdate = { onUpdateConfig(updateParams(config, it)) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestButtonClicked
            ) {
                Text("Fire!")
            }
        }
    }
}

@Composable
fun ParamsListWidget(
    title: String = "Params",
    labelAddButton: String = "Add Parameter",
    list: List<Parameter<Any>>,
    newItemBuilder: () -> Parameter<Any>,
    onUpdate: (List<Parameter<Any>>) -> Unit = { }
) {
    Text(title, style = MaterialTheme.typography.h6)
    list.map { item ->
        ParamItemWidget(
            item,
            onDelete = { onUpdate(list.minus(it)) },
            onChanged = { onUpdate(list.map { iter -> if (iter == item) it else iter }) }
        )
    }
    TextButton(onClick = {
        val newItem: Parameter<Any> = newItemBuilder()
        onUpdate(list.plus(newItem))
    }) {
        Text(labelAddButton)
    }
}

@Preview
@Composable
fun ParamItemWidget(
    item: Parameter<Any> = Parameter(key = "Fake Key", value = "Fake Value"),
    onChanged: (Parameter<Any>) -> Unit = { _ -> },
    onDelete: (item: Parameter<Any>) -> Unit = { _ -> }
) {
    return Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            AnimatedVisibility(visible = !item.inEditing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier.clickable { onDelete(item) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.key, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(item.value.toString(), fontWeight = FontWeight.Light, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Item",
                        modifier = Modifier.clickable { onChanged(item.copy(inEditing = true)) }
                    )
                }
            }
            AnimatedVisibility(visible = item.inEditing) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = item.key,
                            label = "Key",
                            maxLines = 1,
                            onStrValueChange = { onChanged(item.copy(key = it)) },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CustomTextField(
                            value = item.value.toString(),
                            label = "Value",
                            maxLines = 1,
                            onStrValueChange = {
                                onChanged(item.copy(value = it))
                            },
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Edit Item",
                        tint = Color.Green,
                        modifier = Modifier.clickable {
                            onChanged(item.copy(inEditing = false))
                        }
                    )
                }
            }
        }
    }

}