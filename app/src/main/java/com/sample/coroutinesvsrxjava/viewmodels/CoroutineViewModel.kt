package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

class CoroutineViewModel(application: Application) : AndroidViewModel(application), ActionViewModel {

    companion object {
        val TAG = CoroutineViewModel::class.java.simpleName
    }

    private var job: Job? = null

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

    override fun single() {
        job?.cancel()
        job = viewModelScope.launch {
            start(getApplication())
            val result = withContext(Dispatchers.IO) {
                suspendLongAction()
            }
            result(getApplication(), result)
        }.apply {
            invokeOnCompletion {
                message(getApplication(), "invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun observable() {
        job?.cancel()
        job = viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                val thread = threadActionEmit(emitter = { index, value ->
                    sendBlocking(Pair(index, value))
                }, complete = {
                    close()
                })

                awaitClose {
                    thread.interrupt()
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun flow(items: UInt) {
        job?.cancel()
        job = viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionEmit(0, 1000U) { index, value ->
                    offer(Pair(index, value))
                }
                close()
            }
//            .buffer(Channel.CONFLATED)
            .conflate()
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }
            .onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }
            .collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun callback() {
        job?.cancel()
        job = viewModelScope.launch {
            start(getApplication())

            val result = suspendCancellableCoroutine<UInt> { continuation ->
                val thread = threadActionResult(2000) {
                    continuation.resume(it)
                }

                continuation.invokeOnCancellation {
                    thread.interrupt()
                }
            }

            result(getApplication(), result)
        }.apply {
            invokeOnCompletion {
                message(getApplication(), "invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun timeout() {
        job?.cancel()
        job = viewModelScope.launch {
            start(getApplication())

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

            result(getApplication(), result)
        }.apply {
            invokeOnCompletion {
                message(getApplication(), "invokeOnCompletion(${it?.message ?: ""})")
            }
        }
    }

    override fun combineLatest() {
        val flowOne = callbackFlow<String> {
            val thread = threadActionEmit(5000, 5U, { index, value ->
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

        val flowThree = flowOf("1", "2", "3", "4", "5")
            .onEach {
                delay(250)
            }

        job?.cancel()
        job = viewModelScope.launch {
            flowOne.combine(flowTwo) { value1, value2 ->
                "$value1-$value2"
            }.combine(flowThree) { value1, value2 ->
                "$value1-$value2"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun zip() {
        val flowOne = callbackFlow<String> {
            val thread = threadActionEmit(5000, 5U, { index, value ->
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

        val flowThree = flowOf("1", "2", "3", "4", "5")
            .onEach {
                delay(250)
            }

        job?.cancel()
        job = viewModelScope.launch {
            flowOne.zip(flowTwo) { value1, value2 ->
                "$value1-$value2"
            }.zip(flowThree) { value1, value2 ->
                "$value1-$value2"
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun flatMap() {
        job?.cancel()
        job = viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionEmit(delay = 5000, 5U) { index, value ->
                    sendBlocking(Pair(index, value))
                }
                close()
            }
            .flatMapMerge { pair ->
                callbackFlow<String> {
                    offer("---- $pair")
                    suspendLongActionEmit(delay = 2000, 4U) { index, value ->
                        sendBlocking("${index + 1U}-$value-${Thread.currentThread().id}")
                    }
                    close()
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun switchMap() {
        job?.cancel()
        job = viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionEmit(delay = 5000, 5U) { index, value ->
                    sendBlocking(Pair(index, value))
                }
                close()
            }
            .flatMapLatest { pair ->
                callbackFlow<String> {
                    offer("---- $pair")
                    suspendLongActionEmit(delay = 2000, 4U) { index, value ->
                        sendBlocking("${index + 1U}-$value-${Thread.currentThread().id}")
                    }
                    close()
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun concatMap() {
        job?.cancel()
        job = viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionEmit(delay = 5000, 5U) { index, value ->
                    sendBlocking(Pair(index, value))
                }
                close()
            }
            .flatMapConcat { pair ->
                callbackFlow<String> {
                    offer("---- $pair")
                    suspendLongActionEmit(delay = 2000, 4U) { index, value ->
                        sendBlocking("${index + 1U}-$value-${Thread.currentThread().id}")
                    }
                    close()
                }
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }.collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun distinctUntilChanged() {
        job?.cancel()
        job = viewModelScope.launch {
            flowOf("hello", "hello", "world")
            .onEach {
                delay(500)
            }
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }
            .distinctUntilChanged()
            .collect { result ->
                emit(getApplication(), result)
            }
        }
    }

    override fun debounce() {
        job?.cancel()
        job = viewModelScope.launch {
            flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .onEach {
                if (it % 2 == 0) {
                    delay(1010)
                }
            }
            .debounce(1000)
            .flowOn(Dispatchers.IO)
            .onStart {
                start(getApplication())
            }.onCompletion {
                message(getApplication(), "onCompletion(${it?.message ?: ""})")
            }
            .collect { result ->
                emit(getApplication(), result)
            }
        }
    }
}