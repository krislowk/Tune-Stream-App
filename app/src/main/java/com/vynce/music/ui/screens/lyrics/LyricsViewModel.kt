package com.vynce.music.ui.screens.lyrics

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.vynce.music.db.MusicDatabase
import com.vynce.music.db.entities.SyncedLyric
import com.vynce.music.repository.LyricRepository
import com.vynce.music.service.dsp.DspAudioProcessor
import com.vynce.music.service.dsp.DspEngine
import com.vynce.music.service.dsp.SynthAudioTrack
import com.vynce.music.service.dsp.WavWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LyricRepository
    val synthTrack = SynthAudioTrack()

    // Database flow
    val savedLyrics: StateFlow<List<SyncedLyric>>

    // Input States
    private val _rawLyrics = MutableStateFlow(SynthAudioTrack.DEMO_RAW_LYRICS)
    val rawLyrics: StateFlow<String> = _rawLyrics.asStateFlow()

    private val _songTitle = MutableStateFlow("DSP Synth Odyssey")
    val songTitle: StateFlow<String> = _songTitle.asStateFlow()

    private val _songArtist = MutableStateFlow("Acoustic Generator")
    val songArtist: StateFlow<String> = _songArtist.asStateFlow()

    private val _songDurationMs = MutableStateFlow(30000L) // Default 30s
    val songDurationMs: StateFlow<Long> = _songDurationMs.asStateFlow()

    // Current synchronized LRC output
    private val _generatedLrc = MutableStateFlow("")
    val generatedLrc: StateFlow<String> = _generatedLrc.asStateFlow()

    // List of parsed active timings for playback highlights [(TimeMs, Text)]
    private val _parsedLrcTimeline = MutableStateFlow<List<Pair<Long, String>>>(emptyList())
    val parsedLrcTimeline: StateFlow<List<Pair<Long, String>>> = _parsedLrcTimeline.asStateFlow()

    // Real-Time Playback and Timeline Cursor States (ExoPlayer-Driven)
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackMs = MutableStateFlow(0L)
    val playbackMs: StateFlow<Long> = _playbackMs.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    // DSP Waveform States
    private val _staticWaveform = MutableStateFlow<List<Float>>(emptyList())
    val staticWaveform: StateFlow<List<Float>> = _staticWaveform.asStateFlow()

    private val _dspOnsets = MutableStateFlow<List<Long>>(emptyList())
    val dspOnsets: StateFlow<List<Long>> = _dspOnsets.asStateFlow()

    // Microphone real-time alignment States
    private val _recordingActive = MutableStateFlow(false)
    val recordingActive: StateFlow<Boolean> = _recordingActive.asStateFlow()

    private val _realtimeAmplitude = MutableStateFlow(0f)
    val realtimeAmplitude: StateFlow<Float> = _realtimeAmplitude.asStateFlow()

    private val _micOnsetsDetected = MutableStateFlow<List<Long>>(emptyList())
    val micOnsetsDetected: StateFlow<List<Long>> = _micOnsetsDetected.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    // Sound source selection (true = Synthetic soundscape, false = Mic Recording)
    private val _useSyntheticTrack = MutableStateFlow(true)
    val useSyntheticTrack: StateFlow<Boolean> = _useSyntheticTrack.asStateFlow()

    // Tap sync status: current tapping progress lines
    private val _tapSyncProgress = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val tapSyncProgress: StateFlow<List<Pair<String, Long>>> = _tapSyncProgress.asStateFlow()

    private val _activeTapLineIndex = MutableStateFlow(0)
    val activeTapLineIndex: StateFlow<Int> = _activeTapLineIndex.asStateFlow()

    private val _tapSnappingFlashes = MutableStateFlow<String?>(null) // feedback text
    val tapSnappingFlashes: StateFlow<String?> = _tapSnappingFlashes.asStateFlow()

    // ExoPlayer and custom real-time audio components
    private val dspAudioProcessor = DspAudioProcessor()

    private val renderersFactory = object : DefaultRenderersFactory(application) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(dspAudioProcessor))
                .build()
        }
    }

    private val exoPlayer: ExoPlayer

    // File cache configurations for playback
    private val synthCacheFile = File(application.cacheDir, "synth_track.wav")
    private val micCacheFile = File(application.cacheDir, "recorded_track.wav")

    // Async thread jobs
    private var playheadJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val recordedShorts = ArrayList<Short>()

    init {
        // Room Connection
        val database = MusicDatabase.getInstance(application)
        repository = LyricRepository(database.databaseDao)
        savedLyrics = repository.allLyrics.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize custom ExoPlayer with integrated real-time audio analyzer
        exoPlayer = ExoPlayer.Builder(application, renderersFactory).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPlayheadPolling()
                } else {
                    stopPlayheadPolling()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    stopPlayheadPolling()
                    _playbackMs.value = _songDurationMs.value
                }
            }
        })

        // Hook real-time processor to monitor dynamic amplitude/energy levels
        dspAudioProcessor.onPcmFrameProcessed = { shortArray, size, _ ->
            val liveRms = DspEngine.calculateRms(shortArray, size)
            // Multiply for visual layout representation
            viewModelScope.launch(Dispatchers.Main) {
                _realtimeAmplitude.value = liveRms * 15f
            }
        }

        // Complete background IO prep tasks
        viewModelScope.launch(Dispatchers.Default) {
            // Write demo song to cache once so ExoPlayer can render it real-time
            if (!synthCacheFile.exists()) {
                val synthObj = SynthAudioTrack()
                WavWriter.writeWavFile(synthObj.fullBuffer, SynthAudioTrack.SAMPLE_RATE, synthCacheFile)
            }

            // Extract baseline static highlights
            extractStaticSynthWaveform()

            // Pre-compile basic heuristic LRC timings
            generateHeuristicsLrc()

            // Initialize player default track
            withContext(Dispatchers.Main) {
                preparePlayerWithFile(synthCacheFile)
            }
        }
    }

    /**
     * Prep play source
     */
    private fun preparePlayerWithFile(file: File) {
        if (!file.exists()) return
        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    /**
     * Playback Position Polling loop
     */
    private fun startPlayheadPolling() {
        playheadJob?.cancel()
        playheadJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                val currentPos = exoPlayer.currentPosition
                _playbackMs.value = currentPos
                updateCurrentLineIndex(currentPos)
                delay(33) // Low latency updates (~30fps refresh)
            }
        }
    }

    private fun stopPlayheadPolling() {
        playheadJob?.cancel()
        playheadJob = null
    }

    /**
     * Sets raw lyrics input
     */
    fun updateRawLyrics(newLyrics: String) {
        _rawLyrics.value = newLyrics
        resetTappingSession()
    }

    fun updateSongInfo(title: String, artist: String) {
        _songTitle.value = title
        _songArtist.value = artist
    }

    fun updateDuration(durationMs: Long) {
        _songDurationMs.value = durationMs
        resetTappingSession()
    }

    /**
     * Toggles play source: Synthetic Track vs Custom Live Recording
     */
    fun setSourceMode(useSynthetic: Boolean) {
        stopAllPlayback()
        _useSyntheticTrack.value = useSynthetic
        if (useSynthetic) {
            _songDurationMs.value = 30000L
            extractStaticSynthWaveform()
            preparePlayerWithFile(synthCacheFile)
        } else {
            _songDurationMs.value = 0L
            _staticWaveform.value = emptyList()
            _dspOnsets.value = emptyList()
            if (micCacheFile.exists()) {
                _songDurationMs.value = micCacheFile.length() / (16000 * 2) * 1000L // Estimate duration
                preparePlayerWithFile(micCacheFile)
            } else {
                exoPlayer.clearMediaItems()
            }
        }
        resetTappingSession()
    }

    /**
     * Analyzes PCM audio data and displays static waveform representation
     */
    private fun extractStaticSynthWaveform() {
        viewModelScope.launch(Dispatchers.Default) {
            val pcm = synthTrack.fullBuffer
            val features = DspEngine.analyzePcmData(pcm)

            val bins = 150
            val chunkSize = maxOf(1, features.size / bins)
            val downsampled = ArrayList<Float>(bins)
            val onsets = ArrayList<Long>()

            for (i in 0 until bins) {
                val start = i * chunkSize
                val end = minOf(features.size, start + chunkSize)
                if (start >= features.size) break
                var maxRms = 0f
                for (j in start until end) {
                    if (features[j].rms > maxRms) maxRms = features[j].rms
                }
                downsampled.add(maxRms)
            }

            features.forEach { feature ->
                if (feature.isOnset) {
                    onsets.add(feature.timestampMs)
                }
            }

            _staticWaveform.value = downsampled
            _dspOnsets.value = onsets
        }
    }

    /**
     * Playback Controls
     */
    fun togglePlayback() {
        if (_recordingActive.value) return
        if (exoPlayer.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        if (_songDurationMs.value <= 0L && !_useSyntheticTrack.value) return
        if (exoPlayer.currentPosition >= _songDurationMs.value) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
    }

    fun pausePlayback() {
        exoPlayer.pause()
    }

    fun stopAllPlayback() {
        exoPlayer.pause()
        exoPlayer.seekTo(0)
        _playbackMs.value = 0L
        _currentLineIndex.value = -1
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _songDurationMs.value)
        _playbackMs.value = clamped
        exoPlayer.seekTo(clamped)
        updateCurrentLineIndex(clamped)
    }

    private fun updateCurrentLineIndex(posMs: Long) {
        val list = _parsedLrcTimeline.value
        if (list.isEmpty()) {
            _currentLineIndex.value = -1
            return
        }
        var foundIndex = -1
        for (i in list.indices) {
            if (posMs >= list[i].first) {
                foundIndex = i
            } else {
                break
            }
        }
        _currentLineIndex.value = foundIndex
    }

    /**
     * Alignment Type 1: Distributed heuristics using vowel/punctuation logic
     */
    fun generateHeuristicsLrc() {
        viewModelScope.launch(Dispatchers.Default) {
            val lyrics = _rawLyrics.value
            val duration = if (_songDurationMs.value > 0L) _songDurationMs.value else 30000L
            val result = DspEngine.distributeTimestampsHeuristically(lyrics, duration)
            buildLrcString(result)
        }
    }

    /**
     * Alignment Type 2: DSP Peak/Onset Matcher (Auto-sync)
     */
    fun runAutoDspAlignment() {
        viewModelScope.launch(Dispatchers.Default) {
            val lines = _rawLyrics.value.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (lines.isEmpty()) return@launch

            val activeOnsets = if (_useSyntheticTrack.value) {
                _dspOnsets.value
            } else {
                _micOnsetsDetected.value
            }

            if (activeOnsets.isEmpty()) {
                generateHeuristicsLrc()
                return@launch
            }

            val alignedPairs = ArrayList<Pair<String, Long>>(lines.size)
            val duration = _songDurationMs.value
            val startBuffer = 1000L
            val availableDuration = duration - startBuffer - 1500L

            for (idx in lines.indices) {
                val relativeProgress = idx.toDouble() / lines.size
                val estimatedTime = (startBuffer + relativeProgress * availableDuration).toLong()

                // Find nearest spectral peak
                val thresholdDistance = 1500L
                val closestOnset = activeOnsets.filter { abs(it - estimatedTime) < thresholdDistance }
                    .minByOrNull { abs(it - estimatedTime) }

                val finalTime = closestOnset ?: estimatedTime
                alignedPairs.add(lines[idx] to finalTime)
            }

            buildLrcString(alignedPairs)
        }
    }

    /**
     * Alignment Type 3: Smart Tap-Snapping Assistant Live
     */
    fun startTappingSession() {
        stopAllPlayback()
        _activeTapLineIndex.value = 0
        val lines = _rawLyrics.value.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        _tapSyncProgress.value = lines.map { it to -1L }
        startPlayback()
    }

    fun recordTapAtCurrentTime() {
        val index = _activeTapLineIndex.value
        val progressList = _tapSyncProgress.value.toMutableList()
        val currentMs = _playbackMs.value

        if (index < progressList.size) {
            val currentOnsets = if (_useSyntheticTrack.value) _dspOnsets.value else _micOnsetsDetected.value
            val snappedMs = DspEngine.snapTapToOnset(currentMs, currentOnsets, windowMs = 350L)

            val correctionDiff = snappedMs - currentMs
            if (correctionDiff != 0L) {
                _tapSnappingFlashes.value = "Snapping Applied! Peak corrected by ${if (correctionDiff > 0) "+" else ""}${correctionDiff}ms"
            } else {
                _tapSnappingFlashes.value = "Registered."
            }

            viewModelScope.launch {
                delay(1200.milliseconds)
                if (_tapSnappingFlashes.value?.startsWith("Snapping") == true || _tapSnappingFlashes.value == "Registered.") {
                    _tapSnappingFlashes.value = null
                }
            }

            progressList[index] = progressList[index].first to snappedMs
            _tapSyncProgress.value = progressList

            val nextIndex = index + 1
            _activeTapLineIndex.value = nextIndex

            if (nextIndex >= progressList.size) {
                compileTappedLrc()
            }
        }
    }

    fun resetTappingSession() {
        _activeTapLineIndex.value = 0
        _tapSyncProgress.value = emptyList()
        _tapSnappingFlashes.value = null
    }

    fun compileTappedLrc() {
        val progress = _tapSyncProgress.value
        val validPairs = progress.filter { it.second != -1L }
        if (validPairs.isNotEmpty()) {
            buildLrcString(validPairs)
        }
    }

    private fun buildLrcString(timedLines: List<Pair<String, Long>>) {
        val sb = StringBuilder()
        sb.append("[ar:${_songArtist.value}]\n")
        sb.append("[ti:${_songTitle.value}]\n")
        sb.append("[by:Lyric Synchronizer Engine]\n")

        timedLines.forEach { (text, ms) ->
            val tag = DspEngine.formatLrcTimestamp(ms)
            sb.append("$tag $text\n")
        }

        val lrcResult = sb.toString()
        _generatedLrc.value = lrcResult
        _parsedLrcTimeline.value = DspEngine.parseLrc(lrcResult)
    }

    /**
     * Real-time microphone capture for local audio processing mapping
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startMicrophoneRecording() {
        if (_recordingActive.value) return
        stopAllPlayback()

        _recordingActive.value = true
        _recordingDurationMs.value = 0L
        _micOnsetsDetected.value = emptyList()
        _staticWaveform.value = emptyList()
        recordedShorts.clear()

        val sampleRate = DspEngine.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                maxOf(bufferSize, DspEngine.FRAME_SIZE * 2)
            ).apply {
                startRecording()
            }
        } catch (e: SecurityException) {
            _recordingActive.value = false
            _tapSnappingFlashes.value = "Microphone Permission Required!"
            return
        } catch (e: Exception) {
            _recordingActive.value = false
            _tapSnappingFlashes.value = "Failed to open mic recorder."
            return
        }

        recordJob = viewModelScope.launch(Dispatchers.Default) {
            val frameBuffer = ShortArray(DspEngine.FRAME_SIZE)
            var totalRecordSamples = 0L

            val localRecentDiffs = ArrayList<Float>()
            val noiseGateLinear = 0.005f
            val alphaMultiplier = 1.3f
            val minOnsetDelta = 0.008f
            var previousRms = 0f

            val liveWaveformPoints = ArrayList<Float>()

            while (_recordingActive.value) {
                val readResult = audioRecord?.read(frameBuffer, 0, DspEngine.FRAME_SIZE) ?: -1
                if (readResult > 0) {
                    val frameRms = DspEngine.calculateRms(frameBuffer, readResult)
                    _realtimeAmplitude.value = frameRms * 8f

                    for (i in 0 until readResult) {
                        recordedShorts.add(frameBuffer[i])
                    }

                    totalRecordSamples += readResult
                    val elapsedMs = (totalRecordSamples.toDouble() / sampleRate * 1000).toLong()
                    _recordingDurationMs.value = elapsedMs

                    val dbChange = frameRms - previousRms
                    val currentdE = if (dbChange > 0) dbChange else 0f
                    val isSilence = frameRms < noiseGateLinear

                    var isOnset = false
                    if (localRecentDiffs.size < 10) {
                        isOnset = currentdE > minOnsetDelta && !isSilence
                    } else {
                        val mean = localRecentDiffs.average().toFloat()
                        val variance = localRecentDiffs.map { (it - mean) * (it - mean) }.sum() / localRecentDiffs.size
                        val stdDevice = sqrt(variance)
                        val dynamicThreshold = mean + alphaMultiplier * stdDevice + minOnsetDelta

                        isOnset = currentdE > dynamicThreshold && !isSilence
                    }

                    localRecentDiffs.add(currentdE)
                    if (localRecentDiffs.size > 12) {
                        localRecentDiffs.removeAt(0)
                    }

                    if (isOnset) {
                        val lastDetected = _micOnsetsDetected.value.lastOrNull()
                        if (lastDetected == null || (elapsedMs - lastDetected) > 150L) {
                            _micOnsetsDetected.value = _micOnsetsDetected.value + elapsedMs
                        }
                    }

                    if (liveWaveformPoints.size < 150) {
                        liveWaveformPoints.add(frameRms)
                    } else {
                        liveWaveformPoints.removeAt(0)
                        liveWaveformPoints.add(frameRms)
                    }
                    _staticWaveform.value = liveWaveformPoints.toList()

                    previousRms = frameRms
                }
                delay(30)
            }
        }
    }

    fun stopMicrophoneRecording() {
        if (!_recordingActive.value) return
        _recordingActive.value = false
        recordJob?.cancel()
        recordJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("LyricSyncViewModel", "Error release AudioRecord", e)
        }
        audioRecord = null

        val durationMs = _recordingDurationMs.value
        _songDurationMs.value = durationMs

        viewModelScope.launch(Dispatchers.Default) {
            val shortsArray = recordedShorts.toShortArray()

            // Output standard WAV file for ExoPlayer
            WavWriter.writeWavFile(shortsArray, DspEngine.SAMPLE_RATE, micCacheFile)

            // Perform final offline DSP peak sweep over recorded file
            val features = DspEngine.analyzePcmData(shortsArray)
            val onsets = ArrayList<Long>()
            features.forEach { feature ->
                if (feature.isOnset) {
                    onsets.add(feature.timestampMs)
                }
            }

            _micOnsetsDetected.value = onsets
            _songTitle.value = "Mic Recording"
            _songArtist.value = "Live Vocalist"

            val bins = 150
            val chunkSize = maxOf(1, features.size / bins)
            val downsampled = ArrayList<Float>(bins)
            for (i in 0 until bins) {
                val start = i * chunkSize
                val end = minOf(features.size, start + chunkSize)
                if (start >= features.size) break
                var maxRms = 0f
                for (j in start until end) {
                    if (features[j].rms > maxRms) maxRms = features[j].rms
                }
                downsampled.add(maxRms)
            }
            _staticWaveform.value = downsampled

            withContext(Dispatchers.Main) {
                preparePlayerWithFile(micCacheFile)
            }

            generateHeuristicsLrc()
        }
    }

    /**
     * Database local saving functions (Room)
     */
    fun saveSyncedLyricsToLibrary() {
        viewModelScope.launch {
            if (_generatedLrc.value.isEmpty()) return@launch

            val syncedDoc = SyncedLyric(
                title = _songTitle.value,
                artist = _songArtist.value,
                rawLyrics = _rawLyrics.value,
                lrcContent = _generatedLrc.value,
                durationSec = (_songDurationMs.value / 1000).toInt()
            )
            repository.insert(syncedDoc)
            _tapSnappingFlashes.value = "Lyric Saved to Library Database!"
            delay(1200)
            _tapSnappingFlashes.value = null
        }
    }

    fun deleteSavedLyric(item: SyncedLyric) {
        viewModelScope.launch {
            repository.deleteById(item.id)
        }
    }

    fun loadLrcFromDocument(item: SyncedLyric) {
        _songTitle.value = item.title
        _songArtist.value = item.artist
        _rawLyrics.value = item.rawLyrics
        _generatedLrc.value = item.lrcContent
        _songDurationMs.value = item.durationSec * 1000L
        _parsedLrcTimeline.value = DspEngine.parseLrc(item.lrcContent)
        stopAllPlayback()
        _playbackMs.value = 0L

        if (item.title == "Mic Recording") {
            setSourceMode(false)
            _songDurationMs.value = item.durationSec * 1000L
        } else {
            setSourceMode(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
        stopPlayheadPolling()
        if (_recordingActive.value) {
            _recordingActive.value = false
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (ioe: Exception) {}
        }
    }
}