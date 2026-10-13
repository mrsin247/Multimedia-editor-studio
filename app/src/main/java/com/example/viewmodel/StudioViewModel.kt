package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SynthesizerEngine
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.data.SampleMediaRepository
import com.example.model.AspectRatioType
import com.example.model.ColorAdjustments
import com.example.model.CutoutTool
import com.example.model.LayerType
import com.example.model.PhotoLayer
import com.example.model.PointD
import com.example.model.ProjectItem
import com.example.model.SampleType
import com.example.model.SequencerChannel
import com.example.model.StudioAudioTrack
import com.example.model.StudioFilter
import com.example.model.StudioType
import com.example.model.VideoClip
import com.example.model.VideoOverlay
import com.example.photo.CutoutProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    HUB,
    PHOTO_STUDIO,
    AUDIO_STUDIO,
    VIDEO_STUDIO
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.projectDao()

    // ----------------------------------------------------
    // NAVIGATION & PROJECTS
    // ----------------------------------------------------
    private val _currentScreen = MutableStateFlow(AppScreen.HUB)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _projects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val projects: StateFlow<List<ProjectItem>> = _projects.asStateFlow()

    private val _activeProjectTitle = MutableStateFlow("Untitled Project")
    val activeProjectTitle: StateFlow<String> = _activeProjectTitle.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            projectDao.getAllProjects().collect { entities ->
                if (entities.isEmpty()) {
                    // Seed initial showcase projects
                    SampleMediaRepository.getStarterProjects().forEach { sample ->
                        projectDao.insertProject(ProjectEntity.fromProjectItem(sample))
                    }
                } else {
                    _projects.value = entities.map { it.toProjectItem() }
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen, title: String? = null) {
        if (title != null) _activeProjectTitle.value = title
        _currentScreen.value = screen
    }

    fun deleteProject(item: ProjectItem) {
        viewModelScope.launch(Dispatchers.IO) {
            projectDao.deleteProjectById(item.id)
        }
    }

    // ----------------------------------------------------
    // PHOTO STUDIO STATE & CUTOUT ENGINE
    // ----------------------------------------------------
    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap.asStateFlow()

    private val _photoLayers = MutableStateFlow<List<PhotoLayer>>(emptyList())
    val photoLayers: StateFlow<List<PhotoLayer>> = _photoLayers.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _cutoutTool = MutableStateFlow(CutoutTool.NONE)
    val cutoutTool: StateFlow<CutoutTool> = _cutoutTool.asStateFlow()

    private val _lassoPoints = MutableStateFlow<List<PointD>>(emptyList())
    val lassoPoints: StateFlow<List<PointD>> = _lassoPoints.asStateFlow()

    private val _colorAdjustments = MutableStateFlow(ColorAdjustments())
    val colorAdjustments: StateFlow<ColorAdjustments> = _colorAdjustments.asStateFlow()

    private val _studioFilter = MutableStateFlow(StudioFilter.NORMAL)
    val studioFilter: StateFlow<StudioFilter> = _studioFilter.asStateFlow()

    private var lastExtractedHoleRect: Rect? = null

    fun initPhotoStudio(templateIndex: Int = 0) {
        viewModelScope.launch(Dispatchers.Default) {
            val sampleBmp = SampleMediaRepository.createSamplePhoto(templateIndex)
            _backgroundBitmap.value = sampleBmp
            _photoLayers.value = listOf(
                PhotoLayer(
                    id = "layer_bg",
                    name = "Base Canvas",
                    bitmap = sampleBmp,
                    type = LayerType.BACKGROUND,
                    x = 0f,
                    y = 0f
                )
            )
            _selectedLayerId.value = "layer_bg"
            _cutoutTool.value = CutoutTool.NONE
            _lassoPoints.value = emptyList()
            _colorAdjustments.value = ColorAdjustments()
            _studioFilter.value = StudioFilter.NORMAL
        }
    }

    fun setCutoutTool(tool: CutoutTool) {
        _cutoutTool.value = tool
        _lassoPoints.value = emptyList()
    }

    fun addLassoPoint(point: PointD) {
        _lassoPoints.value = _lassoPoints.value + point
    }

    fun clearLasso() {
        _lassoPoints.value = emptyList()
    }

    /**
     * Executes the freehand lasso extraction without AI, producing a standalone floating layer!
     */
    fun performLassoCutout(viewWidth: Float, viewHeight: Float) {
        val bg = _backgroundBitmap.value ?: return
        val pts = _lassoPoints.value
        if (pts.size < 3) return

        viewModelScope.launch(Dispatchers.Default) {
            val result = CutoutProcessor.extractLassoAsset(bg, pts, viewWidth, viewHeight)
            if (result != null) {
                val (extractedBmp, bounds) = result
                val newLayerId = "cutout_${UUID.randomUUID().toString().take(6)}"
                val newLayer = PhotoLayer(
                    id = newLayerId,
                    name = "Cutout Asset #${_photoLayers.value.size}",
                    bitmap = extractedBmp,
                    type = LayerType.CUTOUT_OBJECT,
                    x = bounds.centerX(),
                    y = bounds.centerY(),
                    scale = 1.0f,
                    rotation = 0f
                )

                lastExtractedHoleRect = Rect(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.right.toInt(),
                    bounds.bottom.toInt()
                )

                _photoLayers.value = _photoLayers.value + newLayer
                _selectedLayerId.value = newLayerId
                _lassoPoints.value = emptyList()
                _cutoutTool.value = CutoutTool.NONE
            }
        }
    }

    /**
     * Executes Magic Wand / Color-Key segmentation on-device
     */
    fun performColorWandCutout(tapX: Float, tapY: Float, tolerance: Float = 45f) {
        val bg = _backgroundBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val extracted = CutoutProcessor.extractColorKeyAsset(bg, tapX, tapY, tolerance)
            val newLayerId = "wand_${UUID.randomUUID().toString().take(6)}"
            val newLayer = PhotoLayer(
                id = newLayerId,
                name = "Color Wand Layer",
                bitmap = extracted,
                type = LayerType.CUTOUT_OBJECT,
                x = 0f,
                y = 0f
            )
            _photoLayers.value = _photoLayers.value + newLayer
            _selectedLayerId.value = newLayerId
            _cutoutTool.value = CutoutTool.NONE
        }
    }

    /**
     * Inpaints / patches the hole where the extracted asset was lifted
     */
    fun inpaintLastCutoutHole() {
        val bg = _backgroundBitmap.value ?: return
        val hole = lastExtractedHoleRect ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val patched = CutoutProcessor.inpaintHole(bg, hole)
            _backgroundBitmap.value = patched
            // Update base layer bitmap
            _photoLayers.value = _photoLayers.value.map {
                if (it.id == "layer_bg") it.copy(bitmap = patched) else it
            }
            lastExtractedHoleRect = null
        }
    }

    fun updateSelectedLayerTransform(dx: Float, dy: Float, dScale: Float = 0f, dRot: Float = 0f) {
        val activeId = _selectedLayerId.value ?: return
        _photoLayers.value = _photoLayers.value.map { layer ->
            if (layer.id == activeId && layer.type != LayerType.BACKGROUND) {
                layer.copy(
                    x = layer.x + dx,
                    y = layer.y + dy,
                    scale = (layer.scale + dScale).coerceIn(0.2f, 4.0f),
                    rotation = (layer.rotation + dRot) % 360f
                )
            } else layer
        }
    }

    fun selectLayer(id: String) {
        _selectedLayerId.value = id
    }

    fun deleteSelectedLayer() {
        val activeId = _selectedLayerId.value ?: return
        if (activeId == "layer_bg") return // don't delete base
        _photoLayers.value = _photoLayers.value.filter { it.id != activeId }
        _selectedLayerId.value = "layer_bg"
    }

    fun duplicateSelectedLayer() {
        val activeId = _selectedLayerId.value ?: return
        val layer = _photoLayers.value.find { it.id == activeId } ?: return
        val copy = layer.copy(
            id = "layer_${UUID.randomUUID().toString().take(6)}",
            name = "${layer.name} (Copy)",
            x = layer.x + 40f,
            y = layer.y + 40f
        )
        _photoLayers.value = _photoLayers.value + copy
        _selectedLayerId.value = copy.id
    }

    fun addTextLayer(text: String = "Miller Studio") {
        val textLayer = PhotoLayer(
            id = "text_${UUID.randomUUID().toString().take(6)}",
            name = "Text: $text",
            type = LayerType.TEXT,
            text = text,
            x = 200f,
            y = 200f,
            textColor = 0xFF00E5FF,
            textSize = 52f
        )
        _photoLayers.value = _photoLayers.value + textLayer
        _selectedLayerId.value = textLayer.id
    }

    fun setAdjustments(adj: ColorAdjustments) {
        _colorAdjustments.value = adj
    }

    fun setFilter(filter: StudioFilter) {
        _studioFilter.value = filter
    }

    fun savePhotoProject() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = _photoLayers.value.size
            val project = ProjectItem(
                id = UUID.randomUUID().toString(),
                title = _activeProjectTitle.value,
                type = StudioType.PHOTO,
                updatedAt = System.currentTimeMillis(),
                description = "Custom cutout composition with ${_studioFilter.value.name.lowercase()} filter",
                durationOrLayers = "$count Layers"
            )
            projectDao.insertProject(ProjectEntity.fromProjectItem(project))
        }
    }

    // ----------------------------------------------------
    // AUDIO STUDIO STATE & SEQUENCER ENGINE
    // ----------------------------------------------------
    private val _bpm = MutableStateFlow(130)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _currentSequencerStep = MutableStateFlow(0)
    val currentSequencerStep: StateFlow<Int> = _currentSequencerStep.asStateFlow()

    private val _sequencerChannels = MutableStateFlow<List<SequencerChannel>>(
        listOf(
            SequencerChannel(
                id = "kick",
                sampleType = SampleType.KICK,
                // Default 4-on-the-floor beat with 808 syncopation
                steps = listOf(true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false)
            ),
            SequencerChannel(
                id = "snare",
                sampleType = SampleType.SNARE,
                // Claps on 5 and 13
                steps = listOf(false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false)
            ),
            SequencerChannel(
                id = "hihat",
                sampleType = SampleType.HIHAT_CLOSED,
                // 8th-note hats
                steps = listOf(true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false)
            ),
            SequencerChannel(
                id = "sub808",
                sampleType = SampleType.SUB_808,
                steps = listOf(true, false, false, true, false, false, false, false, true, false, false, false, false, true, false, false)
            ),
            SequencerChannel(
                id = "pluck",
                sampleType = SampleType.SYNTH_PLUCK,
                steps = listOf(false, false, true, false, false, true, false, false, false, false, true, false, false, false, true, false)
            )
        )
    )
    val sequencerChannels: StateFlow<List<SequencerChannel>> = _sequencerChannels.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<StudioAudioTrack>>(
        listOf(
            StudioAudioTrack(id = "trk_drums", name = "808 Drums Master", volume = 0.9f, colorHex = 0xFFFF5252, durationSec = 4f),
            StudioAudioTrack(id = "trk_bass", name = "Sub Bass 45Hz", volume = 0.85f, colorHex = 0xFFB388FF, durationSec = 4f),
            StudioAudioTrack(id = "trk_melody", name = "Synth Arp Pluck", volume = 0.75f, colorHex = 0xFF00E676, durationSec = 4f),
            StudioAudioTrack(id = "trk_fx", name = "Vocal Chops / FX", volume = 0.65f, colorHex = 0xFF00E5FF, durationSec = 4f)
        )
    )
    val audioTracks: StateFlow<List<StudioAudioTrack>> = _audioTracks.asStateFlow()

    private var sequencerJob: Job? = null

    fun triggerDrumPad(type: SampleType) {
        SynthesizerEngine.playSample(type)
    }

    fun toggleSequencerStep(channelId: String, stepIndex: Int) {
        _sequencerChannels.value = _sequencerChannels.value.map { channel ->
            if (channel.id == channelId) {
                val updatedSteps = channel.steps.toMutableList()
                updatedSteps[stepIndex] = !updatedSteps[stepIndex]
                channel.copy(steps = updatedSteps)
            } else channel
        }
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(60, 200)
    }

    fun toggleAudioPlayback() {
        if (_isAudioPlaying.value) {
            stopAudioPlayback()
        } else {
            startAudioPlayback()
        }
    }

    private fun startAudioPlayback() {
        _isAudioPlaying.value = true
        sequencerJob?.cancel()
        sequencerJob = viewModelScope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && _isAudioPlaying.value) {
                _currentSequencerStep.value = step

                // Trigger samples scheduled on this step
                val channels = _sequencerChannels.value
                channels.forEach { ch ->
                    if (ch.steps.getOrElse(step) { false }) {
                        SynthesizerEngine.playSample(ch.sampleType)
                    }
                }

                // Calculate step duration in milliseconds: 60,000 / BPM / 4 (for 16th notes)
                val stepIntervalMs = (60_000L / _bpm.value) / 4
                delay(stepIntervalMs)
                step = (step + 1) % 16
            }
        }
    }

    fun stopAudioPlayback() {
        _isAudioPlaying.value = false
        sequencerJob?.cancel()
        sequencerJob = null
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        _audioTracks.value = _audioTracks.value.map {
            if (it.id == trackId) it.copy(volume = volume) else it
        }
    }

    fun toggleTrackMute(trackId: String) {
        _audioTracks.value = _audioTracks.value.map {
            if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
        }
    }

    fun saveAudioProject() {
        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectItem(
                id = UUID.randomUUID().toString(),
                title = _activeProjectTitle.value,
                type = StudioType.AUDIO,
                updatedAt = System.currentTimeMillis(),
                description = "16-step sequence at ${_bpm.value} BPM with 808 drum engine",
                durationOrLayers = "${_bpm.value} BPM • ${_sequencerChannels.value.size} Channels"
            )
            projectDao.insertProject(ProjectEntity.fromProjectItem(project))
        }
    }

    // ----------------------------------------------------
    // VIDEO STUDIO STATE & TIMELINE ENGINE
    // ----------------------------------------------------
    private val _videoClips = MutableStateFlow<List<VideoClip>>(
        listOf(
            VideoClip(
                id = "clip_1",
                title = "Intro Skyline",
                totalDurationMs = 6000L,
                trimStartMs = 0L,
                trimEndMs = 5000L,
                speed = 1.0f,
                filter = StudioFilter.CYBERPUNK,
                accentColor = 0xFF00E5FF,
                sceneName = "City Drone Intro"
            ),
            VideoClip(
                id = "clip_2",
                title = "Main Beat Drop",
                totalDurationMs = 8000L,
                trimStartMs = 1000L,
                trimEndMs = 7000L,
                speed = 1.0f,
                filter = StudioFilter.VIVID_POP,
                accentColor = 0xFFFF5252,
                sceneName = "Club Lights & Drop"
            ),
            VideoClip(
                id = "clip_3",
                title = "Outro Horizon",
                totalDurationMs = 5000L,
                trimStartMs = 500L,
                trimEndMs = 4500L,
                speed = 0.8f,
                filter = StudioFilter.GOLDEN_HOUR,
                accentColor = 0xFFFFB300,
                sceneName = "Golden Hour Outro"
            )
        )
    )
    val videoClips: StateFlow<List<VideoClip>> = _videoClips.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>("clip_1")
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioType.PORTRAIT) // 9:16 default for mobile
    val aspectRatio: StateFlow<AspectRatioType> = _aspectRatio.asStateFlow()

    private val _videoPlayheadMs = MutableStateFlow(0L)
    val videoPlayheadMs: StateFlow<Long> = _videoPlayheadMs.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private val _videoOverlays = MutableStateFlow<List<VideoOverlay>>(
        listOf(
            VideoOverlay(
                id = "ov_title",
                text = "MILLER SUITE EXCLUSIVE",
                startMs = 500L,
                endMs = 4500L,
                posX = 0.5f,
                posY = 0.25f
            ),
            VideoOverlay(
                id = "ov_watermark",
                text = "⚡ 100% OFFLINE",
                startMs = 0L,
                endMs = 16000L,
                posX = 0.5f,
                posY = 0.85f,
                isWatermark = true
            )
        )
    )
    val videoOverlays: StateFlow<List<VideoOverlay>> = _videoOverlays.asStateFlow()

    private var videoPlaybackJob: Job? = null

    val totalVideoDurationMs: Long
        get() = _videoClips.value.sumOf { it.activeDurationMs }

    fun selectClip(clipId: String) {
        _selectedClipId.value = clipId
    }

    fun updateClipTrim(clipId: String, startMs: Long, endMs: Long) {
        _videoClips.value = _videoClips.value.map { clip ->
            if (clip.id == clipId) {
                val validStart = startMs.coerceIn(0L, clip.totalDurationMs - 500L)
                val validEnd = endMs.coerceIn(validStart + 500L, clip.totalDurationMs)
                clip.copy(trimStartMs = validStart, trimEndMs = validEnd)
            } else clip
        }
    }

    fun setClipSpeed(clipId: String, speed: Float) {
        _videoClips.value = _videoClips.value.map {
            if (it.id == clipId) it.copy(speed = speed) else it
        }
    }

    fun setClipFilter(clipId: String, filter: StudioFilter) {
        _videoClips.value = _videoClips.value.map {
            if (it.id == clipId) it.copy(filter = filter) else it
        }
    }

    fun addVideoClip(title: String = "New Scene") {
        val newClip = VideoClip(
            id = "clip_${UUID.randomUUID().toString().take(6)}",
            title = title,
            totalDurationMs = 5000L,
            trimStartMs = 0L,
            trimEndMs = 5000L,
            speed = 1.0f,
            accentColor = 0xFF00E676,
            sceneName = title
        )
        _videoClips.value = _videoClips.value + newClip
        _selectedClipId.value = newClip.id
    }

    fun deleteSelectedClip() {
        val selectedId = _selectedClipId.value ?: return
        if (_videoClips.value.size <= 1) return // keep at least 1 clip
        _videoClips.value = _videoClips.value.filter { it.id != selectedId }
        _selectedClipId.value = _videoClips.value.firstOrNull()?.id
    }

    fun setAspectRatio(type: AspectRatioType) {
        _aspectRatio.value = type
    }

    fun seekVideo(ms: Long) {
        _videoPlayheadMs.value = ms.coerceIn(0L, totalVideoDurationMs.coerceAtLeast(100L))
    }

    fun toggleVideoPlayback() {
        if (_isVideoPlaying.value) {
            stopVideoPlayback()
        } else {
            startVideoPlayback()
        }
    }

    private fun startVideoPlayback() {
        _isVideoPlaying.value = true
        videoPlaybackJob?.cancel()
        videoPlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            val total = totalVideoDurationMs.coerceAtLeast(100L)
            while (isActive && _isVideoPlaying.value) {
                var current = _videoPlayheadMs.value + 50L
                if (current >= total) {
                    current = 0L // loop
                }
                _videoPlayheadMs.value = current
                delay(50L)
            }
        }
    }

    fun stopVideoPlayback() {
        _isVideoPlaying.value = false
        videoPlaybackJob?.cancel()
        videoPlaybackJob = null
    }

    fun addVideoOverlay(text: String) {
        val overlay = VideoOverlay(
            id = "ov_${UUID.randomUUID().toString().take(6)}",
            text = text,
            startMs = _videoPlayheadMs.value,
            endMs = (_videoPlayheadMs.value + 3000L).coerceAtMost(totalVideoDurationMs),
            posX = 0.5f,
            posY = 0.5f
        )
        _videoOverlays.value = _videoOverlays.value + overlay
    }

    fun saveVideoProject() {
        viewModelScope.launch(Dispatchers.IO) {
            val totalSec = totalVideoDurationMs / 1000L
            val project = ProjectItem(
                id = UUID.randomUUID().toString(),
                title = _activeProjectTitle.value,
                type = StudioType.VIDEO,
                updatedAt = System.currentTimeMillis(),
                description = "${_videoClips.value.size} clips sequenced with ${_aspectRatio.value.label}",
                durationOrLayers = "${totalSec}s • ${_videoClips.value.size} Clips"
            )
            projectDao.insertProject(ProjectEntity.fromProjectItem(project))
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayback()
        stopVideoPlayback()
    }
}
