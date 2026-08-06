package net.ubikapps.mediascreensaver

import android.app.Notification
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationInfo(
    val packageName: String,
    val icon: Icon,
    val count: Int
)

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
            val notificationGroups = activeNotifications
                .filter { sbn ->
                    val isMedia = sbn.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                    !isMedia && !sbn.isOngoing
                }
                .groupBy { it.packageName }
                .mapNotNull { (packageName, sbns) ->
                    val firstIcon = sbns.firstOrNull()?.notification?.smallIcon ?: return@mapNotNull null
                    NotificationInfo(
                        packageName = packageName,
                        icon = firstIcon,
                        count = sbns.size
                    )
                }

            _notifications.value = notificationGroups
        } catch (e: Exception) {
            // Service might not be connected yet
        }
    }

    companion object {
        private val _notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
        val notifications: StateFlow<List<NotificationInfo>> = _notifications.asStateFlow()
    }
}

