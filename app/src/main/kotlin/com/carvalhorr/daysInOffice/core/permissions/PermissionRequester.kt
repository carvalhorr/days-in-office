package com.carvalhorr.daysInOffice.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object DeniedPermanently : PermissionState()
}

internal sealed class InternalDialogRequest {
    data class ShowRationale(
        val permission: AppPermission,
        val onGrant: () -> Unit,
        val onDismiss: () -> Unit
    ) : InternalDialogRequest()

    data class ShowDeniedPermanently(
        val permission: AppPermission,
        val onSettings: () -> Unit,
        val onDismiss: () -> Unit
    ) : InternalDialogRequest()
}

class PermissionRequester internal constructor(
    private val context: Context,
    private val launchPermission: (String) -> Unit,
    private val pendingResult: MutableState<Continuation<Boolean>?>,
    internal val dialogRequest: MutableState<InternalDialogRequest?>
) {
    suspend fun request(permission: AppPermission): PermissionState {
        if (permission == AppPermission.NOTIFICATIONS && Build.VERSION.SDK_INT < 33) {
            return PermissionState.Granted
        }

        if (permission == AppPermission.BACKGROUND_LOCATION) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return PermissionState.Denied
            }
        }

        if (ContextCompat.checkSelfPermission(context, permission.manifestPermission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return PermissionState.Granted
        }

        val activity = context as? Activity

        if (activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.manifestPermission)
        ) {
            val shouldProceed = suspendCoroutine<Boolean> { cont ->
                dialogRequest.value = InternalDialogRequest.ShowRationale(
                    permission = permission,
                    onGrant = { dialogRequest.value = null; cont.resume(true) },
                    onDismiss = { dialogRequest.value = null; cont.resume(false) }
                )
            }
            if (!shouldProceed) return PermissionState.Denied
        }

        val granted = suspendCoroutine<Boolean> { cont ->
            pendingResult.value = cont
            launchPermission(permission.manifestPermission)
        }

        if (granted) return PermissionState.Granted

        if (activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.manifestPermission)
        ) {
            suspendCoroutine<Unit> { cont ->
                dialogRequest.value = InternalDialogRequest.ShowDeniedPermanently(
                    permission = permission,
                    onSettings = { dialogRequest.value = null; cont.resume(Unit) },
                    onDismiss = { dialogRequest.value = null; cont.resume(Unit) }
                )
            }
            return PermissionState.DeniedPermanently
        }

        return PermissionState.Denied
    }
}

@Composable
fun rememberPermissionRequester(): PermissionRequester {
    val context = LocalContext.current
    val pendingResult = remember { mutableStateOf<Continuation<Boolean>?>(null) }
    val dialogRequest = remember { mutableStateOf<InternalDialogRequest?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingResult.value?.resume(granted)
        pendingResult.value = null
    }

    val requester = remember(context) {
        PermissionRequester(
            context = context,
            launchPermission = { perm -> launcher.launch(perm) },
            pendingResult = pendingResult,
            dialogRequest = dialogRequest
        )
    }

    when (val dialog = dialogRequest.value) {
        is InternalDialogRequest.ShowRationale -> RationaleDialog(
            permission = dialog.permission,
            isPermanentlyDenied = false,
            onGrant = dialog.onGrant,
            onDismiss = dialog.onDismiss
        )
        is InternalDialogRequest.ShowDeniedPermanently -> RationaleDialog(
            permission = dialog.permission,
            isPermanentlyDenied = true,
            onGrant = dialog.onSettings,
            onDismiss = dialog.onDismiss
        )
        null -> Unit
    }

    return requester
}
