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
        manager.createProfile("Juan", "987654")
        assertTrue(manager.hasProfile())
        assertTrue(manager.hasPin())
        assertEquals("Juan", manager.userProfile.value.displayName)

        // Verify correct and incorrect PINs
        assertTrue(manager.verifyPin("987654"))
        assertFalse(manager.verifyPin("000000"))
        assertFalse(manager.verifyPin("123"))

        // Lock & Unlock
        manager.lock()
        assertTrue(manager.isLocked.value)

        val unlocked = manager.unlock("987654")
        assertTrue(unlocked)
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun testWeakPinDetection() {
        assertTrue(isWeakPin("000000"))
        assertTrue(isWeakPin("111111"))
        assertTrue(isWeakPin("123456"))
        assertTrue(isWeakPin("121212"))
        assertTrue(isWeakPin("654321"))

        assertFalse(isWeakPin("482910"))
        assertFalse(isWeakPin("739182"))
        assertFalse(isWeakPin("999999"))
    }

    @Test
    fun testLockoutAfterFiveFailedAttemptsAndPersistence() {
        manager.createProfile("Juan", "654987")
        assertEquals(0, manager.getLockoutRemainingSeconds())

        // 4 failed attempts: not locked out yet
        for (i in 1..4) {
            assertFalse(manager.unlock("000000"))
            assertEquals(0, manager.getLockoutRemainingSeconds())
        }

        // 5th failed attempt: triggers 30 second lockout
        assertFalse(manager.unlock("000000"))
        val remaining = manager.getLockoutRemainingSeconds()
        assertTrue("Expected remaining seconds between 1 and 30, got $remaining", remaining in 1..30)

        // Even correct PIN fails during lockout window
        assertFalse(manager.unlock("654987"))

        // Recreate manager (simulating activity rebuild / phone rotation)
        val managerRebuilt = UserProfileManager(context)
        val rebuiltRemaining = managerRebuilt.getLockoutRemainingSeconds()
        assertTrue("Expected rebuilt lockout to still be active", rebuiltRemaining in 1..30)
    }

    @Test
    fun testChangeAndRemovePin() {
        manager.createProfile("Juan", "654322")

        val wrongChange = manager.changePin("999999", "567890")
        assertFalse(wrongChange)
        assertTrue(manager.verifyPin("654322"))

        val rightChange = manager.changePin("654322", "567890")
        assertTrue(rightChange)
        assertFalse(manager.verifyPin("654322"))
        assertTrue(manager.verifyPin("567890"))

        val removed = manager.removePin("567890")
        assertTrue(removed)
        assertFalse(manager.hasPin())
    }

    @Test
    fun testThemeModePersistence() {
        manager.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, manager.userProfile.value.themeMode)
    }
}
