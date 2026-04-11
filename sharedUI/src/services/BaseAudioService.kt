package io.github.smithjustinn.blackjack.services

import io.github.smithjustinn.blackjack.services.AudioService.Companion.toResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

// Centralises coroutine lifecycle so every AudioService implementation is leak-proof by construction.
abstract class BaseAudioService(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AudioService {
    protected val job = SupervisorJob()
    protected val scope = CoroutineScope(dispatcher + job)

    // Centralises mute guard so no platform impl can forget it.
    override var isMuted: Boolean = false

    // Template method: subclasses implement platform-specific load logic.
    protected abstract suspend fun loadEffect(resource: StringResource)

    // Drives the common initialisation loop; call from subclass init after fields are ready.
    protected fun initializeEffects() {
        scope.launch {
            AudioService.SoundEffect.entries.forEach { effect ->
                loadEffect(effect.toResource())
            }
        }
    }

    final override fun playEffect(effect: AudioService.SoundEffect) {
        if (isMuted) return
        doPlayEffect(effect)
    }

    // Template method: subclasses implement platform-specific playback logic.
    protected abstract fun doPlayEffect(effect: AudioService.SoundEffect)

    final override fun release() {
        job.cancel()
        onRelease()
    }

    protected abstract fun onRelease()
}
