package io.github.smithjustinn.blackjack.services

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.services.AudioService.Companion.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import sharedui.generated.resources.Res
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class AndroidAudioServiceImpl(
    private val context: Context,
    private val logger: Logger,
) : BaseAudioService(Dispatchers.IO) {
    private val soundPool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()

    private val soundMap = ConcurrentHashMap<StringResource, Int>()
    private val resourceToName = ConcurrentHashMap<StringResource, String>()
    private val loadedSounds = ConcurrentHashMap.newKeySet<Int>()
    private val fallbackPlayers = ConcurrentHashMap<StringResource, MediaPlayer>()
    override var isMuted: Boolean = false
    private var soundVolume = 1.0f

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            }
        }

        scope.launch {
            AudioService.SoundEffect.entries.forEach { effect ->
                loadSound(effect.toResource())
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadSound(resource: StringResource): Int? =
        try {
            val name = getString(resource)
            resourceToName[resource] = name
            val fileName = "$name.m4a"
            val bytes = Res.readBytes("files/$fileName")
            val tempFile = File(context.cacheDir, fileName)
            withContext(Dispatchers.IO) {
                FileOutputStream(tempFile).use { it.write(bytes) }
            }
            val id = soundPool.load(tempFile.absolutePath, 1)
            soundMap[resource] = id
            id
        } catch (e: IOException) {
            logger.e(e) { "IO Error loading sound resource: $resource" }
            null
        } catch (e: Exception) {
            logger.e(e) { "Unexpected error loading sound resource: $resource" }
            null
        }

    private fun playSound(resource: StringResource) {
        if (isMuted) return

        val soundId = soundMap[resource]
        if (soundId != null && loadedSounds.contains(soundId)) {
            val streamId = soundPool.play(soundId, soundVolume, soundVolume, 1, 0, 1f)
            if (streamId == 0) {
                logger.w { "SoundPool play failed, using fallback for: $resource" }
                playFallback(resource)
            }
        } else {
            // Sound not loaded yet - wait briefly and retry before falling back
            scope.launch {
                delay(SOUNDPOOL_RETRY_DELAY_MS)
                val retrySoundId = soundMap[resource]
                if (retrySoundId != null && loadedSounds.contains(retrySoundId)) {
                    soundPool.play(retrySoundId, soundVolume, soundVolume, 1, 0, 1f)
                } else {
                    logger.w { "SoundPool not ready after retry, using fallback for: $resource" }
                    playFallback(resource)
                }
            }
        }
    }

    private fun playFallback(resource: StringResource) {
        val name = resourceToName[resource] ?: return
        val fileName = "$name.m4a"
        scope.launch {
            try {
                val tempFile = File(context.cacheDir, fileName)
                if (tempFile.exists()) {
                    withContext(Dispatchers.Main) {
                        val player = fallbackPlayers[resource]
                        if (player != null) {
                            try {
                                if (player.isPlaying) player.seekTo(0) else player.start()
                            } catch (e: IllegalStateException) {
                                logger.w(e) { "Fallback player not ready or in bad state: $name" }
                            }
                        } else {
                            createNewFallbackPlayer(resource, tempFile, name)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Error playing fallback sound: $name" }
                fallbackPlayers.remove(resource)?.release()
            }
        }
    }

    private fun createNewFallbackPlayer(
        resource: StringResource,
        tempFile: File,
        name: String,
    ) {
        val newPlayer = MediaPlayer()
        try {
            fallbackPlayers[resource] =
                newPlayer.apply {
                    setDataSource(tempFile.absolutePath)
                    setVolume(soundVolume, soundVolume)
                    setOnCompletionListener { mp -> mp.seekTo(0) }
                    setOnPreparedListener { mp -> mp.start() }
                    prepareAsync()
                }
        } catch (e: IOException) {
            logger.e(e) { "IO Error initializing fallback player: $name" }
            newPlayer.release()
        } catch (e: Exception) {
            logger.e(e) { "Unexpected error initializing fallback player: $name" }
            newPlayer.release()
            fallbackPlayers.remove(resource)?.release()
        }
    }

    override fun playEffect(effect: AudioService.SoundEffect) {
        playSound(effect.toResource())
    }

    override fun onRelease() {
        soundPool.release()
        fallbackPlayers.values.forEach { it.release() }
        fallbackPlayers.clear()
        soundMap.clear()
        resourceToName.clear()
        loadedSounds.clear()
    }

    companion object {
        private const val MAX_STREAMS = 10
        private const val SOUNDPOOL_RETRY_DELAY_MS = 100L
    }
}
