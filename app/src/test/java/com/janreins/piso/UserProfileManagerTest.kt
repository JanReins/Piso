package com.janreins.piso

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.janreins.piso.data.local.ThemeMode
import com.janreins.piso.data.local.UserProfileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserProfileManagerTest {

    private lateinit var manager: UserProfileManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = UserProfileManager(context)
        manager.clearProfile()
    }

    @Test
    fun testCreateProfileWithoutPin() {
        manager.createProfile("Maria", null)
        assertTrue(manager.hasProfile())
        assertFalse(manager.hasPin())
        assertEquals("Maria", manager.userProfile.value.displayName)
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun testCreateProfileWithPin() {
        manager.createProfile("Juan", "1234")
        assertTrue(manager.hasProfile())
        assertTrue(manager.hasPin())
        assertEquals("Juan", manager.userProfile.value.displayName)

        // Verify correct and incorrect PINs
        assertTrue(manager.verifyPin("1234"))
        assertFalse(manager.verifyPin("0000"))
        assertFalse(manager.verifyPin("123"))

        // Lock & Unlock
        manager.lock()
        assertTrue(manager.isLocked.value)

        val unlocked = manager.unlock("1234")
        assertTrue(unlocked)
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun testChangeAndRemovePin() {
        manager.createProfile("Juan", "1234")

        val wrongChange = manager.changePin("9999", "5678")
        assertFalse(wrongChange)
        assertTrue(manager.verifyPin("1234"))

        val rightChange = manager.changePin("1234", "5678")
        assertTrue(rightChange)
        assertFalse(manager.verifyPin("1234"))
        assertTrue(manager.verifyPin("5678"))

        val removed = manager.removePin("5678")
        assertTrue(removed)
        assertFalse(manager.hasPin())
    }

    @Test
    fun testThemeModePersistence() {
        manager.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, manager.userProfile.value.themeMode)
    }
}
