package dev.fritz2.test

import dev.fritz2.binding.MultiMountPoint
import dev.fritz2.binding.Patch
import dev.fritz2.binding.SingleMountPoint
import dev.fritz2.remote.Request
import dev.fritz2.remote.remote
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.promise

fun <T> runTest(block: suspend () -> T): dynamic = GlobalScope.promise {
    delay(50)
    block()
    delay(50)
}

val targetId = "target"

fun initDocument() {
    document.clear()
    document.write(
        """
            <body id="$targetId">
                Loading...
            </body>
        """.trimIndent()
    )
}

fun <T> checkFlow(upstream: Flow<T>, check: TestSingleMountPoint<T>.(Int, T, T?) -> Boolean) =
    TestSingleMountPoint(upstream, check)

class TestSingleMountPoint<T>(
    upstream: Flow<T>,
    val check: TestSingleMountPoint<T>.(Int, T, T?) -> Boolean,
    val done: CompletableDeferred<Boolean> = CompletableDeferred()
) : SingleMountPoint<T>(upstream), CompletableDeferred<Boolean> by done {
    var count = 0;

    override fun set(value: T, last: T?) {
        val result = check(count, value, last)
        count++;
        if (result) done.complete(true)
    }
}

fun <T> checkFlow(
    upstream: Flow<Patch<T>>,
    check: TestMultiMountPoint<T>.(Int, Patch<T>) -> Boolean
) = TestMultiMountPoint(upstream, check)

class TestMultiMountPoint<T>(
    upstream: Flow<Patch<T>>,
    val check: TestMultiMountPoint<T>.(Int, Patch<T>) -> Boolean,
    val done: CompletableDeferred<Boolean> = CompletableDeferred()
) : MultiMountPoint<T>(upstream), CompletableDeferred<Boolean> by done {
    private var count = 0;

    override fun patch(patch: Patch<T>) {
        val result = check(count, patch)
        count++;
        if (result) done.complete(true)
    }
}

typealias Endpoint = String
const val test: Endpoint = "test"
const val rest: Endpoint = "rest"

suspend fun testServer(endpoint: Endpoint): Request {
    val r = remote("http://localhost:3000/$endpoint")
    if(endpoint == rest) r.get("clear")
    return r
}