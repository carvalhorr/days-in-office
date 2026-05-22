package com.carvalhorr.daysInOffice.feature.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.carvalhorr.daysInOffice.core.data.datasource.WifiScanner
import com.carvalhorr.daysInOffice.core.permissions.AppPermission
import com.carvalhorr.daysInOffice.core.permissions.PermissionState
import com.carvalhorr.daysInOffice.core.permissions.rememberPermissionRequester
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WifiSsidPickerViewModel @Inject constructor(
    private val wifiScanner: WifiScanner
) : ViewModel() {

    sealed interface ScanState {
        object Idle : ScanState
        object Scanning : ScanState
        data class Success(val ssids: List<String>) : ScanState
        data class Error(val message: String) : ScanState
    }

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun scan() {
        viewModelScope.launch {
            _state.value = ScanState.Scanning
            wifiScanner.scanForSsids()
                .onSuccess { ssids -> _state.value = ScanState.Success(ssids) }
                .onFailure { e -> _state.value = ScanState.Error(e.message ?: "Scan failed") }
        }
    }

    fun clearState() {
        _state.value = ScanState.Idle
    }
}

@Composable
fun WifiSsidPicker(
    currentSsid: String?,
    onUpdate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx: android.content.Context = context
        while (ctx !is ComponentActivity) {
            ctx = (ctx as android.content.ContextWrapper).baseContext
        }
        ctx as ComponentActivity
    }
    val viewModel: WifiSsidPickerViewModel = viewModel(viewModelStoreOwner = activity)
    val scanState by viewModel.state.collectAsStateWithLifecycle()
    var ssidText by remember { mutableStateOf(currentSsid ?: "") }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionRequester = rememberPermissionRequester()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (scanState is WifiSsidPickerViewModel.ScanState.Scanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        permissionDeniedMessage = null
                        viewModel.clearState()
                        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            AppPermission.NEARBY_WIFI_DEVICES
                        } else {
                            AppPermission.FINE_LOCATION
                        }
                        val result = permissionRequester.request(permissionToRequest)
                        if (result is PermissionState.Granted) {
                            viewModel.scan()
                        } else {
                            permissionDeniedMessage = "Permission denied. Enter SSID manually."
                        }
                    }
                },
                enabled = scanState !is WifiSsidPickerViewModel.ScanState.Scanning
            ) {
                Text("Scan for networks")
            }
        }

        when (val state = scanState) {
            is WifiSsidPickerViewModel.ScanState.Error -> Column {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { viewModel.scan() }) {
                    Text("Retry")
                }
            }
            is WifiSsidPickerViewModel.ScanState.Success -> {
                if (state.ssids.isEmpty()) {
                    Column {
                        Text(
                            text = "No networks detected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { viewModel.scan() }) {
                            Text("Retry")
                        }
                    }
                } else {
                    Text("Detected networks:", style = MaterialTheme.typography.labelMedium)
                    state.ssids.forEach { ssid ->
                        TextButton(
                            onClick = { ssidText = ssid; onUpdate(ssid) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(ssid, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            else -> {}
        }

        if (permissionDeniedMessage != null) {
            Text(
                text = permissionDeniedMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = ssidText,
            onValueChange = { ssidText = it; onUpdate(it) },
            label = { Text("Office Wi-Fi SSID") },
            placeholder = { Text("e.g. OfficeNetwork") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
