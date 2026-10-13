package com.example.ui.screens.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProjectItem
import com.example.model.StudioType
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HubScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .testTag("hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Studio Banner
        item {
            HeroHeaderCard()
        }

        // Three Core Studio Launchers
        item {
            Text(
                text = "CREATIVE SUITES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = StudioTextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioLauncherCard(
                    title = "Photo Studio",
                    subtitle = "Lasso Cutout • Magic Wand • Multi-Layers • Inpaint",
                    icon = Icons.Default.Palette,
                    accentColor = StudioAmber,
                    badgeText = "ZERO AI • 100% LOCAL",
                    testTag = "launch_photo_studio",
                    onClick = {
                        viewModel.initPhotoStudio(0)
                        viewModel.navigateTo(AppScreen.PHOTO_STUDIO, "Neon Cutout Master")
                    }
                )

                StudioLauncherCard(
                    title = "Audio & Music Studio",
                    subtitle = "808 MPC Pads • 16-Step Sequencer • Multi-Track Mixer",
                    icon = Icons.Default.Audiotrack,
                    accentColor = StudioCyan,
                    badgeText = "SYNTH ENGINE",
                    testTag = "launch_audio_studio",
                    onClick = {
                        viewModel.navigateTo(AppScreen.AUDIO_STUDIO, "Midnight Beat 130 BPM")
                    }
                )

                StudioLauncherCard(
                    title = "Video Studio",
                    subtitle = "Multi-Clip Timeline • Dual-Handle Trimmer • 9:16 Shorts",
                    icon = Icons.Default.Movie,
                    accentColor = StudioCoral,
                    badgeText = "REALTIME TIMELINE",
                    testTag = "launch_video_studio",
                    onClick = {
                        viewModel.navigateTo(AppScreen.VIDEO_STUDIO, "Cinematic Sequence")
                    }
                )
            }
        }

        // Quick Starter Photo Templates
        item {
            Text(
                text = "QUICK PHOTO TEMPLATES (FOR OBJECT CUTOUT)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = StudioTextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TemplateChip(
                        title = "Cyberpunk Skyline",
                        subtitle = "Neon moon & crystal asset",
                        gradient = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)),
                        onClick = {
                            viewModel.initPhotoStudio(0)
                            viewModel.navigateTo(AppScreen.PHOTO_STUDIO, "Cyberpunk Canvas")
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "Retro Synthwave",
                        subtitle = "Striped sun & mountains",
                        gradient = listOf(Color(0xFFFF5E62), Color(0xFFFF9966), Color(0xFF2C3E50)),
                        onClick = {
                            viewModel.initPhotoStudio(1)
                            viewModel.navigateTo(AppScreen.PHOTO_STUDIO, "Retro Sun Canvas")
                        }
                    )
                }
                item {
                    TemplateChip(
                        title = "Golden Studio Sphere",
                        subtitle = "Glowing orb & torus ring",
                        gradient = listOf(Color(0xFF212529), Color(0xFFFFB300), Color(0xFF121212)),
                        onClick = {
                            viewModel.initPhotoStudio(2)
                            viewModel.navigateTo(AppScreen.PHOTO_STUDIO, "Golden Sphere Canvas")
                        }
                    )
                }
            }
        }

        // Saved Local Projects
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL PROJECTS (${projects.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = StudioTextSecondary
                )
                Text(
                    text = "Room SQLite Persisted",
                    fontSize = 11.sp,
                    color = StudioCyan
                )
            }
        }

        items(projects, key = { it.id }) { item ->
            ProjectRowCard(
                project = item,
                onOpen = {
                    when (item.type) {
                        StudioType.PHOTO -> {
                            viewModel.initPhotoStudio(0)
                            viewModel.navigateTo(AppScreen.PHOTO_STUDIO, item.title)
                        }
                        StudioType.AUDIO -> {
                            viewModel.navigateTo(AppScreen.AUDIO_STUDIO, item.title)
                        }
                        StudioType.VIDEO -> {
                            viewModel.navigateTo(AppScreen.VIDEO_STUDIO, item.title)
                        }
                    }
                },
                onDelete = {
                    viewModel.deleteProject(item)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2638),
                            StudioCard
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MILLER'S MASTER MAKER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = StudioTextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StudioEmerald.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, StudioEmerald), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Offline indicator",
                            tint = StudioEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OFFLINE SUITE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Professional offline creative production engine. Extract & move photo assets without AI, compose 808 beats with low-latency PCM audio, and sequence multi-clip videos on-device.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = StudioTextSecondary
                )
            }
        }
    }
}

@Composable
private fun StudioLauncherCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, accentColor), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StudioTextSecondary,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Open",
                tint = StudioCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TemplateChip(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradient))
            .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "CANVAS READY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCyan
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ProjectRowCard(
    project: ProjectItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, color) = when (project.type) {
        StudioType.PHOTO -> Pair(Icons.Default.Image, StudioAmber)
        StudioType.AUDIO -> Pair(Icons.Default.GraphicEq, StudioCyan)
        StudioType.VIDEO -> Pair(Icons.Default.Movie, StudioCoral)
    }

    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(project.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("project_item_${project.id}"),
        shape = RoundedCornerShape(12.dp),
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = project.type.name,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = project.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "• ${project.durationOrLayers}",
                        fontSize = 11.sp,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$dateStr • ${project.description}",
                    fontSize = 11.sp,
                    color = StudioTextMuted,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Project",
                    tint = StudioTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
