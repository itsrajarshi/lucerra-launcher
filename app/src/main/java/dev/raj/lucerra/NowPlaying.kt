package dev.raj.lucerra

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

/** Follows the active media session (Spotify etc.) and reports what is on, or null when nothing is. */
class NowPlaying(ctx: Context, private val onChange: (State?) -> Unit) {
    class State(val title: String, val artist: String, val art: Bitmap?, val playing: Boolean)

    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val me = ComponentName(ctx, NowPlayingListener::class.java)
    private var ctl: MediaController? = null

    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = push()
        override fun onMetadataChanged(metadata: MediaMetadata?) = push()
        override fun onSessionDestroyed() = pick(msm.getActiveSessions(me))
    }
    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    fun start() {
        try {
            msm.addOnActiveSessionsChangedListener(sessions, me)
            pick(msm.getActiveSessions(me))
        } catch (e: SecurityException) {
            onChange(null) // notification access not granted yet
        }
    }

    fun stop() {
        try { msm.removeOnActiveSessionsChangedListener(sessions) } catch (e: Exception) { }
        ctl?.unregisterCallback(cb)
        ctl = null
    }

    fun previous() = ctl?.transportControls?.skipToPrevious()
    fun next() = ctl?.transportControls?.skipToNext()
    fun toggle() { if (isPlaying(ctl)) ctl?.transportControls?.pause() else ctl?.transportControls?.play() }

    private fun pick(list: List<MediaController>?) {
        val next = list?.firstOrNull { isPlaying(it) } ?: list?.firstOrNull()
        if (next?.sessionToken != ctl?.sessionToken) {
            ctl?.unregisterCallback(cb)
            ctl = next
            next?.registerCallback(cb)
        }
        push()
    }

    private fun isPlaying(c: MediaController?) =
        c?.playbackState?.state.let { it == PlaybackState.STATE_PLAYING || it == PlaybackState.STATE_BUFFERING }

    private fun push() {
        val c = ctl
        val st = c?.playbackState?.state
        if (c == null || st == null || st == PlaybackState.STATE_NONE || st == PlaybackState.STATE_STOPPED) {
            onChange(null); return
        }
        val m = c.metadata
        onChange(State(
            title = m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "",
            art = m?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: m?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
            playing = isPlaying(c),
        ))
    }
}
