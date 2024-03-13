package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.sample.coroutinesvsrxjava.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
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

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class CoroutineViewModel(application: Application) : BaseViewModel(application), Actions {

    private suspend fun suspendLongAction(): UInt {
        return runInterruptible(block = ::longActionResult, context = Dispatchers.IO)
    }

    private suspend fun suspendLongActionEmit(
        delay: Long = 3000,
        count: UInt = 10U,
        emitter: (UInt, UInt) -> Unit
    ) {
        return runInterruptible {
            longActionEmit(delay, count, false) { index, value ->
                emitter(index, value)
            }
        }
    }

    override fun completable() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { context, error ->
            error("CoroutineExceptionHandler(${error.message ?: "-"})")
        }) {
            start()
            suspendLongAction()
        }.apply {
            invokeOnCompletion {
                if (it == null) {
                    result("invokeOnCompletion()")
                }
            }
        }
    }

    override fun single() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { context, error ->
            error("CoroutineExceptionHandler(${error.message ?: "-"})")
        }) {
            start()
            suspendLongAction().apply(::result)
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun observable() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val thread = threadActionEmit(emitter = { index, value ->
                    trySendBlocking(index to value)
                }, complete = {
                    close()
                }, error = {
                    close(it)
                })

                awaitClose {
                    thread.interrupt()
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
                emit(result)
            }
        }
    }

    override fun flow(items: UInt) {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionEmit(0, 1000U) { index, value ->
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
                emit(result)
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
            .onEach {
                delay(500)
            }

        val flowC = flowOf("1", "2", "3", "4", "5", "6")
            .onEach {
                delay(250)
            }

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
            }.onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.catch {
                error(it.message)
            }.collect { result ->
                emit(result)
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
                emit(result)
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
                emit(result)
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
                emit(result)
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
                emit(result)
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
                message("onCompletion(${it?.message ?: ""})")
            }
            .distinctUntilChanged()
            .collect { result ->
                emit(result)
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
                emit(result)
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
                emit("one - $it")
            }
        }

        viewModelScope.launch {
            delay(4000)
            receiver.collect {
                delay(1000)
                emit("two - $it")
            }
        }

        viewModelScope.launch {
            start()
            message( "Flow is: ${receiver.javaClass.simpleName}")
            (1..20).forEach { index ->
                emiterChannel?.trySend(index)
                    ?: emiterFlow?.tryEmit(index)
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
            emit("Complete done", R.color.colorOne)

            val single = withContext(Dispatchers.IO) { longActionResult() }
            emit("Single: ${single.toString()}", R.color.colorTwo)
            emit("Single done")

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
                    emit(value = "Second subscriber: $it", colorId = R.color.colorTwo)
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
            .onEach { emit("Flowable one: $it", R.color.colorThree) }
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
                .onEach { emit("Flowable two: $it", R.color.colorFour) }
                .onCompletion { emit("Flowable done") }
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
}