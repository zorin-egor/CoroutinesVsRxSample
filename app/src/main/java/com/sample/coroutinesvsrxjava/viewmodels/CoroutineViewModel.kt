package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*

class CoroutineViewModel(application: Application) : AndroidViewModel(application), ActionViewModel {

    companion object {
        val TAG = CoroutineViewModel::class.java.simpleName
    }

    private var job: Job? = null

    override val result = MutableLiveData<Spanned?>()

    private suspend fun suspendLongAction(): UInt {
        return runInterruptible(block = ::longActionResult)
    }

    private suspend fun suspendLongActionProgress(delay: Long = 3000, count: UInt = 10U, progress: (UInt, UInt) -> Unit) {
        runInterruptible {
            longActionEmit(delay, count) { index, value ->
                progress(index, value)
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
            callbackFlow<UInt> {
                suspendLongActionProgress { index, value ->
                    offer(value)
                }
                close()
            }
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

    override fun flow(items: UInt) {
        job?.cancel()
        job = viewModelScope.launch {
            callbackFlow<Pair<UInt, UInt>> {
                suspendLongActionProgress(0, 1000U) { index, value ->
                    sendBlocking(Pair(index, value)) // Or use offer without block
                }
                close()
            }
//            .buffer(1)
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

}