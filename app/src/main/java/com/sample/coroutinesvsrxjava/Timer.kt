package com.sample.coroutinesvsrxjava

import android.os.CountDownTimer

class Timer {

    interface OnActionListener {
        fun onTimerTick(ms: Long, format: String)
        fun onTimerFinish()
    }

    var total: Long = 10 * 1000
    var interval: Long = 1000
    var isFinished: Boolean = false
    var listener: OnActionListener? = null

    private var mInnerTimer: InnerTimer? = null

    fun start() {
        stop()
        mInnerTimer = InnerTimer(total, interval)
        mInnerTimer?.start()
    }

    fun stop() {
        isFinished = false
        mInnerTimer?.cancel()
    }

    private inner class InnerTimer(total: Long, interval: Long) : CountDownTimer(total, interval) {
        override fun onTick(time: Long) {
            isFinished = false
            listener?.onTimerTick(time, time.toMinutes())
        }

        override fun onFinish() {
            isFinished = true
            listener?.onTimerFinish()
        }
    }

}