package com.lunarvr.music

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MusicManager(private val context: Context) {

    private val PREFS_NAME = "lunar_music_prefs"
    private val KEY_TRACKS = "saved_tracks"

    val tracks = mutableListOf<MusicTrack>()
    private var mediaPlayer: MediaPlayer? = null
    var currentTrack: MusicTrack? = null
    var isPlaying: Boolean = false

    var onPlaybackChanged: (() -> Unit)? = null

    init {
        loadSavedTracks()
    }

    private fun loadSavedTracks() {
        tracks.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_TRACKS, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val path = obj.getString("filePath")
                val file = File(path)
                if (file.exists()) {
                    tracks.add(MusicTrack(id, name, path))
                }
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Error loading saved tracks", e)
        }
    }

    private fun saveTracks() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (t in tracks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("filePath", t.filePath)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TRACKS, arr.toString()).apply()
    }

    fun addTrackFromUri(uri: Uri, defaultName: String): MusicTrack? {
        try {
            val musicDir = File(context.filesDir, "lunar_music")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }
            val trackId = "track_${System.currentTimeMillis()}"
            val destFile = File(musicDir, "$trackId.mp3")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val track = MusicTrack(trackId, defaultName, destFile.absolutePath)
            tracks.add(track)
            saveTracks()
            return track
        } catch (e: Exception) {
            Log.e("MusicManager", "Error saving track from uri", e)
            return null
        }
    }

    fun renameTrack(trackId: String, newName: String) {
        val track = tracks.find { it.id == trackId } ?: return
        track.name = newName.trim().ifEmpty { "Minha Música" }
        saveTracks()
    }

    fun playTrack(track: MusicTrack) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(track.filePath)
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener {
                this.isPlaying = false
                this.onPlaybackChanged?.invoke()
            }
            mediaPlayer = mp
            currentTrack = track
            isPlaying = true
            onPlaybackChanged?.invoke()
        } catch (e: Exception) {
            Log.e("MusicManager", "Error playing track", e)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: run {
            if (tracks.isNotEmpty()) {
                playTrack(tracks[0])
            }
            return
        }

        try {
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
            onPlaybackChanged?.invoke()
        } catch (e: Exception) {
            Log.e("MusicManager", "Error toggling play/pause", e)
        }
    }

    fun seekForward10s() {
        val mp = mediaPlayer ?: return
        try {
            val currentPos = mp.currentPosition
            val newPos = (currentPos + 10000).coerceAtMost(mp.duration)
            mp.seekTo(newPos)
            onPlaybackChanged?.invoke()
        } catch (e: Exception) {}
    }

    fun seekBackward10s() {
        val mp = mediaPlayer ?: return
        try {
            val currentPos = mp.currentPosition
            val newPos = (currentPos - 10000).coerceAtLeast(0)
            mp.seekTo(newPos)
            onPlaybackChanged?.invoke()
        } catch (e: Exception) {}
    }

    fun getCurrentPositionMs(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (_: Exception) { 0 }
    }

    fun getDurationMs(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (_: Exception) { 0 }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        } catch (_: Exception) {}
    }
}
