package net.ubikapps.mediascreensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationInfoTest {

    @Test
    fun notificationInfo_properties_areCorrect() {
        val info = NotificationInfo(
            packageName = "com.spotify.music",
            icon = null,
            count = 3
        )

        assertEquals("com.spotify.music", info.packageName)
        assertNull(info.icon)
        assertEquals(3, info.count)
    }

    @Test
    fun notificationInfo_copy_updatesProperties() {
        val original = NotificationInfo(
            packageName = "com.example.app",
            icon = null,
            count = 1
        )

        val updated = original.copy(count = 5)

        assertEquals("com.example.app", updated.packageName)
        assertEquals(5, updated.count)
    }

    @Test
    fun notificationInfo_equality_comparesFields() {
        val info1 = NotificationInfo("com.whatsapp", null, 2)
        val info2 = NotificationInfo("com.whatsapp", null, 2)
        val info3 = NotificationInfo("com.whatsapp", null, 4)

        assertEquals(info1, info2)
        assertNotEquals(info1, info3)
    }

    @Test
    fun notificationInfo_destructuring_worksCorrectly() {
        val info = NotificationInfo("com.google.android.youtube", null, 10)
        val (pkg, icon, count) = info

        assertEquals("com.google.android.youtube", pkg)
        assertNull(icon)
        assertEquals(10, count)
    }
}
