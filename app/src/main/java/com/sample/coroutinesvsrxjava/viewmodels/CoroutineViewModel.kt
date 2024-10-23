package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.sample.coroutinesvsrxjava.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlin.random.nextUInt

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class CoroutineViewModel(application: Application) : BaseViewModel(application), Actions {

    private suspend fun suspendLongAction(delay: Long = 1000, isRandomError: Boolean = false): UInt {
        return runInterruptible(
            block = { longActionResult(delay = delay, isRandomError = isRandomError) },
            context = Dispatchers.IO
        )
    }

    private suspend fun suspendLongActionEmit(
        delay: Long = 3000,
        count: UInt = 10U,
        isError: Boolean = false,
        emitter: (UInt, UInt) -> Unit
    ) {
        return runInterruptible {
            longActionEmit(delay, count, isError) { index, value ->
                emitter(index, value)
            }
        }
    }

    private suspend fun someLongWorkScope() {
        coroutineScope {
            launch(SupervisorJob()) {
                delay(5000)
                message("someWork(${coroutineContext[CoroutineName]})")
            }
        }
    }

    override fun completable() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            start()
            suspendLongAction()
            result("Complete")
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun single() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { context, error ->
            error("CoroutineExceptionHandler(${error.message ?: "-"})")
        }) {
            start()
            result(suspendLongAction())
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun observable() {
        viewModelScope.coroutineContext.cancelChildren()

        val job = viewModelScope.launch {
            val flow = kotlinx.coroutines.flow.flow<Pair<UInt, UInt>>{
                repeat(10) {
                    emit(it.toUInt() to Random.nextUInt())
                    delay(100)
                    if (Random.nextInt() % 10 == 0) {
                        throw NullPointerException("Unexpected error")
                    }
                }
            }
            .catch {
                error(it.message)
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                message(value = "onEach: $it,  ${Thread.currentThread().name}", colorId = R.color.colorThree)
                delay(200)
            }
            .onStart {
                start(isLogClear = false)
            }.onCompletion {
                message(value = "onCompletion(${it?.message ?: ""})", colorId = R.color.colorTwo)
            }

            flow.collect { result ->
                message(value = result, colorId = R.color.colorOne)
            }

            delay(500)

            try {
                flow.collect { result ->
                    message(value = result, colorId = R.color.colorTwo)
                }
            } catch (e: Exception) {
                error(e)
            }
        }

        viewModelScope.launch {
            delay(2000)
            if (Random.nextInt() % 10 == 0) {
                job.cancel()
            }
        }
    }

    override fun flow(items: UInt) {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionEmit(delay = 0, count = 1000U) { index, value ->
                    trySend(index to value)
                }
                close()
            }
            .conflate()
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }
            .onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                message(result)
            }
        }
    }

    override fun callback() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { context, error ->
            error("CoroutineExceptionHandler(${error.message ?: "-"})")
        }) {
            start()

            val result = suspendCancellableCoroutine<UInt> { continuation ->
                val thread = threadActionResult(delay = 2000, emitter = {
                    continuation.resume(it)
                }, error = {
                    continuation.cancel(it)
                })

                continuation.invokeOnCancellation {
                    thread.interrupt()
                }
            }

            result(result)
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun timeout() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { context, error ->
            error("CoroutineExceptionHandler(${error.message ?: "-"})")
        }) {
            start()

            val result = withTimeout(2000) {
                suspendCancellableCoroutine<UInt> { continuation ->
                    val thread = threadActionResult(delay = 3000, emitter = {
                        continuation.resume(it)
                    }, error = {
                        continuation.cancel(it)
                    })

                    continuation.invokeOnCancellation {
                        thread.interrupt()
                    }
                }
            }

            result(result)
        }.apply {
            invokeOnCompletion {
                if (it != null) {
                    error("invokeOnCompletion(${it.message})")
                } else {
                    message("invokeOnCompletion()")
                }
            }
        }
    }

    override fun combineLatest() {
        val flowA = callbackFlow {
            val thread = threadActionEmit(delay = 7000, count = 7U, emitter = { index, value ->
                trySendBlocking((index + 1U).toString())
            }, complete = {
                close()
            }, error = {
                close(it)
            })

            awaitClose {
                thread.interrupt()
            }
        }

        val flowB = flowOf("1", "2", "3", "4", "5")
            .onEach { delay(500) }

        val flowC = flowOf("1", "2", "3", "4", "5", "6")
            .onEach { delay(250) }

        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowA.combine(flowB) { value1, value2 ->
                "A$value1-B$value2"
            }.combine(flowC) { value1, value2 ->
                "$value1-C$value2"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }
            .onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                message(result)
            }
        }
    }

    override fun zip() {
        val flowA = callbackFlow {
            val thread = threadActionEmit(delay = 7000, count = 7U, emitter = { index, value ->
                trySendBlocking((index + 1U).toString())
            }, complete = {
                close()
            }, error = {
                close(it)
            })

            awaitClose {
                thread.interrupt()
            }
        }

        val flowB = flowOf("1", "2", "3", "4", "5")
            .onEach {
                delay(500)
            }

        val flowC = flowOf("1", "2", "3", "4", "5", "6")
            .onEach {
                delay(250)
            }

        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowA.zip(flowB) { value1, value2 ->
                "A$value1-B$value2"
            }.zip(flowC) { value1, value2 ->
                "$value1-C$value2"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                message(result)
            }
        }
    }

    override fun flatMap() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 5000, count = 5U, emitter = { index, value ->
                    trySendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    close()
                }, error = {
                    close(it)
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flatMapMerge { triple ->
                callbackFlow {
                    trySend("--------------------------------------------------------")
                    trySend(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        trySendBlocking("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
                    }, complete = {
                        close()
                    }, error = {
                        close(it)
                    })

                    awaitClose {
                        thread.interrupt()
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                    message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                message(result)
            }
        }
    }

    override fun switchMap() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 5000, count = 5U, emitter = { index, value ->
                    trySendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    close()
                }, error = {
                    close(it)
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flatMapLatest { triple ->
                callbackFlow {
                    trySend("--------------------------------------------------------")
                    trySend(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        trySendBlocking("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
                    }, complete = {
                        close()
                    }, error = {
                        close(it)
                    })

                    awaitClose {
                        thread.interrupt()
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                message(result)
            }
        }
    }

    override fun concatMap() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 500, count = 5U, emitter = { index, value ->
                    trySendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    close()
                }, error = {
                    close(it)
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flatMapConcat { triple ->
                callbackFlow {
                    trySend("--------------------------------------------------------")
                    trySend(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 4000, count = 4U, emitter = { index, value ->
                        trySendBlocking("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
                    }, complete = {
                        close()
                    }, error = {
                        close(it)
                    })

                    awaitClose {
                        thread.interrupt()
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                    message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                message(result)
            }
        }
    }

    override fun distinctUntilChanged() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowOf("hello", "hello", "world")
            .map {
                delay(500)
                it to Thread.currentThread().name
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                result("onCompletion(${it?.message ?: ""})")
            }
            .distinctUntilChanged()
            .collect { result ->
                message(result)
            }
        }
    }

    override fun debounce() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .onEach {
                if (it % 2 == 0) {
                    delay(1010)
                }
            }
            .debounce(1000)
            .map {
                "$it-${Thread.currentThread().name}"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }
            .collect { result ->
                message(result)
            }
        }
    }

    override fun eventBus() {
        viewModelScope.coroutineContext.cancelChildren()
        
        // Channels consume value by subscribers
        var emiterChannel: Channel<Int>? = null
        // Flows not consume values
        var emiterFlow: MutableSharedFlow<Int>? = null

        val receiver: Flow<Int> = when(Random.nextInt(6)) {
            0 -> Channel<Int>(Channel.UNLIMITED).also { emiterChannel = it }.receiveAsFlow()
            1 -> Channel<Int>(Channel.CONFLATED).also { emiterChannel = it }.receiveAsFlow()
            2 -> Channel<Int>(Channel.RENDEZVOUS).also { emiterChannel = it }.receiveAsFlow()
            3 -> Channel<Int>(Channel.BUFFERED).also { emiterChannel = it }.receiveAsFlow()
            4 -> MutableStateFlow<Int>(0).also { emiterFlow = it }.asStateFlow()
            else -> MutableSharedFlow<Int>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            ).also { emiterFlow = it }.asSharedFlow()
        }

        viewModelScope.launch {
            delay(2000)
            receiver.buffer().collect {
                delay(2000)
                message("one - $it")
            }
        }

        viewModelScope.launch {
            delay(4000)
            receiver.collect {
                delay(1000)
                message("two - $it")
            }
        }

        viewModelScope.launch {
            start()
            message( "Flow is: ${receiver.javaClass.simpleName}")
            (1..20).forEach { index ->
                emiterChannel?.send(index)
                    ?: emiterFlow?.emit(index)
                delay(500)
            }
        }.invokeOnCompletion {
            message("onCompletion(${it?.message ?: ""})")
        }
    }

    override fun chains() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {

            start()

            withContext(Dispatchers.IO) { longActionResult() }
            message("Complete done", R.color.colorOne)

            val single = withContext(Dispatchers.IO) { longActionResult() }
            message("Single: ${single.toString()}", R.color.colorTwo)
            message("Single done")

            val hotFlow = MutableSharedFlow<String>(
                replay = 1,
                extraBufferCapacity = 10,
                onBufferOverflow = BufferOverflow.DROP_LATEST
            )

//            val hotFlow = Channel<String>(
////                capacity = Channel.CONFLATED,
//                onBufferOverflow = BufferOverflow.DROP_LATEST
//            )

            launch {
                var counter = 10
                while (counter > 0) {
                    hotFlow.emit("HotFlow: ${counter--}")
                    delay(500)
                }
            }

            launch {
                hotFlow.asSharedFlow().collect {
                    message(value = "Second subscriber: $it", colorId = R.color.colorTwo)
                    delay(500)
                }
            }

            delay(2000)

            hotFlow.asSharedFlow()
                .flatMapMerge { hotValue ->
                callbackFlow {
                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 4000, count = 5U, emitter = { index, value ->
                        trySendBlocking(Triple(index, "$value - $hotValue", "${Thread.currentThread().name}, $threadName"))
                    }, complete = {
                        close()
                    }, error = {
                        close(it)
                    })

                    awaitClose { thread.interrupt() }
                }
            }
            .flowOn(Dispatchers.IO)
            .onEach { message("Flowable one: $it", R.color.colorThree) }
            .flowOn(Dispatchers.Main)
            .flatMapMerge { triple ->
                callbackFlow {
                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 5U, emitter = { index, value ->
                        trySendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                    }, complete = {
                        close()
                    }, error = {
                        close(it)
                    })

                    awaitClose { thread.interrupt() }
                }
                .flowOn(Dispatchers.IO)
                .onEach { message("Flowable two: $it", R.color.colorFour) }
                .onCompletion { message("Flowable done") }
                .flowOn(Dispatchers.Main)
            }
            .catch { error(it.message) }
            .collect()
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun experiments() {
        fun getJob(suffixName: String = "job", type: Int? = null) = when(type ?: Random.nextInt(3)) {
            0 -> Job() + CoroutineName("Simple $suffixName")
            1 -> SupervisorJob() + CoroutineName("Supervisor $suffixName")
            else -> CoroutineName("Default $suffixName")
        }

        val scope = if (Random.nextBoolean()) {
            viewModelScope.coroutineContext.cancelChildren()
            viewModelScope
        } else {
            CoroutineScope(Dispatchers.Main)
        }

        val rootErrorHandler = CoroutineExceptionHandler { context, error ->
            error("CoroutineExceptionHandler(${error.message ?: "-"}, ${context[CoroutineName]})")
        }

        val rootContext = Dispatchers.Main + rootErrorHandler + getJob(suffixName = "root job", type = 2)

        scope.launch {
            delay(5000)
            message("Some coroutine in same scope")
        }

        scope.launch(rootContext) {
            start()

            someLongWorkScope()

            when(Random.nextInt(2)) {
                0 -> {
                    message("Random 0 - start")

                    launch {
                        delay(2000)
                        message("Child01(${coroutineContext[CoroutineName]})")
                    }

                    launch {
                        throw IllegalArgumentException("Child02 throw")
                    }

                    delay(1000)
                    message("Random 0 - end")
                }

                1 -> launch(context = coroutineContext + getJob(suffixName = "child job 1", type = 1) + CoroutineExceptionHandler { context, error ->
                    error("ChildExceptionHandler1(${error.message ?: "-"}, ${context[CoroutineName]})")
                }) {
                    message("Random 1 - start")

                    launch(getJob(suffixName = "child job 11", type = 2)) {
                        delay(2000)
                        message("Child11(${coroutineContext[CoroutineName]})")
                    }

                    launch(getJob(suffixName = "child job 12", type = 2)) {
                        throw IllegalArgumentException("Child12 throw")
                    }

                    delay(1000)
                    message("Random 1 - end")
                }
            }

            when(Random.nextInt(3)) {
                0 -> {
                    message("Random cancel")
                    delay(500)
                    cancel("Random cancel reason")
                }
                1 -> {
                    message("Random root throw")
                    throw IllegalArgumentException("Root throw")
                }
                else -> message("No error or cancel")
            }

            val result = suspendLongAction(delay = 1000, isRandomError = false)
            result("Success: $result")
        }.apply {
            invokeOnCompletion {
                message(value = "invokeOnCompletion(${it?.message})", colorId = R.color.colorAccent)
            }
        }
    }
}