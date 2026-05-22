package com.carvalhorr.daysInOffice.app.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavigationTest {

    @Test
    fun `given Destination sealed class when listing routes then exactly four routes exist`() {
        val routes = listOf(
            Destination.Onboarding.route,
            Destination.Dashboard.route,
            Destination.Calendar.route,
            Destination.Settings.route
        )
        assertEquals(4, routes.distinct().size)
    }

    @Test
    fun `given Destination when checking routes then correct string values assigned`() {
        assertEquals("onboarding", Destination.Onboarding.route)
        assertEquals("dashboard", Destination.Dashboard.route)
        assertEquals("calendar", Destination.Calendar.route)
        assertEquals("settings", Destination.Settings.route)
    }

    @Test
    fun `given bottomNavDestinations when counting items then exactly three items`() {
        assertEquals(3, Destination.bottomNavDestinations.size)
    }

    @Test
    fun `given bottomNavDestinations when checking contents then Dashboard Calendar Settings present`() {
        val destinations = Destination.bottomNavDestinations
        assertTrue(destinations.contains(Destination.Dashboard))
        assertTrue(destinations.contains(Destination.Calendar))
        assertTrue(destinations.contains(Destination.Settings))
    }

    @Test
    fun `given bottomNavDestinations when checking Onboarding then it is absent`() {
        val routes = Destination.bottomNavDestinations.map { it.route }
        assertFalse(routes.contains(Destination.Onboarding.route))
    }

    @Test
    fun `given Dashboard destination when checking label then returns Home`() {
        assertEquals("Home", Destination.Dashboard.label)
    }

    @Test
    fun `given Calendar destination when checking label then returns Calendar`() {
        assertEquals("Calendar", Destination.Calendar.label)
    }

    @Test
    fun `given Settings destination when checking label then returns Settings`() {
        assertEquals("Settings", Destination.Settings.label)
    }

    @Test
    fun `given tab destinations when checking icons then icons are non-null`() {
        assertNotNull(Destination.Dashboard.icon)
        assertNotNull(Destination.Calendar.icon)
        assertNotNull(Destination.Settings.icon)
    }

    @Test
    fun `given bottomNavDestinations when checking routes then all routes are unique`() {
        val routes = Destination.bottomNavDestinations.map { it.route }
        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun `given all tab destinations when checking labels then all labels are non-blank`() {
        Destination.bottomNavDestinations.forEach { tab ->
            assertTrue(tab.label.isNotBlank(), "Tab '${tab.route}' has blank label")
        }
    }
}
