package com.example.ui.screens.video

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioType
import com.example.model.StudioFilter
import com.example.model.VideoClip
import com.example.ui.components.StudioSlider
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

@Composable
fun VideoStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.stopVideoPlayback()
        viewModel.navigateTo(AppScreen.HUB)
    }

    val title by viewModel.activeProjectTitle.collectAsState()
    val clips by viewModel.videoClips.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val playheadMs by viewModel.videoPlayheadMs.collectAsState()
    val isPlaying by viewModel.isVideoPlaying.collectAsState()
    val overlays by viewModel.videoOverlays.collectAsState()

    val totalDurationMs = viewModel.totalVideoDurationMs
    val selectedClip = clips.find { it.id == selectedClipId } ?: clips.firstOrNull()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_studio_screen"),
        topBar = {
            StudioTopBar(
                title = title,
                subtitle = "${aspectRatio.label} • ${totalDurationMs / 1000}s Timeline",
                onBack = {
                    viewModel.stopVideoPlayback()
                    viewModel.navigateTo(AppScreen.HUB)
                },
                onSave = {
                    viewModel.saveVideoProject()
                    scope.launch {
                        snackbarHostState.showSnackbar("Video project saved! (${clips.size} clips)")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = StudioBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Monitor Aspect Ratio Switcher Bar
            AspectRatioSelectorBar(
                currentRatio = aspectRatio,
                onSelectRatio = { viewModel.setAspectRatio(it) }
            )

            // Video Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF070A0F))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                VideoMonitorView(
                    aspectRatio = aspectRatio,
                    playheadMs = playheadMs,
                    totalDurationMs = totalDurationMs,
                    activeClip = selectedClip,
                    overlays = overlays,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.toggleVideoPlayback() }
                )
            }

            // Timeline Scrubber & Clip Arranger Track
            TimelineTrackView(
                clips = clips,
                selectedClipId = selectedClipId,
                playheadMs = playheadMs,
                totalDurationMs = totalDurationMs,
                onSelectClip = { viewModel.selectClip(it) },
                onSeek = { viewModel.seekVideo(it) }
            )

            // Studio Tabs (Trim & Speed vs. Overlays & Captions)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = StudioSurface,
                contentColor = StudioCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = StudioCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Trim & Speed", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Overlays & Titles", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Clips (${clips.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .background(StudioSurface)
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> ClipTrimmerPanel(
                        clip = selectedClip,
                        onUpdateTrim = { start, end ->
                            selectedClip?.let { viewModel.updateClipTrim(it.id, start, end) }
                        },
                        onSetSpeed = { speed ->
                            selectedClip?.let { viewModel.setClipSpeed(it.id, speed) }
                        },
                        onSetFilter = { filter ->
                            selectedClip?.let { viewModel.setClipFilter(it.id, filter) }
                        }
                    )
                    1 -> OverlaysPanel(
                        overlays = overlays,
                        onAddTitle = { viewModel.addVideoOverlay("CINEMATIC CUT") },
                        onAddWatermark = { viewModel.addVideoOverlay("MILLER'S MASTER") }
                    )
                    2 -> ClipManagerPanel(
                        clips = clips,
                        selectedId = selectedClipId,
                        onSelect = { viewModel.selectClip(it) },
                        onAddClip = { viewModel.addVideoClip("Scene ${clips.size + 1}") },
                        onDeleteClip = { viewModel.deleteSelectedClip() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectRatioSelectorBar(
    currentRatio: AspectRatioType,
    onSelectRatio: (AspectRatioType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "FRAME RATIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioTextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AspectRatioType.values().forEach { ratio ->
                    val isSelected = ratio == currentRatio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) StudioCoral.copy(alpha = 0.2f) else StudioCard)
                            .border(
                                BorderStroke(1.dp, if (isSelected) StudioCoral else StudioBorder),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectRatio(ratio) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ratio.label.split(" ")[0],
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) StudioCoral else StudioTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoMonitorView(
    aspectRatio: AspectRatioType,
    playheadMs: Long,
    totalDurationMs: Long,
    activeClip: VideoClip?,
    overlays: List<com.example.model.VideoOverlay>,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    val secCurrent = playheadMs / 1000f
    val secTotal = totalDurationMs / 1000f

    Card(
        modifier = Modifier
            .aspectRatio(aspectRatio.ratio, matchHeightConstraintsFirst = true)
            .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(10.dp))
            .clickable(onClick = onTogglePlay)
            .testTag("video_monitor_screen"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated video motion frame
            val baseColor = activeClip?.accentColor?.let { Color(it) } ?: Color(0xFF1976D2)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.7f),
                                Color(0xFF050811)
                            )
                        )
                    )
            )

            // Dynamic Scene Visual Elements
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = activeClip?.title ?: "Clip",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SPEED ${activeClip?.speed ?: 1.0}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioAmber
                        )
                    }
                }

                // Center Active Overlays
                overlays.forEach { ov ->
                    if (playheadMs in ov.startMs..ov.endMs) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .border(BorderStroke(1.dp, StudioCoral), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ov.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = StudioCoral
                            )
                        }
                    }
                }

                // Center Play Icon if paused
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(BorderStroke(2.dp, StudioCoral), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = StudioCoral,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // Timecode Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "%.1fs / %.1fs".format(secCurrent, secTotal),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineTrackView(
    clips: List<VideoClip>,
    selectedClipId: String?,
    playheadMs: Long,
    totalDurationMs: Long,
    onSelectClip: (String) -> Unit,
    onSeek: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TIMELINE CLIPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextSecondary
                )
                Text(
                    text = "Drag needle to scrub",
                    fontSize = 10.sp,
                    color = StudioCyan
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Clips strip with Playhead overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCard)
                    .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(8.dp))
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((progress * totalDurationMs).toLong())
                        }
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    clips.forEach { clip ->
                        val isSelected = clip.id == selectedClipId
                        val weight = (clip.activeDurationMs.toFloat() / totalDurationMs.coerceAtLeast(1L)).coerceAtLeast(0.1f)
                        val color = Color(clip.accentColor)

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxSize()
                                .background(color.copy(alpha = if (isSelected) 0.6f else 0.3f))
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) StudioCoral else StudioBorder
                                    )
                                )
                                .clickable { onSelectClip(clip.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = clip.title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Playhead Needle
                val playheadFraction = if (totalDurationMs > 0) (playheadMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val x = size.width * playheadFraction
                    drawLine(
                        color = StudioCoral,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawCircle(
                        color = StudioCoral,
                        radius = 4.dp.toPx(),
                        center = Offset(x, 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipTrimmerPanel(
    clip: VideoClip?,
    onUpdateTrim: (Long, Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetFilter: (StudioFilter) -> Unit
) {
    if (clip == null) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trim: ${clip.title}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Active: %.1fs".format(clip.activeDurationMs / 1000f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCoral
                )
            }
        }

        item {
            StudioSlider(
                label = "In-Point (Start Trim)",
                value = clip.trimStartMs.toFloat(),
                onValueChange = { onUpdateTrim(it.toLong(), clip.trimEndMs) },
                valueRange = 0f..(clip.totalDurationMs - 500f),
                displayValue = "%.1fs".format(clip.trimStartMs / 1000f),
                accentColor = StudioCoral
            )
        }

        item {
            StudioSlider(
                label = "Out-Point (End Trim)",
                value = clip.trimEndMs.toFloat(),
                onValueChange = { onUpdateTrim(clip.trimStartMs, it.toLong()) },
                valueRange = (clip.trimStartMs + 500f)..clip.totalDurationMs.toFloat(),
                displayValue = "%.1fs".format(clip.trimEndMs / 1000f),
                accentColor = StudioAmber
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clip Speed:", fontSize = 11.sp, color = StudioTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = clip.speed == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) StudioCyan else StudioCard)
                                .clickable { onSetSpeed(speed) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else StudioTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlaysPanel(
    overlays: List<com.example.model.VideoOverlay>,
    onAddTitle: () -> Unit,
    onAddWatermark: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onAddTitle,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCoral, contentColor = Color.White),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(imageVector = Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Title", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onAddWatermark,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyan),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Add Stamp", fontSize = 11.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(overlays) { ov ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCard)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = ov.text, fontSize = 12.sp, color = StudioTextPrimary)
                    Text(
                        text = "%.1fs - %.1fs".format(ov.startMs / 1000f, ov.endMs / 1000f),
                        fontSize = 10.sp,
                        color = StudioTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipManagerPanel(
    clips: List<VideoClip>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAddClip: () -> Unit,
    onDeleteClip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddClip,
                colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald, contentColor = Color.Black),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Scene", fontSize = 11.sp)
            }

            IconButton(onClick = onDeleteClip, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StudioCoral, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(clips) { clip ->
                val isSelected = clip.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) StudioCoral.copy(alpha = 0.2f) else StudioCard)
                        .border(
                            BorderStroke(1.dp, if (isSelected) StudioCoral else StudioBorder),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelect(clip.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = clip.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = StudioTextPrimary)
                    Text(text = "%.1fs".format(clip.activeDurationMs / 1000f), fontSize = 11.sp, color = StudioCoral)
                }
            }
        }
    }
}
