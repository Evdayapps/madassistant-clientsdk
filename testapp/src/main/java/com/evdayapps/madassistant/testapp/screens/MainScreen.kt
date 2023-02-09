import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evdayapps.madassistant.testapp.screens.MainScreenViewModel
import com.evdayapps.madassistant.testapp.screens.main.widgets.ApiCallWidget

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
        ) {
            ApiCallWidget(
                viewModel.networkCallConfig.value,
                onRequestButtonClicked = viewModel::testApiCall,
                onSettingsButtonClicked = {}
            )
        }
    }
}

// region Api Call
