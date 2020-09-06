package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoroutineViewModel(application: Application) : AndroidViewModel(application), ActionViewModel {

    companion object {
        val TAG = CoroutineViewModel::class.java.simpleName
    }

    private var job: Job? = null

    override val result = MutableLiveData<Spanned?>()

    override fun single() {
        job?.cancel()
        job = viewModelScope.launch {
            startMessage(getApplication())
            val result = withContext(Dispatchers.IO) {
                longAction()
            }
            resultMessage(getApplication(), result)
        }
    }

}