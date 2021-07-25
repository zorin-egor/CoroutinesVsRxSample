package com.sample.coroutinesvsrxjava.viewmodels

import android.app.Application
import android.text.Spanned
import android.util.Log
import androidx.annotation.ColorRes
import androidx.lifecycle.AndroidViewModel
import com.sample.coroutinesvsrxjava.R
import com.sample.coroutinesvsrxjava.managers.getTime
import com.sample.coroutinesvsrxjava.managers.plus
import com.sample.coroutinesvsrxjava.managers.toSpanned
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextUInt


@ExperimentalUnsignedTypes
abstract class BaseViewModel(private val app: Application) : AndroidViewModel(app) {

    companion object {
        private val TAG = BaseViewModel::class.java.simpleName
    }

    private val _result = MutableSharedFlow<Spanned?>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    val result: Flow<Spanned?> = _result.asSharedFlow()

    protected fun longActionResult(delay: Long = 1000): UInt {
        return try {
            Thread.sleep(delay)
            (Random.nextUInt() % 99U + 1U).also(::errorGenerator)
        } catch (e: InterruptedException) {
            0U
        }
    }

    protected fun longActionEmit(
        delay: Long = 3000,
        count: UInt = 10U,
        isError: Boolean = true,
        emitter: (UInt, UInt) -> Unit
    ) {
        try {
            val delayTime = delay / count.toLong()
            (0U until count).forEach {
                emitter(it, (Random.nextUInt() % 99U + 1U).also {
                    if (isError) {
                        errorGenerator(it)
                    }
                })
                if (delayTime > 0) {
                    Thread.sleep(delayTime)
                }
            }
        } catch (e: InterruptedException) {
            // Stub
        }
    }

    protected fun threadActionResult(delay: Long = 1000, emitter: (UInt) -> Unit, error: (Exception) -> Unit): Thread {
        return thread {
            try {
                emitter(longActionResult(delay))
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    protected fun threadActionEmit(
        delay: Long = 5000,
        count: UInt = 10U,
        emitter: (UInt, UInt) -> Unit,
        complete: () -> Unit,
        error: (Exception) -> Unit
    ): Thread {
        return thread {
            try {
                longActionEmit(delay, count, true) { index, value ->
                    emitter(index, value)
                }
                complete()
            } catch (e: Exception) {
                error(e)
            }
        }
    }

    protected fun <T> message(value: T) {
        val message = (getTime() + ": ").toSpanned(app, R.color.colorText) +
                value.toString().toSpanned(app, R.color.colorResult)

        Log.d(TAG, message.toString())
        _result.tryEmit(message)
    }

    protected fun start() {
        val message = (getTime() + ": ").toSpanned(app, R.color.colorText) +
                app.getString(R.string.action_start).toSpanned(app, R.color.colorStart)

        Log.d(TAG, message.toString())
        _result.tryEmit(null)
        _result.tryEmit(message)
    }

    protected fun <T> emit(value: T, @ColorRes colorId: Int = R.color.colorProgress) {
        val message = (getTime() + ": ").toSpanned(app, R.color.colorText) +
                app.getString(R.string.action_emit, value.toString()).toSpanned(app, colorId)

        Log.d(TAG, message.toString())
        _result.tryEmit(message)
    }

    protected fun <T> result(value: T) {
        val message = (getTime() + ": ").toSpanned(app, R.color.colorText) +
                app.getString(R.string.action_result, value.toString()).toSpanned(app, R.color.colorResult)

        Log.d(TAG, message.toString())
        _result.tryEmit(message)
    }

    protected fun <T> error(value: T? = null) {
        val message = (getTime() + ": ").toSpanned(app, R.color.colorText) +
                app.getString(R.string.action_error, (value ?: "-").toString()).toSpanned(app, R.color.colorError)

        Log.e(TAG, message.toString())
        _result.tryEmit(message)
    }

    private fun errorGenerator(value: UInt, mod: UInt = 15U) {
        if (value % mod == 0U) {
            throw IllegalArgumentException("Some unexpected error :)")
        }
    }

}