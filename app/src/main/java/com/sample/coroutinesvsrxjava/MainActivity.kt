package com.sample.coroutinesvsrxjava

import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_pair_text.view.*

class MainActivity : AppCompatActivity() {

    private val rxLog: TextView
        get() = textLayout.rxActionText

    private val coroutineLog: TextView
        get() = textLayout.coroutineActionText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init(savedInstanceState)
    }

    private fun init(savedInstanceState: Bundle?) {
        rxLog.apply {
            text = getString(R.string.app_log_rx).toSpanned(this@MainActivity, android.R.color.black, Typeface.BOLD)
            movementMethod = ScrollingMovementMethod()
            setHorizontallyScrolling(true)
            setTextIsSelectable(true)
        }

        coroutineLog.apply {
            text = getString(R.string.app_log_coroutine).toSpanned(this@MainActivity, android.R.color.black, Typeface.BOLD)
            movementMethod = ScrollingMovementMethod()
            setHorizontallyScrolling(true)
            setTextIsSelectable(true)
        }

        (0..100).forEach {
            rxLog.append(it.toString().toSpanned(this, android.R.color.holo_green_light, Typeface.BOLD))
            rxLog.append(".............................................................................................")
            rxLog.append("\n")
        }
    }



}