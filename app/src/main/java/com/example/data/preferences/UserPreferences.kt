package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("video_library_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _viewMode = MutableStateFlow(prefs.getString(KEY_VIEW_MODE, "grid") ?: "grid")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _skipIntervalSec = MutableStateFlow(prefs.getInt(KEY_SKIP_INTERVAL, 10))
    val skipIntervalSec: StateFlow<Int> = _skipIntervalSec.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f))
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _autoResume = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RESUME, true))
    val autoResume: StateFlow<Boolean> = _autoResume.asStateFlow()

    private val _gestureControls = MutableStateFlow(prefs.getBoolean(KEY_GESTURES, true))
    val gestureControls: StateFlow<Boolean> = _gestureControls.asStateFlow()

    private val _crawlerMaxDepth = MutableStateFlow(prefs.getInt(KEY_CRAWLER_DEPTH, 3))
    val crawlerMaxDepth: StateFlow<Int> = _crawlerMaxDepth.asStateFlow()

    private val _crawlerMaxVideos = MutableStateFlow(prefs.getInt(KEY_CRAWLER_MAX_VIDEOS, 100))
    val crawlerMaxVideos: StateFlow<Int> = _crawlerMaxVideos.asStateFlow()

    private val _crawlerDelayMs = MutableStateFlow(prefs.getLong(KEY_CRAWLER_DELAY, 250L))
    val crawlerDelayMs: StateFlow<Long> = _crawlerDelayMs.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setViewMode(mode: String) {
        prefs.edit().putString(KEY_VIEW_MODE, mode).apply()
        _viewMode.value = mode
    }

    fun setSkipInterval(seconds: Int) {
        prefs.edit().putInt(KEY_SKIP_INTERVAL, seconds).apply()
        _skipIntervalSec.value = seconds
    }

    fun setPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
        _playbackSpeed.value = speed
    }

    fun setAutoResume(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESUME, enabled).apply()
        _autoResume.value = enabled
    }

    fun setGestureControls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURES, enabled).apply()
        _gestureControls.value = enabled
    }

    fun setCrawlerSettings(depth: Int, maxVideos: Int, delayMs: Long) {
        prefs.edit()
            .putInt(KEY_CRAWLER_DEPTH, depth)
            .putInt(KEY_CRAWLER_MAX_VIDEOS, maxVideos)
            .putLong(KEY_CRAWLER_DELAY, delayMs)
            .apply()
        _crawlerMaxDepth.value = depth
        _crawlerMaxVideos.value = maxVideos
        _crawlerDelayMs.value = delayMs
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_SKIP_INTERVAL = "skip_interval"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_AUTO_RESUME = "auto_resume"
        private const val KEY_GESTURES = "gestures"
        private const val KEY_CRAWLER_DEPTH = "crawler_depth"
        private const val KEY_CRAWLER_MAX_VIDEOS = "crawler_max_videos"
        private const val KEY_CRAWLER_DELAY = "crawler_delay"
    }
}
