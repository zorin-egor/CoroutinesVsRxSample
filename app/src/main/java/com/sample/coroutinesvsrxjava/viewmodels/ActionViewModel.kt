package com.sample.coroutinesvsrxjava.viewmodels

import android.content.Context
import android.text.Spanned
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.sample.coroutinesvsrxjava.R
import com.sample.coroutinesvsrxjava.managers.getTime
import com.sample.coroutinesvsrxjava.managers.plus
import com.sample.coroutinesvsrxjava.managers.toSpanned
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextUInt


interface ActionViewModel {

    companion object {
        val TAG = ActionViewModel::class.java.simpleName
    }

    val result: MutableLiveData<Spanned?>

    fun longActionResult(delay: Long = 1000): UInt {
        return try {
            Thread.sleep(delay)
            Random.nextUInt() % 99U + 1U
        } catch (e: InterruptedException) {
            0U
        }
    }

    fun longActionEmit(delay: Long = 3000, count: UInt = 10U, emitter: (UInt, UInt) -> Unit) {
        try {
            val delayTime = delay / count.toLong()
            (0U until count).forEach {
                emitter(it, Random.nextUInt() % 99U + 1U)
                if (delayTime > 0) {
                    Thread.sleep(delayTime)
                }
            }
        } catch (e: InterruptedException) {
            // Stub
        }
    }

    fun threadActionResult(delay: Long = 1000, emitter: (UInt) -> Unit): Thread {
        return thread {
            emitter(longActionResult(delay))
        }
    }

    fun threadActionEmit(
        delay: Long = 3000,
        count: UInt = 10U,
        emitter: (UInt, UInt) -> Unit,
        complete: () -> Unit
    ): Thread {
        return thread {
            longActionEmit(delay, count) { index, value ->
                emitter(index, value)
            }
            complete()
        }
    }

    fun <T> message(context: Context, value: T) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                value.toString().toSpanned(context, R.color.colorResult)

        Log.d(TAG, message.toString())
        result.value = message
    }

    fun start(context: Context) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_start).toSpanned(context, R.color.colorStart)

        Log.d(TAG, message.toString())
        result.value = null
        result.value = message
    }

    fun <T> emit(context: Context, value: T) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_emit, value.toString()).toSpanned(context, R.color.colorProgress)

        Log.d(TAG, message.toString())
        result.value = message
    }

    fun <T> result(context: Context, value: T) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_result, value.toString()).toSpanned(context, R.color.colorResult)

        Log.d(TAG, message.toString())
        result.value = message
    }

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

}