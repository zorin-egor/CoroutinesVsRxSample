package com.sample.coroutinesvsrxjava.viewmodels

import android.content.Context
import android.text.Spanned
import androidx.lifecycle.MutableLiveData
import com.sample.coroutinesvsrxjava.R
import com.sample.coroutinesvsrxjava.managers.getTime
import com.sample.coroutinesvsrxjava.managers.plus
import com.sample.coroutinesvsrxjava.managers.toSpanned
import kotlin.random.Random
import kotlin.random.nextUInt


interface ActionViewModel {

    val result: MutableLiveData<Spanned?>

    fun longAction(delay: Long = 3000): UInt {
        Thread.sleep(delay)
        return Random.nextUInt() % 100U
    }

    fun longActionProgress(delay: Long = 3000, progress: (Int) -> Unit): UInt {
        val parts = 10
        (0..parts).forEach {
            progress(it)
            Thread.sleep(delay / parts)
        }
        return Random.nextUInt() % 100U
    }

    fun startMessage(context: Context) {
        result.value = null
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_start).toSpanned(context, R.color.colorStart)
    }

    fun <T> progressMessage(context: Context, value: T) {
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_progress, value.toString()).toSpanned(context, R.color.colorProgress)
    }

    fun <T> resultMessage(context: Context, value: T) {
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_result, value.toString()).toSpanned(context, R.color.colorResult)
    }

    fun single()

}