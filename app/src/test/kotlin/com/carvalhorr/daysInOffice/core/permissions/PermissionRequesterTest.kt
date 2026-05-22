package com.carvalhorr.daysInOffice.core.permissions

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RunWith(RobolectricTestRunner::class)
class PermissionRequesterTest {

    private fun makeRequester(
        context: android.content.Context = ApplicationProvider.getApplicationContext(),
        pendingResult: androidx.compose.runtime.MutableState<Continuation<Boolean>?> = mutableStateOf(null),
        onLaunch: (String) -> Unit = {}
    ): PermissionRequester = PermissionRequester(
        context = context,
        launchPermission = onLaunch,
        pendingResult = pendingResult,
        dialogRequest = mutableStateOf(null)
    )

    @Test
    @Config(sdk = [32])
    fun `given API 32 when requesting NOTIFICATIONS then returns Granted without launching`() = runTest {
        var launched = false
        val requester = makeRequester(onLaunch = { launched = true })

        val result = requester.request(AppPermission.NOTIFICATIONS)

        assertEquals(PermissionState.Granted, result)
        assertFalse(launched)
    }

    @Test
    @Config(sdk = [33])
    fun `given API 33 when requesting NOTIFICATIONS then does not short-circuit to Granted`() = runTest {
        var launched = false
        val pendingResult = mutableStateOf<Continuation<Boolean>?>(null)
        val requester = makeRequester(
            pendingResult = pendingResult,
            onLaunch = {
                launched = true
                pendingResult.value?.resume(true)
                pendingResult.value = null
            }
        )

        val result = requester.request(AppPermission.NOTIFICATIONS)

        assertTrue(launched)
        assertEquals(PermissionState.Granted, result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `given FINE_LOCATION not granted when requesting BACKGROUND_LOCATION then returns Denied without launching`() = runTest {
        var launched = false
        val requester = makeRequester(onLaunch = { launched = true })

        val result = requester.request(AppPermission.BACKGROUND_LOCATION)

        assertEquals(PermissionState.Denied, result)
        assertFalse(launched)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `given FINE_LOCATION granted when requesting BACKGROUND_LOCATION then proceeds to launch`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        var launched = false
        val pendingResult = mutableStateOf<Continuation<Boolean>?>(null)
        val requester = makeRequester(
            context = app,
            pendingResult = pendingResult,
            onLaunch = {
                launched = true
                pendingResult.value?.resume(true)
                pendingResult.value = null
            }
        )

        val result = requester.request(AppPermission.BACKGROUND_LOCATION)

        assertTrue(launched)
        assertEquals(PermissionState.Granted, result)
    }
}
