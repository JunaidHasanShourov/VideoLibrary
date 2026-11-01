package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import android.util.TypedValue
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.network.NetworkClient
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.PlayerView
import com.example.VideoLibraryApp
import com.example.ui.components.formatDuration
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

data class VideoTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean,
    val width: Int,
    val height: Int,
    val bitrate: Int
)

enum class VideoScaleMode(val label: String, val resizeMode: Int) {
    FIT("Fit Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom / Fill", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

enum class SubtitleSize(val label: String, val sizeSp: Float) {
    SMALL("Small", 14f),
    MEDIUM("Normal", 18f),
    LARGE("Large", 24f),
    EXTRA_LARGE("Huge", 30f)
}

fun formatVideoTrackLabel(format: androidx.media3.common.Format, index: Int): String {
    val res = when {
        format.height >= 2160 -> "4K UHD (${format.width}x${format.height})"
        format.height >= 1440 -> "1440p QHD (${format.width}x${format.height})"
        format.height >= 1080 -> "1080p FHD (${format.width}x${format.height})"
        format.height >= 720 -> "720p HD (${format.width}x${format.height})"
        format.height >= 480 -> "480p SD (${format.width}x${format.height})"
        format.height >= 360 -> "360p (${format.width}x${format.height})"
        format.height > 0 -> "${format.height}p (${format.width}x${format.height})"
        !format.label.isNullOrBlank() -> format.label!!
        else -> "Track ${index + 1}"
    }

    val bitrate = if (format.bitrate > 0) {
        val mbps = format.bitrate / 1_000_000f
        if (mbps >= 1.0f) String.format(java.util.Locale.US, "%.1f Mbps", mbps)
        else "${format.bitrate / 1000} kbps"
    } else null

    val fps = if (format.frameRate > 0) "${format.frameRate.toInt()}fps" else null
    val details = listOfNotNull(bitrate, fps).joinToString(" • ")

    return if (details.isNotBlank()) "$res • $details" else res
}

data class AudioTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

data class SubtitleTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

fun formatAudioTrackLabel(format: androidx.media3.common.Format, index: Int): String {
    val lang = format.language?.uppercase()?.takeIf { it.isNotBlank() }
    val explicitLabel = format.label?.takeIf { it.isNotBlank() }

    val codec = when {
        format.sampleMimeType?.contains("eac3-joc", ignoreCase = true) == true -> "Dolby Atmos"
        format.sampleMimeType?.contains("eac3", ignoreCase = true) == true -> "E-AC-3"
        format.sampleMimeType?.contains("ac3", ignoreCase = true) == true -> "AC-3"
        format.sampleMimeType?.contains("ac4", ignoreCase = true) == true -> "AC-4"
        format.sampleMimeType?.contains("dts.hd", ignoreCase = true) == true -> "DTS-HD"
        format.sampleMimeType?.contains("dts", ignoreCase = true) == true -> "DTS"
        format.sampleMimeType?.contains("true-hd", ignoreCase = true) == true -> "TrueHD"
        format.sampleMimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
        format.sampleMimeType?.contains("opus", ignoreCase = true) == true -> "Opus"
        format.sampleMimeType?.contains("vorbis", ignoreCase = true) == true -> "Vorbis"
        format.sampleMimeType?.contains("mp4a", ignoreCase = true) == true -> "AAC"
        format.sampleMimeType?.contains("mpeg", ignoreCase = true) == true -> "MP3"
        format.sampleMimeType?.contains("alac", ignoreCase = true) == true -> "ALAC"
        format.sampleMimeType?.contains("pcm", ignoreCase = true) == true || format.sampleMimeType?.contains("raw", ignoreCase = true) == true -> "PCM"
        format.sampleMimeType?.contains("amr", ignoreCase = true) == true -> "AMR"
        format.sampleMimeType?.contains("wma", ignoreCase = true) == true -> "WMA"
        !format.codecs.isNullOrBlank() -> format.codecs
        !format.sampleMimeType.isNullOrBlank() -> format.sampleMimeType!!.substringAfterLast('/')
        else -> null
    }

    val channels = when (format.channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1 Surround"
        8 -> "7.1 Surround"
        in 3..5 -> "${format.channelCount}ch"
        in 9..16 -> "${format.channelCount}ch"
        else -> null
    }

    val sampleRate = if (format.sampleRate > 0) {
        val khz = format.sampleRate / 1000f
        if (khz == khz.toInt().toFloat()) "${khz.toInt()} kHz" else String.format(java.util.Locale.US, "%.1f kHz", khz)
    } else null

    val details = listOfNotNull(codec, channels, sampleRate).joinToString(" • ")

    return when {
        explicitLabel != null && details.isNotBlank() -> "$explicitLabel ($details)"
        explicitLabel != null -> explicitLabel
        lang != null && details.isNotBlank() -> "$lang • $details"
        lang != null -> "Audio ($lang)"
        details.isNotBlank() -> "Track ${index + 1} ($details)"
        else -> "Audio Track ${index + 1}"
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val userPrefs = remember { VideoLibraryApp.instance.userPreferences }
    val scope = rememberCoroutineScope()

    val video by repository.getVideoFlowById(videoId).collectAsState(initial = null)
    val skipIntervalSec by userPrefs.skipIntervalSec.collectAsState()
    val gestureEnabled by userPrefs.gestureControls.collectAsState()
    val defaultSpeed by userPrefs.playbackSpeed.collectAsState()
    val autoResumeSetting by userPrefs.autoResume.collectAsState()

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    var playbackSpeed by remember { mutableFloatStateOf(defaultSpeed) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    var currentScaleMode by remember { mutableStateOf(VideoScaleMode.FIT) }
    var subtitleSize by remember { mutableStateOf(SubtitleSize.MEDIUM) }
    var volumeBoostPercent by remember { mutableIntStateOf(0) }
    var loudnessEnhancer by remember { mutableStateOf<android.media.audiofx.LoudnessEnhancer?>(null) }
    var audioDelayMs by remember { mutableLongStateOf(0L) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }

    var currentTracks by remember { mutableStateOf<Tracks?>(null) }

    // Gesture status overlays (Volume / Brightness indicator)
    var gestureOverlayText by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }

    // Gesture accumulators to ensure smooth, gradual adjustments
    var accumulatedVolumeDrag by remember { mutableFloatStateOf(0f) }
    var accumulatedBrightnessDrag by remember { mutableFloatStateOf(0f) }

    var showResumeDialog by remember { mutableStateOf(false) }
    var pendingResumePosition by remember { mutableLongStateOf(0L) }

    val isLive = video?.isLiveStream == true

    fun retryPlayback() {
        val p = player ?: return
        playbackError = null
        isBuffering = true
        p.prepare()
        p.play()
    }

    // Save playback position helper
    fun saveProgress() {
        val p = player ?: return
        if (isLive) return
        val pos = p.currentPosition
        val dur = p.duration
        if (pos > 0 && dur > 0) {
            scope.launch {
                repository.updatePlaybackProgress(videoId, pos, dur)
            }
        }
    }

    // Initialize ExoPlayer with AudioAttributes, MediaSourceFactory & Audio Focus
    LaunchedEffect(video) {
        val currentVideo = video ?: return@LaunchedEffect
        if (durationMs == 0L && currentVideo.durationMs > 0L) {
            durationMs = currentVideo.durationMs
        }
        if (player != null) return@LaunchedEffect

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(NetworkClient.DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setKeepPostFor302Redirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setAdtsExtractorFlags(AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
            .setAmrExtractorFlags(AmrExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
            .setFlacExtractorFlags(FlacExtractor.FLAG_DISABLE_ID3_METADATA)
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            setAllowedVideoJoiningTimeMs(5000)
        }

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedSampleRateAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
                    .setAllowMultipleAdaptiveSelections(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
            )
        }

        val exo = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
                volume = 1.0f

                val mediaUri = if (currentVideo.isDownloaded && !currentVideo.localPath.isNullOrBlank() && File(currentVideo.localPath).exists()) {
                    Uri.fromFile(File(currentVideo.localPath))
                } else {
                    Uri.parse(currentVideo.sourceUrl)
                }

                val mediaItemBuilder = MediaItem.Builder().setUri(mediaUri)

                // Subtitle attachment if available
                if (!currentVideo.subtitleUri.isNullOrBlank()) {
                    val subMime = if (currentVideo.subtitleUri.endsWith(".vtt", ignoreCase = true)) {
                        MimeTypes.TEXT_VTT
                    } else {
                        MimeTypes.APPLICATION_SUBRIP
                    }
                    val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(currentVideo.subtitleUri))
                        .setMimeType(subMime)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
                }

                setMediaItem(mediaItemBuilder.build())
                setPlaybackSpeed(playbackSpeed)
                prepare()

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) {
                            playbackError = null
                            val dur = duration
                            if (dur > 0 && dur != C.TIME_UNSET && !isLive) {
                                durationMs = dur
                            }
                        }
                    }

                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        val dur = duration
                        if (dur > 0 && dur != C.TIME_UNSET && !isLive) {
                            durationMs = dur
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                            currentPositionMs = newPosition.positionMs.coerceAtLeast(0L)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val hasAudioOverride = trackSelectionParameters.overrides.values.any {
                            it.type == C.TRACK_TYPE_AUDIO
                        }
                        val isAudioOrDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED

                        // Auto-recover if manual audio track selection caused a decoder/track init failure
                        if (hasAudioOverride && isAudioOrDecoderError) {
                            trackSelectionParameters = trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .build()
                            prepare()
                            play()
                            return
                        }

                        isBuffering = false
                        isPlaying = false
                        val cause = error.cause
                        val errorDetails = when {
                            cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403 ->
                                "Access forbidden (HTTP 403): The media server restricted access to this stream. Check permissions or network access."
                            cause is HttpDataSource.InvalidResponseCodeException ->
                                "Media server returned HTTP ${cause.responseCode}. Stream cannot be loaded."
                            cause is HttpDataSource.HttpDataSourceException ->
                                "Network connection failed while streaming media. Check your connection."
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                                "Network connection failed. Please check internet access."
                            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                                "Unsupported stream or codec format."
                            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                                "Audio/Video decoder failed to initialize for this format."
                            else ->
                                error.message ?: "Failed to play video. Stream source may be unavailable."
                        }
                        playbackError = errorDetails
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        currentTracks = tracks
                    }
                })
            }

        player = exo

        // Check if there is an existing playback position to resume (disabled for live streams)
        if (!isLive && currentVideo.playbackPositionMs > 10000L && currentVideo.watchedPercentage < 0.92f) {
            if (autoResumeSetting) {
                exo.seekTo(currentVideo.playbackPositionMs)
                exo.playWhenReady = true
            } else {
                pendingResumePosition = currentVideo.playbackPositionMs
                showResumeDialog = true
            }
        } else {
            exo.playWhenReady = true
        }
    }

    // Periodic time polling (does not overwrite slider position while user is dragging)
    LaunchedEffect(player, isPlaying, isDraggingSlider) {
        while (true) {
            player?.let { p ->
                if (!isDraggingSlider) {
                    currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                }
                val dur = p.duration
                if (dur > 0 && dur != C.TIME_UNSET && !isLive) {
                    durationMs = dur
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls after 4 seconds of inactivity (only when playing and not seeking)
    LaunchedEffect(controlsVisible, isDraggingSlider, isPlaying) {
        if (controlsVisible && !isControlsLocked && !isDraggingSlider && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Volume Boost (LoudnessEnhancer) attachment
    LaunchedEffect(player, volumeBoostPercent) {
        val p = player ?: return@LaunchedEffect
        val sessionId = p.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                }
                loudnessEnhancer?.let { enhancer ->
                    enhancer.setTargetGain(volumeBoostPercent * 10)
                    enhancer.enabled = volumeBoostPercent > 0
                }
            } catch (_: Exception) {}
        }
    }

    // Back button handling
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            saveProgress()
            try {
                loudnessEnhancer?.release()
            } catch (_: Exception) {}
            loudnessEnhancer = null
            player?.stop()
            player?.release()
            player = null
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveProgress()
            try {
                loudnessEnhancer?.release()
            } catch (_: Exception) {}
            loudnessEnhancer = null
            player?.stop()
            player?.release()
            player = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Parse Video Tracks
    val availableVideoTracks = remember(currentTracks) {
        val list = mutableListOf<VideoTrackItem>()
        currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = formatVideoTrackLabel(format, list.size)
                    list.add(
                        VideoTrackItem(
                            group = group,
                            trackIndex = i,
                            label = label,
                            isSelected = group.isTrackSelected(i),
                            width = format.width,
                            height = format.height,
                            bitrate = format.bitrate
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.height * 10000 + it.bitrate }
    }

    // Parse Audio Tracks
    val availableAudioTracks = remember(currentTracks) {
        val list = mutableListOf<AudioTrackItem>()
        currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = formatAudioTrackLabel(format, list.size)
                    list.add(AudioTrackItem(group, i, label, group.isTrackSelected(i)))
                }
            }
        }
        list
    }

    // Parse Subtitle Tracks
    val availableSubtitleTracks = remember(currentTracks) {
        val list = mutableListOf<SubtitleTrackItem>()
        currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.uppercase() ?: ""
                    val label = format.label?.takeIf { it.isNotBlank() }
                        ?: if (lang.isNotBlank()) "Subtitles ($lang)" else "Subtitle ${list.size + 1}"
                    list.add(SubtitleTrackItem(group, i, label, group.isTrackSelected(i)))
                }
            }
        }
        list
    }

    val hasAudioOverride = remember(player?.trackSelectionParameters) {
        player?.trackSelectionParameters?.overrides?.values?.any { it.type == C.TRACK_TYPE_AUDIO } == true
    }
    val hasVideoOverride = remember(player?.trackSelectionParameters) {
        player?.trackSelectionParameters?.overrides?.values?.any { it.type == C.TRACK_TYPE_VIDEO } == true
    }
    val hasSubtitleDisabled = remember(player?.trackSelectionParameters) {
        player?.trackSelectionParameters?.disabledTrackTypes?.contains(C.TRACK_TYPE_TEXT) == true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    resizeMode = currentScaleMode.resizeMode
                    subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize.sizeSp)
                    subtitleView?.setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.argb(160, 0, 0, 0),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            android.graphics.Color.BLACK,
                            null
                        )
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = currentScaleMode.resizeMode
                playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize.sizeSp)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Detection Overlay with Smooth Sensitivity Curve
        if (gestureEnabled && !isControlsLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = { offset ->
                                if (isLive) return@detectTapGestures
                                val width = size.width
                                val skipIntervalMs = skipIntervalSec * 1000L
                                player?.let { p ->
                                    if (offset.x < width / 2) {
                                        val newPos = (p.currentPosition - skipIntervalMs).coerceAtLeast(0L)
                                        currentPositionMs = newPos
                                        p.seekTo(newPos)
                                        gestureOverlayText = "-${skipIntervalSec}s"
                                        gestureOverlayIcon = Icons.Default.FastRewind
                                    } else {
                                        val dur = if (durationMs > 0) durationMs else (p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
                                        val newPos = (p.currentPosition + skipIntervalMs).coerceAtMost(dur)
                                        currentPositionMs = newPos
                                        p.seekTo(newPos)
                                        gestureOverlayText = "+${skipIntervalSec}s"
                                        gestureOverlayIcon = Icons.Default.FastForward
                                    }
                                    scope.launch {
                                        delay(800)
                                        gestureOverlayText = null
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                accumulatedVolumeDrag = 0f
                                accumulatedBrightnessDrag = 0f
                            },
                            onDragCancel = {
                                accumulatedVolumeDrag = 0f
                                accumulatedBrightnessDrag = 0f
                            }
                        ) { change, dragAmount ->
                            val width = size.width
                            val isRightSide = change.position.x > width / 2

                            if (isRightSide) {
                                // Volume change: smooth accumulator (1 step per 32 pixels)
                                accumulatedVolumeDrag += dragAmount
                                val stepPixels = 32f
                                if (abs(accumulatedVolumeDrag) >= stepPixels) {
                                    val steps = (accumulatedVolumeDrag / stepPixels).toInt()
                                    accumulatedVolumeDrag -= steps * stepPixels

                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = -steps
                                    val newVol = (currentVol + delta).coerceIn(0, maxVol)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)

                                    val percent = (newVol * 100) / maxVol.coerceAtLeast(1)
                                    val blocks = (percent / 10).coerceIn(0, 10)
                                    val meter = "█".repeat(blocks) + "░".repeat(10 - blocks)

                                    gestureOverlayText = "Volume $meter $percent%"
                                    gestureOverlayIcon = Icons.Default.VolumeUp

                                    scope.launch {
                                        delay(1200)
                                        gestureOverlayText = null
                                    }
                                }
                            } else {
                                // Brightness change: smooth accumulator (2% per 28 pixels)
                                accumulatedBrightnessDrag += dragAmount
                                val stepPixels = 28f
                                if (abs(accumulatedBrightnessDrag) >= stepPixels) {
                                    val steps = (accumulatedBrightnessDrag / stepPixels).toInt()
                                    accumulatedBrightnessDrag -= steps * stepPixels

                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        val currentBrightness = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                        val delta = -steps * 0.02f
                                        lp.screenBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                                        act.window.attributes = lp

                                        val percent = (lp.screenBrightness * 100).toInt()
                                        val blocks = (percent / 10).coerceIn(0, 10)
                                        val meter = "█".repeat(blocks) + "░".repeat(10 - blocks)

                                        gestureOverlayText = "Brightness $meter $percent%"
                                        gestureOverlayIcon = Icons.Default.BrightnessMedium
                                    }

                                    scope.launch {
                                        delay(1200)
                                        gestureOverlayText = null
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Gesture feedback icon & text overlay
        if (gestureOverlayText != null) {
            Surface(
                color = Color(0xCC000000),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (gestureOverlayIcon != null) {
                        Icon(gestureOverlayIcon!!, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = gestureOverlayText!!,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = ElectricViolet,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )
        }

        // Playback Error Overlay
        if (playbackError != null) {
            Surface(
                color = Color(0xEE1A1B20),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
                    .widthIn(max = 440.dp)
                    .testTag("playback_error_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Playback Error",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Unable to Stream Video",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError ?: "Stream error occurred.",
                        color = Color(0xFFD0D0D0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                saveProgress()
                                player?.stop()
                                player?.release()
                                player = null
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("error_back_button")
                        ) {
                            Text("Go Back", color = Color.White)
                        }
                        Button(
                            onClick = { retryPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("retry_playback_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }

        // Lock button (always accessible even when controls hidden)
        if (isControlsLocked) {
            Surface(
                shape = CircleShape,
                color = Color(0x88000000),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                IconButton(onClick = { isControlsLocked = false }) {
                    Icon(Icons.Default.Lock, contentDescription = "Unlock controls", tint = Color.White)
                }
            }
        }

        // Player Controls HUD
        AnimatedVisibility(
            visible = controlsVisible && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            saveProgress()
                            player?.stop()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("player_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = video?.title ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (isLive) {
                            Surface(
                                color = Color(0xFFE53935),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "● LIVE BROADCAST",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Video Settings (Quality / Resolution & Aspect Ratio)
                        IconButton(
                            onClick = { showVideoDialog = true },
                            modifier = Modifier.testTag("player_video_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Video Quality & Aspect Ratio",
                                tint = if (hasVideoOverride || currentScaleMode != VideoScaleMode.FIT) ElectricViolet else Color.White
                            )
                        }

                        // Audio Settings (Track Selector, Software Boost, Audio Delay)
                        IconButton(
                            onClick = { showAudioDialog = true },
                            modifier = Modifier.testTag("player_audio_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Tracks & Settings",
                                tint = if (hasAudioOverride || volumeBoostPercent > 0 || audioDelayMs != 0L) ElectricViolet else Color.White
                            )
                        }

                        // Subtitle Settings (Track Selector, Text Size, Subtitle Delay)
                        IconButton(
                            onClick = { showSubtitleDialog = true },
                            modifier = Modifier.testTag("player_subtitle_settings_btn")
                        ) {
                            Icon(
                                imageVector = if (!hasSubtitleDisabled && availableSubtitleTracks.any { it.isSelected }) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                contentDescription = "Subtitle Settings",
                                tint = if (!hasSubtitleDisabled && availableSubtitleTracks.any { it.isSelected }) NeonCyan else Color.White
                            )
                        }

                        // Lock Controls
                        IconButton(onClick = { isControlsLocked = true }) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Lock controls", tint = Color.White)
                        }

                        // Speed selector (disabled for live stream)
                        if (!isLive) {
                            Box {
                                IconButton(onClick = { showSpeedMenu = true }) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x", fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                playbackSpeed = speed
                                                player?.setPlaybackSpeed(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Fullscreen / Landscape toggle
                        IconButton(
                            onClick = {
                                isFullscreen = !isFullscreen
                                if (isFullscreen) {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                } else {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            },
                            modifier = Modifier.testTag("player_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Play / Rewind / Fast-Forward Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLive) {
                        // Rewind button
                        IconButton(
                            onClick = {
                                val newPos = (currentPositionMs - (skipIntervalSec * 1000L)).coerceAtLeast(0L)
                                currentPositionMs = newPos
                                player?.seekTo(newPos)
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind $skipIntervalSec sec",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Play/Pause button
                    Surface(
                        shape = CircleShape,
                        color = ElectricViolet,
                        modifier = Modifier.size(68.dp)
                    ) {
                        IconButton(
                            onClick = {
                                player?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                }
                            },
                            modifier = Modifier.testTag("player_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    if (!isLive) {
                        // Fast-Forward button
                        IconButton(
                            onClick = {
                                val maxPos = if (durationMs > 0) durationMs else (player?.duration?.takeIf { it > 0 } ?: Long.MAX_VALUE)
                                val newPos = (currentPositionMs + (skipIntervalSec * 1000L)).coerceAtMost(maxPos)
                                currentPositionMs = newPos
                                player?.seekTo(newPos)
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward $skipIntervalSec sec",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline and Time Bar (only shown for non-live videos)
                if (!isLive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { }
                            }
                    ) {
                        val maxDuration = maxOf(1f, durationMs.toFloat())
                        val displayPos = if (isDraggingSlider) sliderPositionMs else currentPositionMs.toFloat()

                        Slider(
                            value = displayPos.coerceIn(0f, maxDuration),
                            onValueChange = { newPos ->
                                isDraggingSlider = true
                                sliderPositionMs = newPos
                            },
                            onValueChangeFinished = {
                                val targetPos = sliderPositionMs.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                                currentPositionMs = targetPos
                                player?.seekTo(targetPos)
                                isDraggingSlider = false
                            },
                            valueRange = 0f..maxDuration,
                            enabled = durationMs > 0,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricViolet,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = Color(0x44FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_timeline_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(if (isDraggingSlider) sliderPositionMs.toLong() else currentPositionMs),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatDuration(durationMs),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    // Resume Dialog
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = {
                showResumeDialog = false
                player?.playWhenReady = true
            },
            title = { Text("Resume Playback?") },
            text = { Text("Resume watching from ${formatDuration(pendingResumePosition)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        player?.seekTo(pendingResumePosition)
                        player?.playWhenReady = true
                        showResumeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        player?.seekTo(0)
                        player?.playWhenReady = true
                        showResumeDialog = false
                    }
                ) {
                    Text("Start Over")
                }
            }
        )
    }

    // Video & Display Settings Dialog
    if (showVideoDialog) {
        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            containerColor = Color(0xFA1E1F24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Video & Display", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Section 1: Quality / Resolution
                    Text("RESOLUTION & QUALITY", color = ElectricViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = if (!hasVideoOverride) ElectricViolet.copy(alpha = 0.2f) else Color(0x22FFFFFF),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    player?.let { p ->
                                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                            .build()
                                    }
                                } catch (_: Exception) {}
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!hasVideoOverride) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column {
                                Text("Auto (Adaptive)", color = Color.White, fontWeight = if (!hasVideoOverride) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                Text("Dynamically adjusts quality for bandwidth", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }

                    if (availableVideoTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                            items(availableVideoTracks) { trackItem ->
                                val isSelected = hasVideoOverride && trackItem.isSelected
                                Surface(
                                    color = if (isSelected) ElectricViolet.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                player?.let { p ->
                                                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                                        .setOverrideForType(TrackSelectionOverride(trackItem.group.mediaTrackGroup, trackItem.trackIndex))
                                                        .build()
                                                }
                                            } catch (_: Exception) {}
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(trackItem.label, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (video != null && video!!.width > 0) "Current video stream: ${video!!.width}x${video!!.height} (${video!!.format.uppercase()})" else "Single Progressive Stream",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Section 2: Aspect Ratio Mode
                    Text("ASPECT RATIO / DISPLAY MODE", color = ElectricViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoScaleMode.values().forEach { mode ->
                            val isSelected = currentScaleMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricViolet else Color(0x22FFFFFF),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        currentScaleMode = mode
                                    }
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVideoDialog = false }) {
                    Text("Done", color = ElectricViolet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Audio Settings Dialog
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = Color(0xFA1E1F24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Audio Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Section 1: Audio Tracks
                    Text("AUDIO TRACKS (${availableAudioTracks.size})", color = ElectricViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)) {
                        item {
                            Surface(
                                color = if (!hasAudioOverride) ElectricViolet.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            player?.let { p ->
                                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                                    .build()
                                            }
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!hasAudioOverride) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Auto (Default Track)", color = Color.White, fontWeight = if (!hasAudioOverride) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }

                        items(availableAudioTracks) { trackItem ->
                            val isSelected = hasAudioOverride && trackItem.isSelected
                            Surface(
                                color = if (isSelected) ElectricViolet.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            player?.let { p ->
                                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                                    .setOverrideForType(TrackSelectionOverride(trackItem.group.mediaTrackGroup, trackItem.trackIndex))
                                                    .build()
                                            }
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(trackItem.label, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 2: Volume Boost (Software Amplifier)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VOLUME BOOST", color = ElectricViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(if (volumeBoostPercent == 0) "Normal (100%)" else "+$volumeBoostPercent%", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 25, 50, 75, 100).forEach { boost ->
                            val isSelected = volumeBoostPercent == boost
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ElectricViolet else Color(0x22FFFFFF),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        volumeBoostPercent = boost
                                    }
                            ) {
                                Text(
                                    text = if (boost == 0) "Off" else "+$boost%",
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 3: Audio Sync (Offset)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AUDIO SYNC", color = ElectricViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        if (audioDelayMs != 0L) {
                            TextButton(onClick = { audioDelayMs = 0L }, modifier = Modifier.height(28.dp)) {
                                Text("Reset", color = ElectricViolet, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { audioDelayMs = (audioDelayMs - 50L).coerceAtLeast(-2000L) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease audio delay", tint = Color.White)
                        }
                        Text(
                            text = if (audioDelayMs == 0L) "0 ms" else if (audioDelayMs > 0) "+$audioDelayMs ms" else "$audioDelayMs ms",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { audioDelayMs = (audioDelayMs + 50L).coerceAtMost(2000L) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase audio delay", tint = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text("Done", color = ElectricViolet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Subtitle Settings Dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = Color(0xFA1E1F24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Subtitle Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Section 1: Subtitle Tracks
                    Text("SUBTITLE TRACKS", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)) {
                        item {
                            Surface(
                                color = if (hasSubtitleDisabled) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            player?.let { p ->
                                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                                    .build()
                                            }
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasSubtitleDisabled) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Subtitles Off", color = Color.White, fontWeight = if (hasSubtitleDisabled) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }

                        items(availableSubtitleTracks) { subItem ->
                            val isSelected = !hasSubtitleDisabled && subItem.isSelected
                            Surface(
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            player?.let { p ->
                                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                    .setOverrideForType(TrackSelectionOverride(subItem.group.mediaTrackGroup, subItem.trackIndex))
                                                    .build()
                                            }
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(subItem.label, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 2: Subtitle Text Size
                    Text("TEXT SIZE", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubtitleSize.values().forEach { sizeOption ->
                            val isSelected = subtitleSize == sizeOption
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonCyan else Color(0x22FFFFFF),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        subtitleSize = sizeOption
                                    }
                            ) {
                                Text(
                                    text = sizeOption.label,
                                    color = if (isSelected) Color.Black else Color.LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 3: Subtitle Timing Sync (Delay Offset)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SUBTITLE TIMING SYNC", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        if (subtitleDelayMs != 0L) {
                            TextButton(onClick = { subtitleDelayMs = 0L }, modifier = Modifier.height(28.dp)) {
                                Text("Reset", color = NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { subtitleDelayMs = (subtitleDelayMs - 50L).coerceAtLeast(-5000L) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease subtitle delay", tint = Color.White)
                        }
                        Text(
                            text = if (subtitleDelayMs == 0L) "0 ms" else if (subtitleDelayMs > 0) "+$subtitleDelayMs ms" else "$subtitleDelayMs ms",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { subtitleDelayMs = (subtitleDelayMs + 50L).coerceAtMost(5000L) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase subtitle delay", tint = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Done", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
