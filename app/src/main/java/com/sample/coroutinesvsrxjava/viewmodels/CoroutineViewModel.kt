package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class CoroutineViewModel(application: Application) : AndroidViewModel(application), ActionViewModel {

    companion object {
        val TAG = CoroutineViewModel::class.java.simpleName
    }

    private var job: Job? = null

    override val result = MutableLiveData<Spanned?>()

    private suspend fun suspendLongAction(): UInt {
        return runInterruptible(block = ::longAction)
    }

    private suspend fun suspendLongActionProgress(progress: (Int) -> Unit): UInt {
        return runInterruptible {
            longActionProgress{
                progress(it)
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

}