package com.sample.coroutinesvsrxjava.viewmodels


@ExperimentalUnsignedTypes
interface Actions {

    fun completable()

    fun single()

    fun observable()

    fun flow(items: UInt = 1000U)

    fun callback()

    fun timeout()

    fun combineLatest()

    fun zip()

    fun flatMap()

    fun switchMap()

    fun concatMap()

    fun distinctUntilChanged()

    fun debounce()

    fun eventBus()

    fun chains()

    fun experiments()

}