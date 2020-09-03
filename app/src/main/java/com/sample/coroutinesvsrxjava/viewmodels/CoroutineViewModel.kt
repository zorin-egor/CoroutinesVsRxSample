package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class CoroutineViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = CoroutineViewModel::class.java.simpleName
    }

    init {

    }

    override fun onCleared() {
        super.onCleared()
    }
}