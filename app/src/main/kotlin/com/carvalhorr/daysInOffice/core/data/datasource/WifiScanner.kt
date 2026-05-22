package com.carvalhorr.daysInOffice.core.data.datasource

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class WifiScanner @Inject constructor(@ApplicationContext private val context: Context) {

    private val wifi by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    @SuppressLint("MissingPermission")
    suspend fun scanForSsids(): Result<List<String>> {
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        try { ctx.unregisterReceiver(this) } catch (_: Exception) {}
                        if (!cont.isCompleted) {
                            val ssids = wifi.scanResults
                                .mapNotNull { it.SSID?.takeIf { s -> s.isNotBlank() } }
                                .distinct()
                            cont.resume(Result.success(ssids))
                        }
                    }
                }

                context.registerReceiver(
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )

                try {
                    val started = wifi.startScan()
                    if (!started) {
                        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                        if (!cont.isCompleted) {
                            cont.resume(
                                Result.failure(IllegalStateException("Wi-Fi scan throttled. Try again in a moment."))
                            )
                        }
                    }
                } catch (e: SecurityException) {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                    if (!cont.isCompleted) {
                        cont.resume(Result.failure(SecurityException("Wi-Fi scan permission denied. Grant Nearby Wi-Fi Devices permission.")))
                    }
                }

                cont.invokeOnCancellation {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                }
            }
        } ?: Result.failure(IllegalStateException("Wi-Fi scan timed out. Try again later."))
    }
}
