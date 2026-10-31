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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.VideoLibraryApp
import com.example.ui.components.formatDuration
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

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
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    var playbackSpeed by remember { mutableFloatStateOf(defaultSpeed) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

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

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val exo = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
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
                            durationMs = duration.coerceAtLeast(0L)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
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

    // Periodic time polling
    LaunchedEffect(player, isPlaying) {
        while (true) {
            player?.let {
                currentPositionMs = it.currentPosition.coerceAtLeast(0L)
                if (it.duration > 0 && !isLive) durationMs = it.duration
            }
            delay(500)
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !isControlsLocked) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Back button handling
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            saveProgress()
            player?.stop()
            player?.release()
            player = null
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            saveProgress()
            player?.stop()
            player?.release()
            player = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Parse Audio Tracks
    val availableAudioTracks = remember(currentTracks) {
        val list = mutableListOf<AudioTrackItem>()
        currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.uppercase() ?: ""
                    val label = format.label?.takeIf { it.isNotBlank() }
                        ?: if (lang.isNotBlank()) "Audio ($lang)" else "Audio Track ${list.size + 1}"
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
                }
            },
            update = { playerView ->
                playerView.player = player
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
                                        p.seekTo((p.currentPosition - skipIntervalMs).coerceAtLeast(0L))
                                        gestureOverlayText = "-${skipIntervalSec}s"
                                        gestureOverlayIcon = Icons.Default.FastRewind
                                    } else {
                                        p.seekTo((p.currentPosition + skipIntervalMs).coerceAtMost(p.duration))
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

                    Row {
                        // Audio Track Selector
                        Box {
                            IconButton(onClick = { showAudioMenu = true }) {
                                Icon(Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showAudioMenu,
                                onDismissRequest = { showAudioMenu = false }
                            ) {
                                if (availableAudioTracks.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Default Audio") },
                                        onClick = { showAudioMenu = false }
                                    )
                                } else {
                                    availableAudioTracks.forEach { trackItem ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (trackItem.isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricViolet)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(trackItem.label, fontWeight = if (trackItem.isSelected) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            },
                                            onClick = {
                                                player?.let { p ->
                                                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                        .setOverrideForType(TrackSelectionOverride(trackItem.group.mediaTrackGroup, trackItem.trackIndex))
                                                        .build()
                                                }
                                                showAudioMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Subtitle Track Selector
                        Box {
                            IconButton(onClick = { showSubtitleMenu = true }) {
                                Icon(
                                    imageVector = if (availableSubtitleTracks.any { it.isSelected }) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                    contentDescription = "Subtitle Tracks",
                                    tint = if (availableSubtitleTracks.any { it.isSelected }) NeonCyan else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showSubtitleMenu,
                                onDismissRequest = { showSubtitleMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Subtitles Off") },
                                    onClick = {
                                        player?.trackSelectionParameters = player?.trackSelectionParameters?.buildUpon()
                                            ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                            ?.build() ?: return@DropdownMenuItem
                                        showSubtitleMenu = false
                                    }
                                )
                                availableSubtitleTracks.forEach { subItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (subItem.isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(subItem.label, fontWeight = if (subItem.isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        },
                                        onClick = {
                                            player?.let { p ->
                                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                    .setOverrideForType(TrackSelectionOverride(subItem.group.mediaTrackGroup, subItem.trackIndex))
                                                    .build()
                                            }
                                            showSubtitleMenu = false
                                        }
                                    )
                                }
                            }
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
                                val newPos = (currentPositionMs + (skipIntervalSec * 1000L)).coerceAtMost(durationMs)
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
                    ) {
                        Slider(
                            value = currentPositionMs.toFloat().coerceAtMost(durationMs.toFloat()),
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toLong()
                            },
                            onValueChangeFinished = {
                                player?.seekTo(currentPositionMs)
                            },
                            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
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
                                text = formatDuration(currentPositionMs),
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
}
