package io.github.smithjustinn.blackjack.services

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.services.AudioService.Companion.toResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import sharedui.generated.resources.Res
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class JvmAudioServiceImpl(
    private val logger: Logger,
) : AudioService {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isSoundEnabled = true
    private val tempAudioDir = File(System.getProperty("java.io.tmpdir"), "blackjack_audio").apply { mkdirs() }
    private val resourceToPath = ConcurrentHashMap<StringResource, String>()

    init {
        scope.launch {
            AudioService.SoundEffect.entries.forEach { effect ->
                prepareSound(effect.toResource())
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun prepareSound(resource: StringResource) {
        try {
            val name = getString(resource)
            val fileName = "$name.m4a"
            val tempFile = File(tempAudioDir, fileName)

            if (!tempFile.exists()) {
                val bytes = Res.readBytes("files/$fileName")
                withContext(Dispatchers.IO) {
                    FileOutputStream(tempFile).use { it.write(bytes) }
                }
            }
            resourceToPath[resource] = tempFile.absolutePath
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            logger.e(e) { "Error preparing sound resource: $resource" }
        }
    }

    private fun playSound(resource: StringResource) {
        if (!isSoundEnabled) return
        val path = resourceToPath[resource] ?: return

        scope.launch {
            try {
                // macOS only audio playback fallback natively provided by the OS
                ProcessBuilder("afplay", path).start()
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                logger.e(e) { "Error playing sound: $resource" }
            }
        }
    }

    override fun playEffect(effect: AudioService.SoundEffect) {
        playSound(effect.toResource())
    }

    override fun release() {
        // Nothing to forcefully release currently
    }

}
