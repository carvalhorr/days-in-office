package com.carvalhorr.daysInOffice.core.detection

import android.content.Context
import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.carvalhorr.daysInOffice.core.detection.detector.GeofenceDetector
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.google.android.gms.location.GeofencingClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class GeofenceDetectorTest {

    private val mockContext = mockk<Context>()
    private val mockPreferencesDataSource = mockk<PreferencesDataSource>(relaxed = true)

    private fun makeConfig(
        lat: Double? = 37.4220,
        lng: Double? = -122.0841,
        radius: Float? = 200f,
        methods: Set<DetectionMethod> = setOf(DetectionMethod.GEOFENCE)
    ) = DetectionConfig(
        enabledMethods = methods,
        wifiSsid = null,
        geofenceLatitude = lat,
        geofenceLongitude = lng,
        geofenceRadiusMeters = radius
    )

    private fun setupPreferencesFlows(
        methods: Set<DetectionMethod> = setOf(DetectionMethod.GEOFENCE),
        ssid: String? = null,
        lat: Double? = 37.4220,
        lng: Double? = -122.0841,
        radius: Float? = 200f
    ) {
        every { mockPreferencesDataSource.detectionMethods } returns flowOf(methods)
        every { mockPreferencesDataSource.wifiSsid } returns flowOf(ssid)
        every { mockPreferencesDataSource.geofenceLat } returns flowOf(lat)
        every { mockPreferencesDataSource.geofenceLng } returns flowOf(lng)
        every { mockPreferencesDataSource.geofenceRadius } returns flowOf(radius)
    }

    @Test
    fun `given geofence_inside=true in DataStore when isAtOffice then returns true`() = runTest {
        every { mockPreferencesDataSource.geofenceInside } returns flowOf(true)
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertTrue(detector.isAtOffice(makeConfig()))
    }

    @Test
    fun `given geofence_inside=false in DataStore when isAtOffice then returns false`() = runTest {
        every { mockPreferencesDataSource.geofenceInside } returns flowOf(false)
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertFalse(detector.isAtOffice(makeConfig()))
    }

    @Test
    fun `given geofence_inside not set in DataStore when isAtOffice then returns false`() = runTest {
        // PreferencesDataSource defaults geofenceInside to false when key absent
        every { mockPreferencesDataSource.geofenceInside } returns flowOf(false)
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertFalse(detector.isAtOffice(makeConfig()))
    }

    @Test
    fun `given config missing lat_lng when isAvailable then returns false`() = runTest {
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertFalse(detector.isAvailable(mockContext, makeConfig(lat = null, lng = null)))
    }

    @Test
    fun `given config with lat_lng_radius when isAvailable then returns true`() = runTest {
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertTrue(detector.isAvailable(mockContext, makeConfig()))
    }

    @Test
    fun `isAvailable_readsPassedConfig_notConstructorSnapshot`() = runTest {
        // Construct detector with NO pre-loaded config. isAvailable must reflect the passed-in config.
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        val configWithCoords = makeConfig(lat = 1.0, lng = 2.0, radius = 100f)
        val configWithoutCoords = makeConfig(lat = null, lng = null, radius = null)
        assertTrue(detector.isAvailable(mockContext, configWithCoords))
        assertFalse(detector.isAvailable(mockContext, configWithoutCoords))
    }

    @Test
    fun `given geofence enabled in config when shouldActivate then returns true`() = runTest {
        setupPreferencesFlows(methods = setOf(DetectionMethod.GEOFENCE))
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertTrue(detector.shouldActivate())
    }

    @Test
    fun `given geofence not in enabledMethods when shouldActivate then returns false`() = runTest {
        setupPreferencesFlows(methods = setOf(DetectionMethod.WIFI_CONNECTED))
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertFalse(detector.shouldActivate())
    }

    @Test
    fun `given geofence enabled but coords missing when shouldActivate then returns false`() = runTest {
        setupPreferencesFlows(methods = setOf(DetectionMethod.GEOFENCE), lat = null)
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource)
        assertFalse(detector.shouldActivate())
    }

    @Test
    fun `when removeGeofence called then geofencingClient removeGeofences is invoked`() {
        val mockGeofencingClient = mockk<GeofencingClient>(relaxed = true)
        val detector = GeofenceDetector(mockContext, mockPreferencesDataSource, geofencingClientOverride = mockGeofencingClient)
        detector.removeGeofence()
        verify { mockGeofencingClient.removeGeofences(listOf("OFFICE_GEOFENCE")) }
    }
}
