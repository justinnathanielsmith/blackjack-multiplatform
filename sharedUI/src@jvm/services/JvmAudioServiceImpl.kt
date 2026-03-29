package io.github.smithjustinn.blackjack.services

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.services.AudioService.Companion.toResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import sharedui.generated.resources.Res
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class JvmAudioServiceImpl(
    private val logger: Logger,
) : AudioService {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    override var isMuted: Boolean = false

    private val tempAudioDir: File = Files.createTempDirectory("blackjack_audio").toFile()

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

            var shouldWrite = false
            try {
                Files.createFile(tempFile.toPath())
                shouldWrite = true
            } catch (e: java.nio.file.FileAlreadyExistsException) {
                // File already exists, skip writing
            }
            if (shouldWrite) {
                val bytes = Res.readBytes("files/$fileName")
                withContext(Dispatchers.IO) {
                    FileOutputStream(tempFile).use { it.write(bytes) }
                }
            }
            resourceToPath[resource] = tempFile.absolutePath
        } catch (e: Exception) {
            logger.e(e) { "Error preparing sound resource: $resource" }
        }
    }

    private fun playSound(resource: StringResource) {
        if (isMuted) return
        val path = resourceToPath[resource] ?: return

        val file = File(path)
        val canonicalPath = file.canonicalPath
        val canonicalTempDir = tempAudioDir.canonicalPath

        if (!canonicalPath.startsWith(canonicalTempDir) || !file.exists()) {
            logger.e { "Invalid sound path detected: $path" }
            return
        }

        scope.launch {
            try {
                // macOS only audio playback fallback natively provided by the OS
                ProcessBuilder(listOf("afplay", canonicalPath)).start()
            } catch (e: Exception) {
                logger.e(e) { "Error playing sound: $resource" }
            }
        }
    }

    override fun playEffect(effect: AudioService.SoundEffect) {
        playSound(effect.toResource())
    }

    override fun release() {
        job.cancel()
        tempAudioDir.deleteRecursively()
    }
}
