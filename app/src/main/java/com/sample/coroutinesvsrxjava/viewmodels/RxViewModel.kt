package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class RxViewModel(application: Application) : AndroidViewModel(application), ActionViewModel {

    companion object {
        val TAG = RxViewModel::class.java.simpleName
    }

    private var dispose: Disposable? = null

    override val result = MutableLiveData<Spanned?>()

    override fun single() {
        dispose?.dispose()
        dispose = Single.fromCallable {
            longAction()
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            startMessage(getApplication())
        }.subscribe { result ->
            resultMessage(getApplication(), result)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

}