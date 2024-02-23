package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import com.sample.coroutinesvsrxjava.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@ExperimentalUnsignedTypes
class RxViewModel(application: Application) : BaseViewModel(application), Actions {

    private val compositeDisposable = CompositeDisposable()

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
                }.subscribe({
                    result("Complete")
                }, {
                    error(it.message)
                })
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
                }.subscribe({ result ->
                    result(result)
                }, {
                    error(it.message)
                })
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
                }, error = {
                    emitter.onError(it)
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
            }.subscribe({ result ->
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun flow(items: UInt) {
        var processedItems = 0
        compositeDisposable.clear()
        compositeDisposable.add(
            Flowable.create<Pair<UInt, UInt>>({ emitter ->
                longActionEmit(0, items, false) { index, value ->
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
            }.subscribe({ result ->
                ++processedItems
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun callback() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Single.create<UInt> { emitter ->
                val thread = threadActionResult(delay = 2000, emitter = {
                    emitter.onSuccess(it)
                }, error = {
                    emitter.onError(it)
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
            }.subscribe({ result ->
                result(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun timeout() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Single.create<UInt> { emitter ->
                val thread = threadActionResult(delay = 3000, emitter = {
                    emitter.onSuccess(it)
                }, error = {
                    emitter.onError(it)
                })

                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .timeout(2000, TimeUnit.MILLISECONDS, Schedulers.trampoline())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe({ result ->
                result(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun combineLatest() {
        val observableA = Observable.create<String> { emitter ->
            val thread = threadActionEmit(delay = 7000, count = 7U, emitter = { index, value ->
                emitter.onNext((index + 1U).toString())
            }, complete = {
                emitter.onComplete()
            }, error = {
                emitter.onError(it)
            })

            emitter.setCancellable {
                thread.interrupt()
            }
        }

        val observableB = Observable.just("1", "2", "3", "4", "5")
            .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS), { item, interval -> item })

        val observableC = Observable.just("1", "2", "3", "4", "5", "6")
            .zipWith(Observable.interval(250, TimeUnit.MILLISECONDS), { item, interval -> item })

        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.combineLatest(observableA, observableB, observableC) { one, two, three ->
                "A$one-B$two-C$three"
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe({ result ->
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun zip() {
        val observableA = Observable.create<String> { emitter ->
            val thread = threadActionEmit(delay = 7000, count = 7U, emitter = { index, value ->
                emitter.onNext((index + 1U).toString())
            }, complete = {
                emitter.onComplete()
            }, error = {
                emitter.onError(it)
            })

            emitter.setCancellable {
                thread.interrupt()
            }
        }

        val observableB = Observable.just("1", "2", "3", "4", "5")
            .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS)){ item, interval -> item }

        val observableC = Observable.just("1", "2", "3", "4", "5", "6")
            .zipWith(Observable.interval(250, TimeUnit.MILLISECONDS)) { item, interval -> item }

        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.zip(observableA, observableB, observableC) { one, two, three ->
                "A$one-B$two-C$three"
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                start()
            }.doFinally {
                message("doFinally()")
            }.subscribe({ result ->
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun flatMap() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Triple<UInt, UInt, String>> { emitter ->
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 5000, count = 5U, emitter = { index, value ->
                    emitter.onNext(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    emitter.onComplete()
                }, error = {
                    emitter.onError(it)
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .observeOn(Schedulers.io())
            .flatMap { triple ->
                Observable.create<String> { emitter ->
                    emitter.onNext("--------------------------------------------------------")
                    emitter.onNext(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        emitter.onNext("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
                    }, complete = {
                        emitter.onComplete()
                    }, error = {
                        emitter.onError(it)
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
            }.subscribe({ result ->
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun switchMap() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Triple<UInt, UInt, String>> { emitter ->
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 5000, count = 5U, emitter = { index, value ->
                    emitter.onNext(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    emitter.onComplete()
                }, error = {
                    emitter.onError(it)
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .observeOn(Schedulers.io())
            .switchMap { triple ->
                Observable.create<String> { emitter ->
                    emitter.onNext("--------------------------------------------------------")
                    emitter.onNext(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        emitter.onNext("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
                    }, complete = {
                        emitter.onComplete()
                    }, error = {
                        emitter.onError(it)
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
            }.subscribe({ result ->
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun concatMap() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.create<Triple<UInt, UInt, String>> { emitter ->
                val threadName = Thread.currentThread().name
                val thread = threadActionEmit(delay = 5000, count = 5U, emitter = { index, value ->
                    emitter.onNext(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
                }, complete = {
                    emitter.onComplete()
                }, error = {
                    emitter.onError(it)
                })
                emitter.setCancellable {
                    thread.interrupt()
                }
            }
            .observeOn(Schedulers.io())
            .concatMap { triple ->
                Observable.create<String> { emitter ->
                    emitter.onNext("--------------------------------------------------------")
                    emitter.onNext(">$triple")

                    val threadName = Thread.currentThread().name
                    val thread = threadActionEmit(delay = 2000, count = 4U, emitter = { index, value ->
                        emitter.onNext("${index + 1U}-$value, ${Thread.currentThread().name}, $threadName")
                    }, complete = {
                        emitter.onComplete()
                    }, error = {
                        emitter.onError(it)
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
            }.subscribe({ result ->
                emit(result)
            }, {
                error(it.message)
            })
        )
    }

    override fun distinctUntilChanged() {
        compositeDisposable.clear()
        compositeDisposable.add(
            Observable.just("hello", "hello", "world", "world")
                .zipWith(Observable.interval(500, TimeUnit.MILLISECONDS, Schedulers.trampoline())) { item, interval ->
                    item to Thread.currentThread().name
                }
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
                        Observable.just(it)
                            .delay(1010, TimeUnit.MILLISECONDS, Schedulers.io())
                    } else {
                        Observable.just(it)
                    }
                }
                .debounce(1000, TimeUnit.MILLISECONDS, Schedulers.io())
                .map {
                    "$it-${Thread.currentThread().name}"
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

    override fun eventBus() {
        compositeDisposable.clear()

        val bus = when (Random.nextInt(4)) {
            0 -> BehaviorSubject.create<Int>()
            1 -> ReplaySubject.create<Int>()
            2 -> AsyncSubject.create<Int>()
            3 -> UnicastSubject.create<Int>()
            else -> PublishSubject.create<Int>()
        }
        
        compositeDisposable.add(
            bus.delaySubscription(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    emit("one - $it")
                }, {
                    error(it.message)
                })
        )

        compositeDisposable.add(
            bus.delaySubscription(2000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    emit("two - $it")
                }, {
                    error(it.message)
                })
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

        val observable = Observable.create<Triple<UInt, UInt, String>> { emiter ->
            val threadName = Thread.currentThread().name
            val thread = threadActionEmit(delay = 4000, count = 5U, emitter = { index, value ->
                emiter.onNext(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
            }, complete = {
                emiter.onComplete()
            }, error = {
                emiter.onError(it)
            })

            emiter.setCancellable { thread.interrupt() }
        }.observeOn(AndroidSchedulers.mainThread())
            .doOnNext { emit("Observable: $it", R.color.colorThree) }
            .doFinally { emit("Observable done") } // Wrong thread with switchMap http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#doFinally-io.reactivex.functions.Action-

        val flowable = Flowable.create<Triple<UInt, UInt, String>> ({ emiter ->
            val threadName = Thread.currentThread().name
            val thread = threadActionEmit(delay = 2000, count = 5U, emitter = { index, value ->
                emiter.onNext(Triple(index, value, "${Thread.currentThread().name}, $threadName"))
            }, complete = {
                emiter.onComplete()
            }, error = {
                emiter.onError(it)
            })

            emiter.setCancellable { thread.interrupt() }
        }, BackpressureStrategy.BUFFER)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { emit("Flowable: $it", R.color.colorFour) }
            .doFinally { emit("Flowable done") } // Wrong thread with switchMap http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#doFinally-io.reactivex.functions.Action-

        compositeDisposable.add(completable
            .observeOn(Schedulers.io())
            .andThen(single)
            .observeOn(Schedulers.io())
            .flatMapObservable { observable }
            .toFlowable(BackpressureStrategy.BUFFER)
            .observeOn(Schedulers.io())
//            .switchMap { flowable } // Changed thread for doFinally despite observeOn
            .flatMap { flowable }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { start() }
            .doFinally { message("doFinally()") }
            .subscribe({
                // Stub
            }, {
                error(it.message)
            })
        );
    }
}