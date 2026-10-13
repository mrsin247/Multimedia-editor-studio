package com.example.ui.screens.audio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SampleType
import com.example.model.SequencerChannel
import com.example.model.StudioAudioTrack
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun AudioStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.stopAudioPlayback()
        viewModel.navigateTo(AppScreen.HUB)
    }

    val title by viewModel.activeProjectTitle.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val currentStep by viewModel.currentSequencerStep.collectAsState()
    val channels by viewModel.sequencerChannels.collectAsState()
    val tracks by viewModel.audioTracks.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("audio_studio_screen"),
        topBar = {
            StudioTopBar(
                title = title,
                subtitle = "$bpm BPM • 16-Step PCM Synth",
                onBack = {
                    viewModel.stopAudioPlayback()
                    viewModel.navigateTo(AppScreen.HUB)
                },
                onSave = {
                    viewModel.saveAudioProject()
                    scope.launch {
                        snackbarHostState.showSnackbar("Audio project saved! ($bpm BPM)")
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
            // Master Transport & Waveform Bar
            MasterTransportHeader(
                bpm = bpm,
                isPlaying = isPlaying,
                currentStep = currentStep,
                onTogglePlay = { viewModel.toggleAudioPlayback() },
                onIncreaseBpm = { viewModel.setBpm(bpm + 5) },
                onDecreaseBpm = { viewModel.setBpm(bpm - 5) }
            )

            // Live Waveform Canvas
            LiveWaveformCanvas(
                isPlaying = isPlaying,
                currentStep = currentStep
            )

            // Studio Tabs (Beat Pads vs. 16-Step Sequencer vs. Multi-Track Mixer)
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
                    text = { Text("MPC Pads", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("16-Step Grid", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Mixer Tracks (${tracks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(StudioBackground)
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> MpcPadsView(
                        onPadClick = { sampleType ->
                            viewModel.triggerDrumPad(sampleType)
                        }
                    )
                    1 -> StepSequencerView(
                        channels = channels,
                        currentStep = currentStep,
                        isPlaying = isPlaying,
                        onStepClick = { chId, idx ->
                            viewModel.toggleSequencerStep(chId, idx)
                        }
                    )
                    2 -> MixerTracksView(
                        tracks = tracks,
                        onVolumeChange = { trkId, vol -> viewModel.setTrackVolume(trkId, vol) },
                        onToggleMute = { trkId -> viewModel.toggleTrackMute(trkId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MasterTransportHeader(
    bpm: Int,
    isPlaying: Boolean,
    currentStep: Int,
    onTogglePlay: () -> Unit,
    onIncreaseBpm: () -> Unit,
    onDecreaseBpm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // BPM Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCard)
                    .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                IconButton(onClick = onDecreaseBpm, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease BPM", tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "$bpm BPM",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCyan,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                IconButton(onClick = onIncreaseBpm, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase BPM", tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            // Step Indicator LED Beacon
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (bar in 0 until 4) {
                    val isActiveBar = currentStep / 4 == bar && isPlaying
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isActiveBar) StudioEmerald else StudioBorder)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "STEP ${currentStep + 1}/16",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) StudioEmerald else StudioTextMuted
                )
            }

            // Play / Stop Master Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) StudioCoral else StudioEmerald)
                    .clickable(onClick = onTogglePlay)
                    .testTag("master_play_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveWaveformCanvas(
    isPlaying: Boolean,
    currentStep: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF090D12))
            .border(BorderStroke(1.dp, StudioBorder))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val numBars = 48
        val barWidth = width / numBars

        for (i in 0 until numBars) {
            val stepMod = (currentStep + i) % 16
            val baseAmp = if (isPlaying) {
                (sin(i * 0.4 + currentStep * 0.6) * 0.4f + 0.5f).toFloat() * (height * 0.42f)
            } else {
                height * 0.08f
            }

            val x = i * barWidth + barWidth / 2f
            val color = if (i % 4 == 0) StudioCyan else StudioTextMuted.copy(alpha = 0.5f)

            drawLine(
                color = if (isPlaying) color else StudioBorder,
                start = Offset(x, centerY - baseAmp),
                end = Offset(x, centerY + baseAmp),
                strokeWidth = barWidth * 0.6f
            )
        }
    }
}

@Composable
private fun MpcPadsView(
    onPadClick: (SampleType) -> Unit
) {
    val samples = SampleType.values()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(samples) { sample ->
            val color = Color(sample.colorHex)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clickable { onPadClick(sample) }
                    .testTag("mpc_pad_${sample.name.lowercase()}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sample.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = StudioTextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${sample.baseFreq.toInt()} Hz",
                                fontSize = 11.sp,
                                color = StudioTextMuted
                            )
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSequencerView(
    channels: List<SequencerChannel>,
    currentStep: Int,
    isPlaying: Boolean,
    onStepClick: (String, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(channels, key = { it.id }) { channel ->
            val color = Color(channel.sampleType.colorHex)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCard)
                    .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.sampleType.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "${channel.steps.count { it }} triggers",
                        fontSize = 10.sp,
                        color = StudioTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 16 Step Buttons in 4 beats
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(16) { stepIdx ->
                        val isActive = channel.steps.getOrElse(stepIdx) { false }
                        val isPlayheadHere = isPlaying && currentStep == stepIdx
                        val isQuarterBeat = stepIdx % 4 == 0

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isActive && isPlayheadHere -> Color.White
                                        isActive -> color
                                        isPlayheadHere -> StudioEmerald.copy(alpha = 0.4f)
                                        isQuarterBeat -> Color(0xFF252D3D)
                                        else -> StudioSurfaceVariant
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isPlayheadHere) Color.White else StudioBorder
                                    ),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { onStepClick(channel.id, stepIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${stepIdx + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.Black else StudioTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MixerTracksView(
    tracks: List<StudioAudioTrack>,
    onVolumeChange: (String, Float) -> Unit,
    onToggleMute: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            val color = Color(track.colorHex)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = track.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "${(track.volume * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyan
                            )
                        }

                        Slider(
                            value = track.volume,
                            onValueChange = { onVolumeChange(track.id, it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = color,
                                activeTrackColor = color,
                                inactiveTrackColor = StudioBorder
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onToggleMute(track.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (track.isMuted) StudioCoral else StudioTextSecondary
                        )
                    }
                }
            }
        }
    }
}
