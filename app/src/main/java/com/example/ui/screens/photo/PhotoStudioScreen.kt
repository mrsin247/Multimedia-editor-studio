package com.example.ui.screens.photo

import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CutoutTool
import com.example.model.LayerType
import com.example.model.PhotoLayer
import com.example.model.PointD
import com.example.model.StudioFilter
import com.example.photo.CutoutProcessor
import com.example.ui.components.StudioSlider
import com.example.ui.components.StudioToolButton
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

@Composable
fun PhotoStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HUB)
    }

    val title by viewModel.activeProjectTitle.collectAsState()
    val bgBitmap by viewModel.backgroundBitmap.collectAsState()
    val layers by viewModel.photoLayers.collectAsState()
    val selectedId by viewModel.selectedLayerId.collectAsState()
    val tool by viewModel.cutoutTool.collectAsState()
    val lassoPoints by viewModel.lassoPoints.collectAsState()
    val adjustments by viewModel.colorAdjustments.collectAsState()
    val filter by viewModel.studioFilter.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostStateState() }
    val scope = rememberCoroutineScope()

    var canvasWidth by remember { mutableStateOf(800f) }
    var canvasHeight by remember { mutableStateOf(800f) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_studio_screen"),
        topBar = {
            StudioTopBar(
                title = title,
                subtitle = "Cutout & Multi-Layer Studio",
                onBack = { viewModel.navigateTo(AppScreen.HUB) },
                onSave = {
                    viewModel.savePhotoProject()
                    scope.launch {
                        snackbarHostState.showSnackbar("Project saved locally with ${layers.size} layers!")
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
            // Cutout Prompt Banner when in Lasso mode
            if (tool == CutoutTool.FREEHAND_LASSO) {
                LassoInstructionBanner(
                    pointCount = lassoPoints.size,
                    onExtract = {
                        viewModel.performLassoCutout(canvasWidth, canvasHeight)
                        scope.launch {
                            snackbarHostState.showSnackbar("Extracted asset into new layer! Drag to move it.")
                        }
                    },
                    onCancel = { viewModel.clearLasso(); viewModel.setCutoutTool(CutoutTool.NONE) }
                )
            } else if (tool == CutoutTool.MAGIC_WAND) {
                MagicWandBanner(
                    onCancel = { viewModel.setCutoutTool(CutoutTool.NONE) }
                )
            }

            // Interactive Studio Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF090D12))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(100f)
                    canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(100f)

                    val colorFilter = remember(adjustments, filter) {
                        CutoutProcessor.createCombinedFilter(adjustments, filter)
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(tool, selectedId) {
                                when (tool) {
                                    CutoutTool.FREEHAND_LASSO -> {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                viewModel.clearLasso()
                                                viewModel.addLassoPoint(PointD(offset.x, offset.y))
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                viewModel.addLassoPoint(PointD(change.position.x, change.position.y))
                                            }
                                        )
                                    }
                                    CutoutTool.MAGIC_WAND -> {
                                        detectTapGestures { tapOffset ->
                                            viewModel.performColorWandCutout(tapOffset.x, tapOffset.y)
                                        }
                                    }
                                    else -> {
                                        // Drag selected asset layer to move it around!
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            viewModel.updateSelectedLayerTransform(dragAmount.x, dragAmount.y)
                                        }
                                    }
                                }
                            }
                    ) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // 1. Draw Background Layer with adjustments
                        bgBitmap?.let { bmp ->
                            drawContext.canvas.nativeCanvas.save()
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                            paint.colorFilter = colorFilter
                            val src = Rect(0, 0, bmp.width, bmp.height)
                            val dst = Rect(0, 0, canvasW.toInt(), canvasH.toInt())
                            drawContext.canvas.nativeCanvas.drawBitmap(bmp, src, dst, paint)
                            drawContext.canvas.nativeCanvas.restore()
                        }

                        // 2. Draw Extracted Cutout / Object Layers
                        layers.forEach { layer ->
                            if (layer.isVisible && layer.type != LayerType.BACKGROUND) {
                                when (layer.type) {
                                    LayerType.CUTOUT_OBJECT -> {
                                        layer.bitmap?.let { objBmp ->
                                            drawContext.canvas.nativeCanvas.save()
                                            drawContext.canvas.nativeCanvas.translate(layer.x, layer.y)
                                            drawContext.canvas.nativeCanvas.rotate(layer.rotation)
                                            drawContext.canvas.nativeCanvas.scale(layer.scale, layer.scale)

                                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                                            paint.alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)
                                            val halfW = objBmp.width / 2f
                                            val halfH = objBmp.height / 2f
                                            drawContext.canvas.nativeCanvas.drawBitmap(objBmp, -halfW, -halfH, paint)

                                            // Draw selection outline if selected
                                            if (layer.id == selectedId) {
                                                val selPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                                                selPaint.color = android.graphics.Color.CYAN
                                                selPaint.style = Paint.Style.STROKE
                                                selPaint.strokeWidth = 3f
                                                drawContext.canvas.nativeCanvas.drawRect(-halfW, -halfH, halfW, halfH, selPaint)
                                            }

                                            drawContext.canvas.nativeCanvas.restore()
                                        }
                                    }
                                    LayerType.TEXT -> {
                                        drawContext.canvas.nativeCanvas.save()
                                        drawContext.canvas.nativeCanvas.translate(layer.x, layer.y)
                                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                            color = layer.textColor.toInt()
                                            textSize = layer.textSize * layer.scale
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                            setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
                                        }
                                        drawContext.canvas.nativeCanvas.drawText(layer.text, 0f, 0f, textPaint)
                                        drawContext.canvas.nativeCanvas.restore()
                                    }
                                    else -> {}
                                }
                            }
                        }

                        // 3. Draw Freehand Lasso Path in real-time
                        if (tool == CutoutTool.FREEHAND_LASSO && lassoPoints.size > 1) {
                            val path = Path()
                            path.moveTo(lassoPoints.first().x, lassoPoints.first().y)
                            for (i in 1 until lassoPoints.size) {
                                path.lineTo(lassoPoints[i].x, lassoPoints[i].y)
                            }
                            drawPath(
                                path = path,
                                color = StudioAmber,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            // Draw dots at vertices
                            lassoPoints.takeLast(10).forEach { pt ->
                                drawCircle(
                                    color = StudioCyan,
                                    radius = 4.dp.toPx(),
                                    center = Offset(pt.x, pt.y)
                                )
                            }
                        }
                    }
                }
            }

            // Layer Quick Status & Transform Bar (Move, Scale, Rotate, Inpaint)
            SelectedLayerToolbar(
                selectedLayer = layers.find { it.id == selectedId },
                onScaleUp = { viewModel.updateSelectedLayerTransform(0f, 0f, 0.15f, 0f) },
                onScaleDown = { viewModel.updateSelectedLayerTransform(0f, 0f, -0.15f, 0f) },
                onRotate = { viewModel.updateSelectedLayerTransform(0f, 0f, 0f, 15f) },
                onDuplicate = { viewModel.duplicateSelectedLayer() },
                onDelete = { viewModel.deleteSelectedLayer() },
                onInpaint = {
                    viewModel.inpaintLastCutoutHole()
                    scope.launch {
                        snackbarHostState.showSnackbar("Synthesized background patch to fill hole!")
                    }
                }
            )

            // Studio Control Tabs
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
                    text = { Text("Cutout & Move", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Layers (${layers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Color Grade", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(StudioSurface)
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> CutoutToolsPanel(
                        currentTool = tool,
                        onSelectLasso = { viewModel.setCutoutTool(CutoutTool.FREEHAND_LASSO) },
                        onSelectWand = { viewModel.setCutoutTool(CutoutTool.MAGIC_WAND) },
                        onSelectMove = { viewModel.setCutoutTool(CutoutTool.NONE) },
                        onAddText = { viewModel.addTextLayer("MILLER SUITE") }
                    )
                    1 -> LayersPanel(
                        layers = layers,
                        selectedId = selectedId,
                        onSelectLayer = { viewModel.selectLayer(it) },
                        onAddText = { viewModel.addTextLayer() }
                    )
                    2 -> ColorGradePanel(
                        adjustments = adjustments,
                        onAdjustmentsChange = { viewModel.setAdjustments(it) }
                    )
                    3 -> FilterPresetsPanel(
                        currentFilter = filter,
                        onSelectFilter = { viewModel.setFilter(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LassoInstructionBanner(
    pointCount: Int,
    onExtract: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF332A00)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gesture,
                    contentDescription = null,
                    tint = StudioAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (pointCount < 3) "Trace around object with your finger" else "$pointCount points traced",
                    fontSize = 12.sp,
                    color = StudioAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioTextSecondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Cancel", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExtract,
                    enabled = pointCount >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Extract Asset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MagicWandBanner(onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF002B30)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tap any color on the canvas to isolate that color island",
                fontSize = 12.sp,
                color = StudioCyan,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = StudioCyan)
            }
        }
    }
}

@Composable
private fun SelectedLayerToolbar(
    selectedLayer: PhotoLayer?,
    onScaleUp: () -> Unit,
    onScaleDown: () -> Unit,
    onRotate: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onInpaint: () -> Unit
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
                text = selectedLayer?.name ?: "No Layer Selected",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedLayer?.type != LayerType.BACKGROUND) StudioCyan else StudioTextSecondary
            )

            if (selectedLayer != null && selectedLayer.type != LayerType.BACKGROUND) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Scale -
                    IconButton(onClick = onScaleDown, modifier = Modifier.size(32.dp)) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                    }
                    Text("Size", fontSize = 11.sp, color = StudioTextSecondary)
                    // Scale +
                    IconButton(onClick = onScaleUp, modifier = Modifier.size(32.dp)) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                    }
                    // Rotate
                    IconButton(onClick = onRotate, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotate", tint = StudioAmber, modifier = Modifier.size(18.dp))
                    }
                    // Duplicate
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = StudioEmerald, modifier = Modifier.size(18.dp))
                    }
                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = StudioCoral, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Drag canvas to reposition assets",
                        fontSize = 11.sp,
                        color = StudioTextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onInpaint,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Healing, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Patch Hole", fontSize = 10.sp, color = StudioEmerald)
                    }
                }
            }
        }
    }
}

@Composable
private fun CutoutToolsPanel(
    currentTool: CutoutTool,
    onSelectLasso: () -> Unit,
    onSelectWand: () -> Unit,
    onSelectMove: () -> Unit,
    onAddText: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioToolButton(
            icon = Icons.Default.Gesture,
            label = "Freehand Lasso",
            isSelected = currentTool == CutoutTool.FREEHAND_LASSO,
            onClick = onSelectLasso,
            accentColor = StudioAmber
        )

        StudioToolButton(
            icon = Icons.Default.AutoAwesome,
            label = "Magic Wand",
            isSelected = currentTool == CutoutTool.MAGIC_WAND,
            onClick = onSelectWand,
            accentColor = StudioCyan
        )

        StudioToolButton(
            icon = Icons.Default.OpenWith,
            label = "Move Asset",
            isSelected = currentTool == CutoutTool.NONE,
            onClick = onSelectMove,
            accentColor = StudioEmerald
        )

        StudioToolButton(
            icon = Icons.Default.TextFields,
            label = "Add Text",
            isSelected = false,
            onClick = onAddText,
            accentColor = StudioPurple
        )
    }
}

@Composable
private fun LayersPanel(
    layers: List<PhotoLayer>,
    selectedId: String?,
    onSelectLayer: (String) -> Unit,
    onAddText: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(layers) { layer ->
            val isSelected = layer.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) StudioCyan.copy(alpha = 0.15f) else StudioCard)
                    .border(
                        BorderStroke(1.dp, if (isSelected) StudioCyan else StudioBorder),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectLayer(layer.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (layer.type == LayerType.BACKGROUND) Icons.Default.Layers else Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = if (isSelected) StudioCyan else StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = layer.name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = StudioTextPrimary
                    )
                }

                Text(
                    text = if (layer.type == LayerType.BACKGROUND) "Locked Base" else "Cutout (${layer.scale.let { "%.1fx".format(it) }})",
                    fontSize = 10.sp,
                    color = StudioTextMuted
                )
            }
        }
    }
}

@Composable
private fun ColorGradePanel(
    adjustments: com.example.model.ColorAdjustments,
    onAdjustmentsChange: (com.example.model.ColorAdjustments) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StudioSlider(
                label = "Brightness",
                value = adjustments.brightness,
                onValueChange = { onAdjustmentsChange(adjustments.copy(brightness = it)) },
                valueRange = -60f..60f,
                displayValue = "${adjustments.brightness.toInt()}"
            )
        }
        item {
            StudioSlider(
                label = "Contrast",
                value = adjustments.contrast,
                onValueChange = { onAdjustmentsChange(adjustments.copy(contrast = it)) },
                valueRange = 0.5f..1.8f,
                displayValue = "%.2f".format(adjustments.contrast)
            )
        }
        item {
            StudioSlider(
                label = "Saturation",
                value = adjustments.saturation,
                onValueChange = { onAdjustmentsChange(adjustments.copy(saturation = it)) },
                valueRange = 0.0f..2.0f,
                displayValue = "%.2f".format(adjustments.saturation)
            )
        }
        item {
            StudioSlider(
                label = "Warmth",
                value = adjustments.warmth,
                onValueChange = { onAdjustmentsChange(adjustments.copy(warmth = it)) },
                valueRange = -40f..40f,
                displayValue = "${adjustments.warmth.toInt()}"
            )
        }
    }
}

@Composable
private fun FilterPresetsPanel(
    currentFilter: StudioFilter,
    onSelectFilter: (StudioFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(StudioFilter.values()) { preset ->
            val isSelected = preset == currentFilter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) StudioCyan.copy(alpha = 0.2f) else StudioCard)
                    .border(
                        BorderStroke(1.dp, if (isSelected) StudioCyan else StudioBorder),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectFilter(preset) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = preset.name,
                    tint = if (isSelected) StudioCyan else StudioTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = preset.name.replace("_", " "),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) StudioCyan else StudioTextPrimary
                )
            }
        }
    }
}

private fun SnackbarHostStateState(): SnackbarHostState = SnackbarHostState()
