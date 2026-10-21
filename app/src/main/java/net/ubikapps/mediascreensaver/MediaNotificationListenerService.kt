package net.ubikapps.mediascreensaver

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationInfo(
    val packageName: String,
    val icon: Icon?,
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
        super.onListenerConnected()
        isConnected = true
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    private fun updateNotifications() {
        try {
            val active = activeNotifications ?: return
            val notificationGroups = active
                .filter { sbn ->
                    if (sbn == null) return@filter false
                    val notification = sbn.notification ?: return@filter false
                    val extras = notification.extras ?: return@filter false
                    val isMedia = try {
                        extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                    } catch (_: Throwable) {
                        false
                    }
                    !isMedia && !sbn.isOngoing
                }
                .groupBy { it.packageName }
                .mapNotNull { (packageName, sbns) ->
                    // Filter out group summaries if there are other notifications in the group
                    // to avoid over-counting (e.g. Gmail showing N+1 for N messages).
                    val children = sbns.filter { sbn ->
                        val flags = sbn.notification?.flags ?: 0
                        (flags and Notification.FLAG_GROUP_SUMMARY) == 0
                    }
                    val displayNotifications = if (children.isEmpty()) sbns else children

                    val firstIcon = displayNotifications.firstOrNull()?.notification?.smallIcon ?: return@mapNotNull null
                    NotificationInfo(
                        packageName = packageName,
                        icon = firstIcon,
                        count = displayNotifications.size
                    )
                }

            _notifications.value = notificationGroups
        } catch (_: Throwable) {
            // Service might not be connected yet or exception during parceling
        }
    }

    companion object {
        private const val TAG = "MediaNotifListener"

        var isConnected: Boolean = false
            private set

        private val _notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
        val notifications: StateFlow<List<NotificationInfo>> = _notifications.asStateFlow()

        fun ensureRebound(context: Context) {
            if (isConnected) return
            try {
                toggleComponent(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to rebind notification listener service", e)
            }
        }

        private fun toggleComponent(context: Context) {
            val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

