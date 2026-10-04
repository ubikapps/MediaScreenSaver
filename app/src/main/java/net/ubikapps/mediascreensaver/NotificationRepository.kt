package net.ubikapps.mediascreensaver

import kotlinx.coroutines.flow.StateFlow

interface NotificationProvider {
    val notifications: StateFlow<List<NotificationInfo>>
}

class RealNotificationProvider : NotificationProvider {
    override val notifications: StateFlow<List<NotificationInfo>>
        get() = MediaNotificationListenerService.notifications
}

object NotificationRepository {
    private var provider: NotificationProvider? = null

    val notifications: StateFlow<List<NotificationInfo>>
        get() {
            var current = provider
            if (current == null) {
                current = RealNotificationProvider()
                provider = current
            }
            return current.notifications
        }
}
