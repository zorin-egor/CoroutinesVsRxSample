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
import java.util.concurrent.TimeUnit

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
            }.doFinally {
                message(getApplication(), "doFinally()")
            }.subscribe { result ->
                result(getApplication(), result)
            }
    }

    override fun observable() {
        dispose?.dispose()
        dispose = Observable.create<Pair<UInt, UInt>> { emitter ->
            val thread = threadActionEmit(emitter = { index, value ->
                emitter.onNext(Pair(index, value))
            }, complete = {
                emitter.onComplete()
            })

            emitter.setCancellable {
                thread.interrupt()
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
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
        }.doFinally {
            message(getApplication(), "processed items: $processedItems")
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            ++processedItems
            emit(getApplication(), result)
        }
    }

    override fun callback() {
        dispose?.dispose()
        dispose = Single.create<UInt> { emitter ->
            val thread = threadActionResult(2000) {
                emitter.onSuccess(it)
            }

            emitter.setCancellable {
                thread.interrupt()
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            result(getApplication(), result)
        }
    }

    override fun timeout() {
        dispose?.dispose()
        dispose = Single.create<UInt> { emitter ->
            val thread = threadActionResult(3000) {
                emitter.onSuccess(it)
            }

            emitter.setCancellable {
                thread.interrupt()
            }
        }
        .timeout(2000, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe({ result ->
            result(getApplication(), result)
        }, {
            message(getApplication(), "subscribe(${it.message})")
        })
    }

    override fun combineLatest() {
        val observableOne = Observable.create<String> { emitter ->
            val thread = threadActionEmit(5000, 5U, { index, value ->
                emitter.onNext((index + 1U).toString())
            }, {
                emitter.onComplete()
            })

            emitter.setCancellable {
                thread.interrupt()
            }
        }

        val observableTwo = Observable.just("1", "2", "3", "4", "5")
            .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS), { item, interval -> item })

        val observableThree = Observable.just("1", "2", "3", "4", "5")
            .zipWith(Observable.interval(250, TimeUnit.MILLISECONDS), { item, interval -> item })

        dispose?.dispose()
        dispose = Observable.combineLatest(observableOne, observableTwo, observableThree) { one, two, three ->
            "$one-$two-$three"
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            emit(getApplication(), result)
        }
    }

    override fun zip() {
        val observableOne = Observable.create<String> { emitter ->
            val thread = threadActionEmit(5000, 5U, { index, value ->
                emitter.onNext((index + 1U).toString())
            }, {
                emitter.onComplete()
            })

            emitter.setCancellable {
                thread.interrupt()
            }
        }

        val observableTwo = Observable.just("1", "2", "3", "4", "5")
            .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS)){ item, interval -> item }

        val observableThree = Observable.just("1", "2", "3", "4", "5")
            .zipWith(Observable.interval(250, TimeUnit.MILLISECONDS)) { item, interval -> item }

        dispose?.dispose()
        dispose = Observable.zip(observableOne, observableTwo, observableThree) { one, two, three ->
            "$one-$two-$three"
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            emit(getApplication(), result)
        }
    }

    override fun flatMap() {
        dispose?.dispose()
        dispose = Observable.create<Pair<UInt, UInt>> { emitter ->
            val thread = threadActionEmit(5000, 5U, { index, value ->
                emitter.onNext(Pair(index, value))
            }, {
                emitter.onComplete()
            })
            emitter.setCancellable {
                thread.interrupt()
            }
        }
        .flatMap { pair ->
            Observable.create<String> { emitter ->
                emitter.onNext("----------")
                val thread = threadActionEmit(2000, 4U, { index, value ->
                    emitter.onNext("${index + 1U}-$value-${Thread.currentThread().id}")
                }, {
                    emitter.onComplete()
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            emit(getApplication(), result)
        }
    }

    override fun switchMap() {
        dispose?.dispose()
        dispose = Observable.create<Pair<UInt, UInt>> { emitter ->
            val thread = threadActionEmit(5000, 5U, { index, value ->
                emitter.onNext(Pair(index, value))
            }, {
                emitter.onComplete()
            })
            emitter.setCancellable {
                thread.interrupt()
            }
        }
        .switchMap { pair ->
            Observable.create<String> { emitter ->
                emitter.onNext("----------")
                val thread = threadActionEmit(2000, 4U, { index, value ->
                    emitter.onNext("${index + 1U}-$value-${Thread.currentThread().id}")
                }, {
                    emitter.onComplete()
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            emit(getApplication(), result)
        }
    }

    override fun concatMap() {
        dispose?.dispose()
        dispose = Observable.create<Pair<UInt, UInt>> { emitter ->
            val thread = threadActionEmit(5000, 5U, { index, value ->
                emitter.onNext(Pair(index, value))
            }, {
                emitter.onComplete()
            })
            emitter.setCancellable {
                thread.interrupt()
            }
        }
        .concatMap { pair ->
            Observable.create<String> { emitter ->
                emitter.onNext("----------")
                val thread = threadActionEmit(2000, 4U, { index, value ->
                    emitter.onNext("${index + 1U}-$value-${Thread.currentThread().id}")
                }, {
                    emitter.onComplete()
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
            start(getApplication())
        }.doFinally {
            message(getApplication(), "doFinally()")
        }.subscribe { result ->
            emit(getApplication(), result)
        }
    }

    override fun distinctUntilChanged() {
        dispose?.dispose()
        dispose = Observable.just("hello", "hello", "world")
            .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS)){ item, interval -> item }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()
            .doOnSubscribe {
                start(getApplication())
            }.doFinally {
                message(getApplication(), "doFinally()")
            }.subscribe { result ->
                emit(getApplication(), result)
            }
    }


    override fun debounce() {
        dispose?.dispose()
        dispose = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .concatMap {
                if (it % 2 == 0) {
                    Observable.just(it).delay(1010, TimeUnit.MILLISECONDS)
                } else {
                    Observable.just(it)
                }
            }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start(getApplication())
            }.doFinally {
                message(getApplication(), "doFinally()")
            }.subscribe { result ->
                emit(getApplication(), result)
            }

    }

}