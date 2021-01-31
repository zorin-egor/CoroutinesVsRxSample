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

    companion object {
        val TAG = CoroutineViewModel::class.java.simpleName
    }

    override val result = MutableLiveData<Spanned?>()

    private suspend fun suspendLongAction(): UInt {
        return runInterruptible(block = ::longActionResult)
    }

    private suspend fun suspendLongActionEmit(delay: Long = 3000, count: UInt = 10U, emitter: (UInt, UInt) -> Unit) {
        return runInterruptible {
            longActionEmit(delay, count) { index, value ->
                emitter(index, value)
            }
        }
    }

    override fun completable() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            start()
             withContext(Dispatchers.IO) {
                suspendLongAction()
            }
            result("Complete")
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun single() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            start()
            val result = withContext(Dispatchers.IO) {
                suspendLongAction()
            }
            result(result)
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
                val thread = threadActionResult(2000) {
                    continuation.resume(it)
                }

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
                    val thread = threadActionResult(3000) {
                        continuation.resume(it)
                    }

                    continuation.invokeOnCancellation {
                        thread.interrupt()
                    }
                }
            }

            result(result)
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun combineLatest() {
        val flowOne = callbackFlow {
            val thread = threadActionEmit(7000, 7U, { index, value ->
                sendBlocking((index + 1U).toString())
            }, {
                close()
            })

            awaitClose {
                thread.interrupt()
            }
        }

        val flowTwo = flowOf("1", "2", "3", "4", "5")
            .onEach {
                delay(500)
            }

        val flowThree = flowOf("1", "2", "3", "4", "5", "6")
            .onEach {
                delay(250)
            }

        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowOne.combine(flowTwo) { value1, value2 ->
                "$value1-$value2"
            }.combine(flowThree) { value1, value2 ->
                "$value1-$value2"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(result)
            }
        }
    }

    override fun zip() {
        val flowOne = callbackFlow {
            val thread = threadActionEmit(7000, 7U, { index, value ->
                sendBlocking((index + 1U).toString())
            }, {
                close()
            })

            awaitClose {
                thread.interrupt()
            }
        }

        val flowTwo = flowOf("1", "2", "3", "4", "5")
            .onEach {
                delay(500)
            }

        val flowThree = flowOf("1", "2", "3", "4", "5", "6")
            .onEach {
                delay(250)
            }

        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowOne.zip(flowTwo) { value1, value2 ->
                "$value1-$value2"
            }.zip(flowThree) { value1, value2 ->
                "$value1-$value2"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start()
            }.onCompletion {
                message("onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(result)
            }
        }
    }

    override fun flatMap() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val thread = threadActionEmit(delay = 5000, 5U, { index, value ->
                    sendBlocking(index to value)
                }, {
                    close()
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flatMapMerge { pair ->
                callbackFlow {
                    offer("---- $pair")
                    val thread = threadActionEmit(delay = 2000, 4U, { index, value ->
                        sendBlocking("${index + 1U}-$value-${Thread.currentThread().id}")
                    }, {
                        close()
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
            }.collect { result ->
                emit(result)
            }
        }
    }

    override fun switchMap() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val thread = threadActionEmit(delay = 5000, 5U, { index, value ->
                    sendBlocking(index to value)
                }, {
                    close()
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flatMapLatest { pair ->
                callbackFlow {
                    offer("---- $pair")
                    val thread = threadActionEmit(delay = 2000, 4U, { index, value ->
                        sendBlocking("${index + 1U}-$value-${Thread.currentThread().id}")
                    }, {
                        close()
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
            }.collect { result ->
                emit(result)
            }
        }
    }

    override fun concatMap() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            callbackFlow {
                val thread = threadActionEmit(delay = 5000, 5U, { index, value ->
                    sendBlocking(index to value)
                }, {
                    close()
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flatMapConcat { pair ->
                callbackFlow {
                    offer("---- $pair")
                    val thread = threadActionEmit(delay = 2000, 4U, { index, value ->
                        sendBlocking("${index + 1U}-$value-${Thread.currentThread().id}")
                    }, {
                        close()
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
            }.collect { result ->
                emit(result)
            }
        }
    }

    override fun distinctUntilChanged() {
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch {
            flowOf("hello", "hello", "world")
            .onEach {
                delay(500)
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
            0 -> BroadcastChannel<Int>(Channel.CONFLATED)
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
                val thread = threadActionEmit(delay = 4000, 5U, { index, value ->
                    sendBlocking(index to value)
                }, {
                    close()
                })

                awaitClose { thread.interrupt() }
            }
            .flowOn(Dispatchers.IO)
            .onCompletion { emit("Observable done") }
            .onEach { emit("Observable: $it", R.color.colorThree) }
            .flowOn(Dispatchers.Main)
            .flatMapLatest { pair ->
                callbackFlow {
                    val thread = threadActionEmit(delay = 2000, 5U, { index, value ->
                        sendBlocking("${pair.first} - ${index to value}) - ${Thread.currentThread().id}")
                    }, {
                        close()
                    })

                    awaitClose { thread.interrupt() }
                }
            }
            .flowOn(Dispatchers.IO)
            .collect { emit(it, R.color.colorFour) }
        }.apply {
            invokeOnCompletion {
                message("invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }
}