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
        return try {
            Thread.sleep(delay)
            Random.nextUInt() % 99U + 1U
        } catch (e: InterruptedException) {
            0U
        }
    }

    fun longActionProgress(delay: Long = 3000, progress: (Int) -> Unit): UInt {
        return try {
            val parts = 10
            (0..parts).forEach {
                progress(it)
                Thread.sleep(delay / parts)
            }
            Random.nextUInt() % 99U + 1U
        } catch (e: InterruptedException) {
            0U
        }
    }

    fun <T> message(context: Context, value: T) {
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                value.toString().toSpanned(context, R.color.colorResult)
    }

    fun start(context: Context) {
        result.value = null
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_start).toSpanned(context, R.color.colorStart)
    }

    fun <T> progress(context: Context, value: T) {
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_progress, value.toString()).toSpanned(context, R.color.colorProgress)
    }

    fun <T> result(context: Context, value: T) {
        result.value = (getTime() + ": ").toSpanned(context, R.color.colorText) +
                context.getString(R.string.action_result, value.toString()).toSpanned(context, R.color.colorResult)
    }

    fun single()

}