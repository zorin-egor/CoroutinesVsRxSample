package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.content.Context
import android.text.Spanned
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.sample.coroutinesvsrxjava.R
import com.sample.coroutinesvsrxjava.managers.getTime
import com.sample.coroutinesvsrxjava.managers.plus
import com.sample.coroutinesvsrxjava.managers.toSpanned
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextUInt


abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val TAG = BaseViewModel::class.java.simpleName
    }

    abstract val result: MutableLiveData<Spanned?>

    protected fun longActionResult(delay: Long = 1000): UInt {
        return try {
            Thread.sleep(delay)
            Random.nextUInt() % 99U + 1U
        } catch (e: InterruptedException) {
            0U
        }
    }

    protected fun longActionEmit(delay: Long = 3000, count: UInt = 10U, emitter: (UInt, UInt) -> Unit) {
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

    protected fun threadActionResult(delay: Long = 1000, emitter: (UInt) -> Unit): Thread {
        return thread {
            emitter(longActionResult(delay))
        }
    }

    protected fun threadActionEmit(
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

    protected fun <T> message(context: Context, value: T) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                value.toString().toSpanned(context, R.color.colorResult)

        Log.d(TAG, message.toString())
        result.value = message
    }

    protected fun start(context: Context) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_start).toSpanned(context, R.color.colorStart)

        Log.d(TAG, message.toString())
        result.value = null
        result.value = message
    }

    protected fun <T> emit(context: Context, value: T) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_emit, value.toString()).toSpanned(context, R.color.colorProgress)

        Log.d(TAG, message.toString())
        result.value = message
    }

    protected fun <T> result(context: Context, value: T) {
        val message = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_result, value.toString()).toSpanned(context, R.color.colorResult)

        Log.d(TAG, message.toString())
        result.value = message
    }

}