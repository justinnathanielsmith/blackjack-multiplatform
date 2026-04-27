import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

fun main() = runTest {
    val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
    var completed = false
    scope.launch {
        println("Started")
        delay(1500)
        println("Finished delay")
        completed = true
    }
    println("Before advance: completed=$completed")
    advanceUntilIdle()
    println("After advance: completed=$completed")
}
