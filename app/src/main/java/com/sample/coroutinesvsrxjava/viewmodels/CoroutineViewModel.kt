package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sample.coroutinesvsrxjava.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.random.Random

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class CoroutineViewModel(application: Application) : BaseViewModel(application), Actions {

    override val result = MutableLiveData<Spanned?>()

    private suspend fun suspendLongAction(): UInt {
        return runInterruptible(block = ::longActionResult)
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
            withContext(Dispatchers.IO) {
                suspendLongAction()
            }
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
        viewModelScope.launch {
            start()
            try {
                withContext(Dispatchers.IO) { suspendLongAction() }.apply(::result)
            } catch (e: Exception) {
                error(e.message)
            }
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
                    sendBlocking(index to value)
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
            callbackFlow {
                suspendLongActionEmit(0, 1000U) { index, value ->
                    offer(index to value)
                }
                close()
            }
//            .buffer(Channel.CONFLATED)
            .conflate()
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }
            .onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }
            .collect { result ->
                emit(result)
            }
        }
    }

    override fun callback() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
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
        viewModelScope.launch {
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
                sendBlocking((index + 1U).toString())
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
                sendBlocking((index + 1U).toString())
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
                    sendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
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
                    offer("--------------------------------------------------------")
                    offer(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        sendBlocking("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
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
                    sendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
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
                    offer("--------------------------------------------------------")
                    offer(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        sendBlocking("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
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
                val thread = threadActionEmit(delay = 5000, count = 5U, emitter = { index, value ->
                    sendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
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
                    offer("--------------------------------------------------------")
                    offer(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        sendBlocking("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
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

//        val bus = when(Random.nextInt(4)) {
//            0 -> Channel<Int>(Channel.UNLIMITED)
//            1 -> Channel<Int>(Channel.CONFLATED)
//            2 -> Channel<Int>(Channel.RENDEZVOUS)
//            else -> Channel<Int>(Channel.BUFFERED)
//        }

        val bus = when(Random.nextInt(2)) {
            0 -> BroadcastChannel(Channel.CONFLATED)
            else -> BroadcastChannel<Int>(Channel.BUFFERED)
        }

        viewModelScope.launch {
            delay(1000)
            bus.openSubscription().consumeEach {
                emit("one - $it")
            }
        }

        viewModelScope.launch {
            delay(2000)
            bus.asFlow().collect {
                emit("two - $it")
            }
        }

        viewModelScope.launch {
            start()
            message( "Channel is: ${bus.javaClass.simpleName}")
            (1..10).forEach {
                bus.offer(it)
                delay(500)
            }
        }.invokeOnCompletion {
            bus.cancel()
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

            callbackFlow {
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 4000, count = 5U, emitter = { index, value ->
                    sendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    close()
                }, error = {
                    close(it)
                })

                awaitClose { thread.interrupt() }
            }
            .flowOn(Dispatchers.IO)
            .onCompletion { emit("Observable done") }
            .onEach { emit("Observable: $it", R.color.colorThree) }
            .flowOn(Dispatchers.Main)
            .flatMapMerge { pair ->
                callbackFlow {
                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 5U, emitter = { index, value ->
                        sendBlocking(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                    }, complete = {
                        close()
                    }, error = {
                        close(it)
                    })

                    awaitClose { thread.interrupt() }
                }
                .flowOn(Dispatchers.IO)
                .onEach { emit("Flowable: $it", R.color.colorFour) }
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