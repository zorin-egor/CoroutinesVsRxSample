package com.sample.coroutinesvsrxjava

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import com.sample.coroutinesvsrxjava.managers.*
import com.sample.coroutinesvsrxjava.viewmodels.CoroutineViewModel
import com.sample.coroutinesvsrxjava.viewmodels.RxViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.view_pair_text.*
import kotlinx.android.synthetic.main.view_pair_text.view.*

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private val mRxViewModel: RxViewModel by viewModels()

    private val mCoroutineViewModel: CoroutineViewModel by viewModels()

    private val rxLog: TextView
        get() = textLayout.rxActionText

    private val rxScroll: ScrollView
        get() = textLayout.rxActionScroll

    private val coroutineLog: TextView
        get() = textLayout.coroutineActionText

    private val coroutineScroll: ScrollView
        get() = textLayout.coroutineActionScroll

    private val isSimultaneousCheckBox: CheckBox
        get() = buttonsScroll.isSimultaneousAction

    private val isSimultaneousAction: Boolean
        get() = isSimultaneousCheckBox.isChecked

    private val buttonsTitles: Array<String> by lazy {
        resources.getStringArray(R.array.button_titles)
    }

    private val onTouchListener = View.OnTouchListener { view, event ->
        when ((event.actionMasked)) {
            MotionEvent.ACTION_MOVE -> {
                setGuidLineBias(event.rawX)
                return@OnTouchListener true
            }
            MotionEvent.ACTION_UP -> {
                setGuidLineBias(event.rawX, true)
                return@OnTouchListener true
            }
        }
        false
    }

    private val onClickListener = View.OnClickListener {
        when ((it.parent as? ViewGroup)?.tag) {
            getString(R.string.action_type_single) -> {
                setButtonsAction(it.id, mRxViewModel::single, mCoroutineViewModel::single)
            }
            getString(R.string.action_type_observer) -> {
                setButtonsAction(it.id, mRxViewModel::observable, mCoroutineViewModel::observable)
            }
            getString(R.string.action_type_flow) -> {
                setButtonsAction(it.id, mRxViewModel::flow, mCoroutineViewModel::flow)
            }
        }
    }

    private fun setButtonsAction(@IdRes buttonsId: Int, actionRx: () -> Unit,  actionCoroutine: () -> Unit) {
        when {
            isSimultaneousAction -> {
                actionRx()
                actionCoroutine()
            }
            R.id.rxActionButton == buttonsId -> {
                actionRx()
            }
            R.id.coroutineActionButton == buttonsId -> {
                actionCoroutine()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init(savedInstanceState)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when ((event.actionMasked)) {
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                guideLine.getVisibleRect(40, 40).takeIf {
                    it.contains(event.rawX.toInt(), event.rawY.toInt())
                }?.also {
                    onTouchListener.onTouch(guideLine, event)
                    return true
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    private fun init(savedInstanceState: Bundle?) {
        setRxInitialText()
        setCoroutineInitialText()

        isSimultaneousCheckBox.setOnCheckedChangeListener { view, isChecked ->
            setBackground(isChecked)
        }

        addButtonsToContainer()
        setData()
    }

    private fun setData() {
        mCoroutineViewModel.result.observe(this) {
            when (it) {
                null -> setCoroutineInitialText()
                else -> coroutineLog.append(it!!) {
                    coroutineScroll.scrollBottom()
                }
            }
        }

        mRxViewModel.result.observe(this) {
            when (it) {
                null -> setRxInitialText()
                else -> rxLog.append(it!!) {
                    rxScroll.scrollBottom()
                }
            }
        }
    }

    private fun setRxInitialText() {
        rxLog.apply {
            text = getString(R.string.log_rx).toSpanned(this@MainActivity, android.R.color.black, Typeface.BOLD)
            setTextIsSelectable(true)
        }
    }

    private fun setCoroutineInitialText() {
        coroutineLog.apply {
            text = getString(R.string.log_coroutine).toSpanned(this@MainActivity, android.R.color.black, Typeface.BOLD)
            setTextIsSelectable(true)
        }
    }



    private fun setBackground(isChecked: Boolean) {
        val colorStart = ContextCompat.getColor(this, if (isChecked) android.R.color.transparent else R.color.colorAccent)
        val colorEnd = ContextCompat.getColor(this, if (isChecked) R.color.colorAccent else android.R.color.transparent)
        buttonsContainer.forEachChild { view, index ->
            ObjectAnimator.ofObject(view, "backgroundColor", ArgbEvaluator(), colorStart, colorEnd).apply {
                duration = 1000
            }.start()
        }
    }

    private fun setGuidLineBias(horizontal: Float, isTransition: Boolean = false) {
        (textLayout as? ConstraintLayout).also { layout ->
            val width = Resources.getSystem().displayMetrics.widthPixels.toFloat()
            val bias = horizontal / width
            val value = if (isTransition) {
                TransitionManager.beginDelayedTransition(layout)
                when {
                    bias < 0.1 -> 0.1f
                    bias > 0.9 -> 0.9f
                    0.35 < bias && bias < 0.65 -> 0.5f
                    else -> bias
                }
            } else {
                bias
            }

            ConstraintSet().apply {
                clone(layout)
                setHorizontalBias(guideLine.id, value)
                applyTo(layout)
            }
        }
    }

    private fun addButtonsToContainer() {
        buttonsTitles.forEach { title ->
            buttonsContainer.addView(getButtonsLayout(title).also { container ->
                container.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    updateMargins(bottom = 2.toDp(resources).toInt())
                }
            })
        }
    }

    private fun getButtonsLayout(title: String): View {
        return (LayoutInflater.from(this).inflate(R.layout.view_pair_buttons, null) as ViewGroup).apply {
            id = View.generateViewId()
            tag = title
            forEachChild { view, i ->
                (view as? Button)?.apply {
                    setOnClickListener(onClickListener)
                    text = title
                }
            }
        }
    }

}