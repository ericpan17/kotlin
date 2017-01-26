// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        SUSPENDED_MARKER
    }

    // Tail calls are not allowed to be Nothing typed. See KT-15051
    suspend fun suspendLogAndThrow(exception: Throwable): Any? = suspendCoroutineOrReturn { c ->
        result += "throw(${exception.message});"
        c.resumeWithException(exception)
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resume(data: Unit) {

        }

        override fun resumeWithException(exception: Throwable) {
            controller.result += "caught(${exception.message});"
        }
    })

    return controller.result
}

fun box(): String {
    val value = builder {
        try {
            try {
                suspendAndLog("1")
                suspendLogAndThrow(RuntimeException("exception"))
            }
            catch (e: RuntimeException) {
                suspendAndLog("caught")
                suspendLogAndThrow(RuntimeException("fromCatch"))
            }
        } finally {
            suspendAndLog("finally")
        }
        suspendAndLog("ignore")
    }
    if (value != "suspend(1);throw(exception);suspend(caught);throw(fromCatch);suspend(finally);caught(fromCatch);") {
        return "fail: $value"
    }

    return "OK"
}