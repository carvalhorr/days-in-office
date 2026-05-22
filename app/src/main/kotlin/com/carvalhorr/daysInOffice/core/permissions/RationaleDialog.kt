package com.carvalhorr.daysInOffice.core.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun RationaleDialog(
    permission: AppPermission,
    isPermanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = {
            Text(
                if (isPermanentlyDenied) permission.deniedPermanentlyText
                else permission.rationaleText
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isPermanentlyDenied) {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                    onGrant()
                }
            ) {
                Text(if (isPermanentlyDenied) "Settings" else "Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}
