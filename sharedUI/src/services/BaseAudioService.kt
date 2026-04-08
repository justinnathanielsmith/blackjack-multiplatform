package io.github.smithjustinn.blackjack.services

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Centralises coroutine lifecycle so every AudioService implementation is leak-proof by construction.
abstract class BaseAudioService(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AudioService {
    protected val job = SupervisorJob()
    protected val scope = CoroutineScope(dispatcher + job)

    final override fun release() {
        job.cancel()
        onRelease()
    }

    protected abstract fun onRelease()
}
