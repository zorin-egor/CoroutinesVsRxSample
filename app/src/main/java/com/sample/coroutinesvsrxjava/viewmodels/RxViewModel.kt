package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class RxViewModel(application: Application) : AndroidViewModel(application), ActionViewModel {

    companion object {
        val TAG = RxViewModel::class.java.simpleName
    }

    private var dispose: Disposable? = null

    override val result = MutableLiveData<Spanned?>()

    override fun onCleared() {
        dispose?.dispose()
        super.onCleared()
    }

    override fun single() {
        dispose?.dispose()
        dispose = Single.fromCallable(::longActionResult)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start(getApplication())
            }
            .doFinally {
                message(getApplication(), "doFinally()")
            }
            .subscribe { result ->
                result(getApplication(), result)
            }
    }

    override fun observable() {
        dispose?.dispose()
        dispose = Observable.create<UInt> { emitter ->
            longActionEmit { index, value ->
                emitter.onNext(value)
            }
            emitter.onComplete()
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }
        .doFinally {
            message(getApplication(), "doFinally()")
        }
        .subscribe { result ->
            emit(getApplication(), result)
        }
    }

    override fun flow(items: UInt) {
        var processedItems = 0
        dispose?.dispose()
        dispose = Flowable.create<Pair<UInt, UInt>>({ emitter ->
            longActionEmit(0, items) { index, value ->
                emitter.onNext(Pair(index, value))
            }
            emitter.onComplete()
        }, BackpressureStrategy.LATEST)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread(), false, 1)
        .doOnSubscribe {
            start(getApplication())
            message(getApplication(), "total items: $items")
        }
        .doFinally {
            message(getApplication(), "processed items: $processedItems")
            message(getApplication(), "doFinally()")
        }
        .subscribe { result ->
            ++processedItems
            emit(getApplication(), result)
        }
    }

}