package net.ubikapps.mediascreensaver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRepositoryTest {

    private class FakeNotificationProvider(
        override val notifications: StateFlow<List<NotificationInfo>>
    ) : NotificationProvider

    @Test
    fun fakeNotificationProvider_emitsNotifications() {
        val initialList = listOf(
            NotificationInfo("com.spotify.music", null, 2),
            NotificationInfo("com.whatsapp", null, 5)
        )
        val stateFlow = MutableStateFlow(initialList)
        val provider = FakeNotificationProvider(stateFlow)

        assertEquals(2, provider.notifications.value.size)
        assertEquals("com.spotify.music", provider.notifications.value[0].packageName)
        assertEquals(2, provider.notifications.value[0].count)
        assertEquals("com.whatsapp", provider.notifications.value[1].packageName)
        assertEquals(5, provider.notifications.value[1].count)
    }

    @Test
    fun fakeNotificationProvider_updatesStateFlow() {
        val stateFlow = MutableStateFlow<List<NotificationInfo>>(emptyList())
        val provider = FakeNotificationProvider(stateFlow)

        assertTrue(provider.notifications.value.isEmpty())

        val updatedList = listOf(
            NotificationInfo("com.google.android.talk", null, 1)
        )
        stateFlow.value = updatedList

        assertEquals(1, provider.notifications.value.size)
        assertEquals("com.google.android.talk", provider.notifications.value.first().packageName)
    }

    @Test
    fun realNotificationProvider_delegatesToServiceNotifications() {
        val realProvider = RealNotificationProvider()
        assertEquals(MediaNotificationListenerService.notifications.value, realProvider.notifications.value)
    }

    @Test
    fun notificationRepository_returnsNotificationsStateFlow() {
        val flow = NotificationRepository.notifications
        assertNotNull(flow)
        assertEquals(MediaNotificationListenerService.notifications.value, flow.value)
    }
}
