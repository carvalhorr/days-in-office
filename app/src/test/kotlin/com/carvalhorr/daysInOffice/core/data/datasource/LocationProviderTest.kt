package com.carvalhorr.daysInOffice.core.data.datasource

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocationProviderTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockClient = mockk<FusedLocationProviderClient>()
    private lateinit var provider: LocationProvider

    @BeforeEach
    fun setup() {
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(mockContext) } returns mockClient
        provider = LocationProvider(mockContext)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    private fun buildTask(
        onSuccess: ((OnSuccessListener<Location>) -> Unit)? = null,
        onFailure: ((OnFailureListener) -> Unit)? = null
    ): Task<Location> {
        val task = mockk<Task<Location>>()
        every { task.addOnSuccessListener(any()) } answers {
            onSuccess?.invoke(firstArg())
            task
        }
        every { task.addOnFailureListener(any()) } answers {
            onFailure?.invoke(firstArg())
            task
        }
        return task
    }

    @Test
    fun `given location available when getCurrentLocation then returns success`() = runTest {
        val mockLocation = mockk<Location>()
        val task = buildTask(onSuccess = { it.onSuccess(mockLocation) })
        every { mockClient.getCurrentLocation(any<Int>(), null) } returns task

        val result = provider.getCurrentLocation()

        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
    }

    @Test
    fun `given location null when getCurrentLocation then returns failure`() = runTest {
        val task = buildTask(onSuccess = { listener ->
            @Suppress("UNCHECKED_CAST")
            (listener as OnSuccessListener<Location?>).onSuccess(null)
        })
        every { mockClient.getCurrentLocation(any<Int>(), null) } returns task

        val result = provider.getCurrentLocation()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `given task fails when getCurrentLocation then returns failure`() = runTest {
        val exception = RuntimeException("Network error")
        val task = buildTask(onFailure = { it.onFailure(exception) })
        every { mockClient.getCurrentLocation(any<Int>(), null) } returns task

        val result = provider.getCurrentLocation()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `given security exception when getCurrentLocation then returns failure`() = runTest {
        every { mockClient.getCurrentLocation(any<Int>(), null) } throws SecurityException("Permission denied")

        val result = provider.getCurrentLocation()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
