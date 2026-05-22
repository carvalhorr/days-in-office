package com.carvalhorr.daysInOffice.feature.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.carvalhorr.daysInOffice.core.data.datasource.LocationProvider
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
class GeofencePickerViewModel @Inject constructor(
    private val locationProvider: LocationProvider
) : ViewModel() {

    sealed interface LocationState {
        object Idle : LocationState
        object Loading : LocationState
        data class Success(val lat: Double, val lng: Double) : LocationState
        data class Error(val message: String) : LocationState
    }

    private val _state = MutableStateFlow<LocationState>(LocationState.Idle)
    val state: StateFlow<LocationState> = _state.asStateFlow()

    fun fetchLocation() {
        viewModelScope.launch {
            _state.value = LocationState.Loading
            locationProvider.getCurrentLocation()
                .onSuccess { loc -> _state.value = LocationState.Success(loc.latitude, loc.longitude) }
                .onFailure { e -> _state.value = LocationState.Error(e.message ?: "Location unavailable") }
        }
    }

    fun clearState() {
        _state.value = LocationState.Idle
    }
}

@Composable
fun GeofencePicker(
    latitude: Double?,
    longitude: Double?,
    radius: Float?,
    onUpdate: (lat: Double, lng: Double, radius: Float) -> Unit,
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
    val viewModel: GeofencePickerViewModel = viewModel(viewModelStoreOwner = activity)
    val locationState by viewModel.state.collectAsStateWithLifecycle()
    var latText by remember { mutableStateOf(latitude?.toString() ?: "") }
    var lngText by remember { mutableStateOf(longitude?.toString() ?: "") }
    var radiusText by remember { mutableStateOf(radius?.toString() ?: "100") }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var backgroundPermissionWarning by remember { mutableStateOf<String?>(null) }

    val permissionRequester = rememberPermissionRequester()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(locationState) {
        if (locationState is GeofencePickerViewModel.LocationState.Success) {
            val s = locationState as GeofencePickerViewModel.LocationState.Success
            latText = s.lat.toString()
            lngText = s.lng.toString()
            onUpdate(s.lat, s.lng, radiusText.toFloatOrNull() ?: 100f)
            permissionDeniedMessage = null
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Office Location", style = MaterialTheme.typography.titleSmall)

        Button(
            onClick = {
                coroutineScope.launch {
                    permissionDeniedMessage = null
                    backgroundPermissionWarning = null
                    viewModel.clearState()
                    val foregroundResult = permissionRequester.request(AppPermission.FINE_LOCATION)
                    if (foregroundResult is PermissionState.Granted) {
                        viewModel.fetchLocation()
                        val backgroundResult = permissionRequester.request(AppPermission.BACKGROUND_LOCATION)
                        if (backgroundResult !is PermissionState.Granted) {
                            backgroundPermissionWarning = "Background location denied. Geofence will only trigger while the app is open."
                        }
                    } else {
                        permissionDeniedMessage = "Location permission denied. Enter coordinates manually."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = locationState !is GeofencePickerViewModel.LocationState.Loading
        ) {
            if (locationState is GeofencePickerViewModel.LocationState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Use current location")
        }

        val errorMessage = (locationState as? GeofencePickerViewModel.LocationState.Error)?.message
            ?: permissionDeniedMessage
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (backgroundPermissionWarning != null) {
            Text(
                text = backgroundPermissionWarning!!,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { newVal ->
                    latText = newVal
                    val lat = newVal.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    val r = radiusText.toFloatOrNull() ?: 100f
                    if (lat != null && lng != null) onUpdate(lat, lng, r)
                },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { newVal ->
                    lngText = newVal
                    val lat = latText.toDoubleOrNull()
                    val lng = newVal.toDoubleOrNull()
                    val r = radiusText.toFloatOrNull() ?: 100f
                    if (lat != null && lng != null) onUpdate(lat, lng, r)
                },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = radiusText,
            onValueChange = { newVal ->
                radiusText = newVal
                val lat = latText.toDoubleOrNull()
                val lng = lngText.toDoubleOrNull()
                val r = newVal.toFloatOrNull() ?: 100f
                if (lat != null && lng != null) onUpdate(lat, lng, r)
            },
            label = { Text("Radius (meters)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
