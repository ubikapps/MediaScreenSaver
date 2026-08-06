package net.ubikapps.mediascreensaver

import android.app.Notification
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onListenerConnected() {
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val activeNotifications = activeNotifications ?: return
            val icons = activeNotifications
                .filter { sbn ->
                    val isMedia = sbn.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                    !isMedia && !sbn.isOngoing
                }
                .mapNotNull { it.notification.smallIcon }
                .distinct() // Basic deduplication

            _notifications.value = icons
        } catch (e: Exception) {
            // Service might not be connected yet
        }
    }

    companion object {
        private val _notifications = MutableStateFlow<List<Icon>>(emptyList())
        val notifications: StateFlow<List<Icon>> = _notifications.asStateFlow()
    }
}

