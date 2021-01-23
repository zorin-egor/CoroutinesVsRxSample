package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.MutableLiveData
import com.sample.coroutinesvsrxjava.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.AsyncSubject
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@ExperimentalUnsignedTypes
class RxViewModel(application: Application) : BaseViewModel(application), Actions {

    companion object {
        val TAG = RxViewModel::class.java.simpleName
    }

    private val compositeDisposable = CompositeDisposable()

    override val result = MutableLiveData<Spanned?>()

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }

    override fun completable() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Completable.fromAction(::longActionResult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    start()
                }.doFinally {
                    message("doFinally()")
                }.subscribe {
                    result("Complete")
                }
        )
    }

    override fun single() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Single.fromCallable(::longActionResult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    start()
                }.doFinally {
                    message("doFinally()")
                }.subscribe { result ->
                    result(result)
                }
        )
    }

    override fun observable() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Pair<UInt, UInt>> { emitter ->
                val thread = threadActionEmit(emitter = { index, value ->
                    emitter.onNext(index to value)
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
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                emit(result)
            }
        )
    }

    override fun flow(items: UInt) {
        var processedItems = 0
        compositeDisposable.clear()
        compositeDisposable.add(
            Flowable.create<Pair<UInt, UInt>>({ emitter ->
                longActionEmit(0, items) { index, value ->
                    emitter.onNext(index to value)
                }
                emitter.onComplete()
            }, BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread(), false, 1)
            .doOnSubscribe {
                start()
                message("total items: $items")
            }.doFinally {
                message("processed items: $processedItems")
                message("doFinally()")
            }.subscribe { result ->
                ++processedItems
                emit(result)
            }
        )
    }

    override fun callback() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Single.create<UInt> { emitter ->
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
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                result(result)
            }
        )
    }

    override fun timeout() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Single.create<UInt> { emitter ->
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
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe({ result ->
                result(result)
            }, {
                message("subscribe(${it.message})")
            })
        )
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

        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.combineLatest(observableOne, observableTwo, observableThree) { one, two, three ->
                "$one-$two-$three"
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                emit(result)
            }
        )
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

        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.zip(observableOne, observableTwo, observableThree) { one, two, three ->
                "$one-$two-$three"
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                emit(result)
            }
        )
    }

    override fun flatMap() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Pair<UInt, UInt>> { emitter ->
                val thread = threadActionEmit(5000, 5U, { index, value ->
                    emitter.onNext(index to value)
                }, {
                    emitter.onComplete()
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .observeOn(Schedulers.io())
            .flatMap { pair ->
                Observable.create<String> { emitter ->
                    emitter.onNext("---- $pair")
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
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                emit(result)
            }
        )
    }

    override fun switchMap() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Pair<UInt, UInt>> { emitter ->
                val thread = threadActionEmit(5000, 5U, { index, value ->
                    emitter.onNext(index to value)
                }, {
                    emitter.onComplete()
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .observeOn(Schedulers.io())
            .switchMap { pair ->
                Observable.create<String> { emitter ->
                    emitter.onNext("---- $pair")
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
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                emit(result)
            }
        )
    }

    override fun concatMap() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Pair<UInt, UInt>> { emitter ->
                val thread = threadActionEmit(5000, 5U, { index, value ->
                    emitter.onNext(index to value)
                }, {
                    emitter.onComplete()
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .observeOn(Schedulers.io())
            .concatMap { pair ->
                Observable.create<String> { emitter ->
                    emitter.onNext("---- $pair")
                    val thread = threadActionEmit(2000, 4U, { index, value ->
                        emitter.onNext("${index + 1U}-$value-${Thread.currentThread().id}")
                    }, {
                        emitter.onComplete()
                    })
                    emitter.setCancellable {
                        thread.interrupt()
                    }
                }.observeOn(Schedulers.io())
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe { result ->
                emit(result)
            }
        )
    }

    override fun distinctUntilChanged() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.just("hello", "hello", "world")
                .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS)){ item, interval -> item }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .doOnSubscribe {
                    start()
                }.doFinally {
                    message("doFinally()")
                }.subscribe { result ->
                    emit(result)
                }
        )
    }


    override fun debounce() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
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
                    start()
                }.doFinally {
                    message("doFinally()")
                }.subscribe { result ->
                    emit(result)
                }
        )
    }

    override fun eventBus() {
        compositeDisposable.clear()

        val bus = when (Random.nextInt(4)) {
            0 -> BehaviorSubject.create<Int>().apply {
                observeOn(AndroidSchedulers.mainThread())
            }
            1 -> ReplaySubject.create<Int>().apply {
                observeOn(AndroidSchedulers.mainThread())
            }
            2 -> AsyncSubject.create<Int>().apply {
                observeOn(AndroidSchedulers.mainThread())
            }
            else -> PublishSubject.create<Int>().apply {
                observeOn(AndroidSchedulers.mainThread())
            }
        }
        
        compositeDisposable.add(
            bus.delaySubscription(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    emit("one - $it")
                }
        )

        compositeDisposable.add(
            bus.delaySubscription(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    emit("two - $it")
                }
        )

        compositeDisposable.add(
            Observable.intervalRange(1, 10, 0, 500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    start()
                    message( "Subject is: ${bus.javaClass.simpleName}")
                }.doFinally {
                    bus.onComplete()
                    message("doFinally()")
                }.subscribe {
                    bus.onNext(it.toInt())
                }
        )
    }

    override fun chains() {
        compositeDisposable.clear()

        val completable = Completable.fromAction(::longActionResult)
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { emit("Complete done", R.color.colorOne) }

        val single = Single.fromCallable(::longActionResult)
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterSuccess { emit("Single: ${it.toString()}", R.color.colorTwo) }
            .doFinally { emit("Single done") }

        val observable = Observable.create<Pair<UInt, UInt>> { emiter ->
            val thread = threadActionEmit(delay = 4000, count = 5U, { index, value ->
                emiter.onNext(index to value)
            }, {
                emiter.onComplete()
            })

            emiter.setCancellable { thread.interrupt() }
        }.observeOn(AndroidSchedulers.mainThread())
            .doAfterNext { emit("Observable: $it", R.color.colorThree) }
            .doFinally { emit("Observable done") }

        val flowable = Flowable.create<Pair<UInt, UInt>> ({ emiter ->
            val thread = threadActionEmit(delay = 2000, count = 5U, { index, value ->
                emiter.onNext(index to value)
            }, {
                emiter.onComplete()
            })

            emiter.setCancellable { thread.interrupt() }
        }, BackpressureStrategy.BUFFER)

        completable
            .observeOn(Schedulers.io())
            .andThen(single)
            .observeOn(Schedulers.io())
            .flatMapObservable { observable }
            .toFlowable(BackpressureStrategy.BUFFER)
            .observeOn(Schedulers.io())
            .switchMap { observableResult ->
                flowable
                    .observeOn(Schedulers.io())
                    .map {
                        "${observableResult.first} - $it - ${Thread.currentThread().id}"
                    }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { start() }
            .doFinally { message("doFinally()") }
            .subscribe { emit(it, R.color.colorFour) }
    }
}