package com.janreins.piso

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.janreins.piso.data.local.ThemeMode
import com.janreins.piso.data.local.UserProfileManager
import com.janreins.piso.data.local.isWeakPin
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

    private lateinit var context: Context
    private lateinit var manager: UserProfileManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
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
        manager.createProfile("Juan", "9876")
        assertTrue(manager.hasProfile())
        assertTrue(manager.hasPin())
        assertEquals("Juan", manager.userProfile.value.displayName)

        // Verify correct and incorrect PINs
        assertTrue(manager.verifyPin("9876"))
        assertFalse(manager.verifyPin("0000"))
        assertFalse(manager.verifyPin("123"))

        // Lock & Unlock
        manager.lock()
        assertTrue(manager.isLocked.value)

        val unlocked = manager.unlock("9876")
        assertTrue(unlocked)
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun testWeakPinDetection() {
        assertTrue(isWeakPin("0000"))
        assertTrue(isWeakPin("1111"))
        assertTrue(isWeakPin("1234"))
        assertTrue(isWeakPin("1212"))

        assertFalse(isWeakPin("4829"))
        assertFalse(isWeakPin("7391"))
        assertFalse(isWeakPin("9999"))
    }

    @Test
    fun testLockoutAfterFiveFailedAttemptsAndPersistence() {
        manager.createProfile("Juan", "5432")
        assertEquals(0, manager.getLockoutRemainingSeconds())

        // 4 failed attempts: not locked out yet
        for (i in 1..4) {
            assertFalse(manager.unlock("0000"))
            assertEquals(0, manager.getLockoutRemainingSeconds())
        }

        // 5th failed attempt: triggers 15 second lockout
        assertFalse(manager.unlock("0000"))
        val remaining = manager.getLockoutRemainingSeconds()
        assertTrue("Expected remaining seconds between 1 and 15, got $remaining", remaining in 1..15)

        // Even correct PIN fails during lockout window
        assertFalse(manager.unlock("5432"))

        // Recreate manager (simulating activity rebuild / phone rotation)
        val managerRebuilt = UserProfileManager(context)
        val rebuiltRemaining = managerRebuilt.getLockoutRemainingSeconds()
        assertTrue("Expected rebuilt lockout to still be active", rebuiltRemaining in 1..15)
    }

    @Test
    fun testChangeAndRemovePin() {
        manager.createProfile("Juan", "4321")

        val wrongChange = manager.changePin("9999", "5678")
        assertFalse(wrongChange)
        assertTrue(manager.verifyPin("4321"))

        val rightChange = manager.changePin("4321", "5678")
        assertTrue(rightChange)
        assertFalse(manager.verifyPin("4321"))
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
