package io.github.smithjustinn.blackjack.services

import co.touchlab.kermit.Logger
import sharedui.generated.resources.Res
import io.github.smithjustinn.blackjack.services.AudioService.Companion.toResource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create


@OptIn(ExperimentalForeignApi::class)
class IosAudioServiceImpl(
    private val logger: Logger,
) : AudioService {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Player pool for each sound effect to allow simultaneous playback
    private val soundPools = mutableMapOf<StringResource, MutableList<AVAudioPlayer>>()
    private val audioDataMap = mutableMapOf<StringResource, NSData>()

    private var isSoundEnabled = true
    private var isSessionActive = false
    private var soundVolume = 1.0f

    init {
        scope.launch {
            AudioService.SoundEffect.entries.forEach { effect ->
                val resource = effect.toResource()
                loadAudioData(resource)?.let { data ->
                    audioDataMap[resource] = data
                    // Pre-warm a player for each sound
                    createPlayerForPool(resource, data)
                }
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class, ExperimentalForeignApi::class)
    private suspend fun loadAudioData(resource: StringResource): NSData? {
        return try {
            val name = getString(resource)
            val fileName = "files/$name.m4a"
            val bytes = Res.readBytes(fileName)

            bytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes.size.toULong(),
                )
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to load audio resource: $resource" }
            null
        }
    }

    private fun setupAudioSession() {
        if (isSessionActive) return

        try {
            val session = AVAudioSession.sharedInstance()
            var error: NSError? = null

            // Use Ambient category so we don't silence other apps' audio
            session.setCategory(
                category = AVAudioSessionCategoryAmbient,
                mode = AVAudioSessionModeDefault,
                options = 0uL,
                error = null,
            )

            val success = session.setActive(true, error = null)
            if (success) {
                isSessionActive = true
            } else {
                logger.e { "Failed to activate audio session: ${error?.localizedDescription}" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error setting up audio session" }
        }
    }

    private fun createPlayerForPool(
        resource: StringResource,
        data: NSData,
    ): AVAudioPlayer? {
        try {
            var error: NSError? = null
            val player = AVAudioPlayer(data, error = null)

            if (player != null) {
                player.prepareToPlay()
                player.setVolume(soundVolume)

                val pool = soundPools.getOrPut(resource) { mutableListOf() }
                // Clean up pool to prevent it from growing infinitely if sounds get stuck
                if (pool.size > MAX_PLAYERS_PER_SOUND * 2) {
                    pool.removeAll { !it.isPlaying() }
                }

                if (pool.size < MAX_PLAYERS_PER_SOUND) {
                    pool.add(player)
                }
                return player
            } else {
                logger.e { "Failed to create player: ${error?.localizedDescription}" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception creating player for pool" }
        }
        return null
    }

    override fun playEffect(effect: AudioService.SoundEffect) {
        if (!isSoundEnabled) return

        val resource = effect.toResource()
        scope.launch {
            withContext(Dispatchers.Main) {
                setupAudioSession()

                val pool = soundPools[resource]
                var player = pool?.firstOrNull { !it.isPlaying() }

                if (player == null) {
                    // Try to create a new one if all current ones are busy
                    val data = audioDataMap[resource]
                    if (data != null) {
                        player = createPlayerForPool(resource, data)
                    }
                }

                if (player != null) {
                    player.currentTime = 0.0
                    player.play()
                } else {
                    logger.w { "Could not find or create available player for $resource" }
                }
            }
        }
    }

    override fun release() {
        soundPools.values.forEach { pool ->
            pool.forEach { it.stop() }
        }
        soundPools.clear()
    }

    companion object {
        // Limit simultaneous sounds of the SAME type to prevent memory explosion
        private const val MAX_PLAYERS_PER_SOUND = 5
    }
}
