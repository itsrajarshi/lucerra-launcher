package dev.raj.lucerra

import android.service.notification.NotificationListenerService

/** Reads nothing. Holding notification access is what lets Android show us other apps' media sessions. */
class NowPlayingListener : NotificationListenerService()
