package io.github.smithjustinn.blackjack.infra
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Creates a [CoroutineScope] that is automatically cancelled when the [Lifecycle] is destroyed.
 */
fun Lifecycle.componentScope(context: CoroutineContext = Dispatchers.Main.immediate): CoroutineScope {
    val scope = CoroutineScope(context + SupervisorJob())
    doOnDestroy { scope.cancel() }
    return scope
}

/**
 * A property delegate that provides a [CoroutineScope] bound to the [ComponentContext] lifecycle.
 */
val ComponentContext.componentScope: CoroutineScope
    get() = lifecycle.componentScope()

val LifecycleOwner.componentScope: CoroutineScope
    get() = lifecycle.componentScope()
