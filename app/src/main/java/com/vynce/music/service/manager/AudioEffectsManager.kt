package com.vynce.music.service.manager

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.vynce.music.repository.PreferenceRepository
import com.vynce.music.repository.constants.PreferenceConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
class AudioEffectsManager(
    private val player: ExoPlayer,
    private val preferenceRepository: PreferenceRepository,
    private val scope: CoroutineScope
) {
    private var fadeJob: Job? = null
    private var fadeOutStarted = false
    private var currentBaseVolume = 1.0f
    private var currentFadeMultiplier = 1.0f

    init {
        observePreferences()
        setupPlayerListener()
        startCrossfadePoller()
    }

    private fun observePreferences() {
        scope.launch {
            preferenceRepository.get(PreferenceConstants.SKIP_SILENCE).collectLatest { enabled ->
                player.skipSilenceEnabled = enabled ?: false
            }
        }

        scope.launch {
            preferenceRepository.get(PreferenceConstants.PLAYBACK_SPEED).collectLatest { speed ->
                player.setPlaybackSpeed(speed ?: 1.0f)
            }
        }

        scope.launch {
            preferenceRepository.get(PreferenceConstants.NORMALIZE_VOLUME).collectLatest {
                updateLoudness(player.currentMediaItem)
            }
        }
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                fadeOutStarted = false
                currentFadeMultiplier = 1.0f
                updateLoudness(mediaItem)
                
                if (preferenceRepository.getBoolean(PreferenceConstants.CROSSFADE_ENABLED, false)) {
                    fadeIn()
                } else {
                    applyVolume()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    fadeOutStarted = false
                }
            }
            
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady && !fadeOutStarted) {
                    applyVolume()
                }
            }
        })
    }

    private fun startCrossfadePoller() {
        scope.launch {
            while (isActive) {
                if (player.isPlaying && preferenceRepository.getBoolean(PreferenceConstants.CROSSFADE_ENABLED, false)) {
                    val duration = player.duration
                    val position = player.currentPosition
                    val crossfadeDurationSec = preferenceRepository.getInt(PreferenceConstants.CROSSFADE_DURATION, 0)
                    val crossfadeDurationMs = crossfadeDurationSec * 1000L
                    
                    if (duration > 0 && crossfadeDurationMs > 0 && !fadeOutStarted) {
                        val remaining = duration - position
                        if (remaining <= crossfadeDurationMs && remaining > 0) {
                            fadeOutStarted = true
                            fadeOut(remaining)
                        }
                    }
                }
                delay(500.milliseconds)
            }
        }
    }

    private fun fadeIn() {
        fadeJob?.cancel()
        val crossfadeDurationSec = preferenceRepository.getInt(PreferenceConstants.CROSSFADE_DURATION, 0)
        val crossfadeDurationMs = crossfadeDurationSec * 1000L
        
        if (crossfadeDurationMs <= 0) {
            currentFadeMultiplier = 1.0f
            applyVolume()
            return
        }

        fadeJob = scope.launch {
            val steps = 20
            val delayInterval = crossfadeDurationMs / steps
            for (i in 0..steps) {
                currentFadeMultiplier = i.toFloat() / steps
                applyVolume()
                delay(delayInterval.milliseconds)
            }
            currentFadeMultiplier = 1.0f
            applyVolume()
        }
    }

    private fun fadeOut(durationMs: Long) {
        fadeJob?.cancel()
        if (durationMs <= 0) return

        fadeJob = scope.launch {
            val steps = 20
            val delayInterval = durationMs / steps
            val startMultiplier = currentFadeMultiplier
            for (i in steps downTo 0) {
                currentFadeMultiplier = startMultiplier * (i.toFloat() / steps)
                applyVolume()
                delay(delayInterval.milliseconds)
            }
            currentFadeMultiplier = 0f
            applyVolume()
        }
    }

    private fun updateLoudness(mediaItem: MediaItem?) {
        if (!preferenceRepository.getBoolean(PreferenceConstants.NORMALIZE_VOLUME, false)) {
            currentBaseVolume = 1.0f
            applyVolume()
            return
        }

        val loudnessDb = mediaItem?.mediaMetadata?.extras?.getDouble("loudness_db")
        if (loudnessDb != null) {
            val targetLoudness = -14.0
            val gain = (targetLoudness - loudnessDb).coerceAtMost(0.0)
            currentBaseVolume = 10.0.pow(gain / 20.0).toFloat()
        } else {
            currentBaseVolume = 1.0f
        }
        applyVolume()
    }

    private fun applyVolume() {
        player.volume = currentBaseVolume * currentFadeMultiplier
    }
}



