package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class RxViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = RxViewModel::class.java.simpleName
    }

    init {

    }

    override fun onCleared() {
        super.onCleared()
    }

}