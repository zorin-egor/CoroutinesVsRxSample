package com.sample.coroutinesvsrxjava.managers

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_pair_text.view.*
import java.text.SimpleDateFormat
import java.util.*


fun String.toSpanned(context: Context, @ColorRes color: Int, style: Int = Typeface.NORMAL): Spanned {
    return toSpanned(ContextCompat.getColor(context, color), style)
}

fun String.toSpanned(@ColorInt color: Int, style: Int = Typeface.NORMAL): Spanned {
    return SpannableStringBuilder(this).apply {
        setSpan(StyleSpan(style), 0, this@toSpanned.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(ForegroundColorSpan(color), 0, this@toSpanned.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun getTime(format: String = "HH:mm:ss.SSS"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(Date())
}

operator fun Spanned.plus(value: Spanned): Spanned {
    return TextUtils.concat(this, value) as Spanned
}

fun TextView.append(text: Spanned, action: () -> Unit) {
    append(text)
    append("\n")
    action()
}

fun ScrollView.scrollBottom() {
    post {
        fullScroll(View.FOCUS_DOWN)
    }
}

fun View.getVisibleRect(leftOffset: Int, rightOffset: Int): Rect {
    return Rect().apply {
        guideLine.getGlobalVisibleRect(this)
        left -= leftOffset.toDp(resources).toInt()
        right += rightOffset.toDp(resources).toInt()
    }
}

fun ViewGroup.forEachChild(action: (View, Int) -> Unit) {
    (0 until childCount).forEach { index ->
        getChildAt(index)?.also { view ->
            action(view, index)
        }
    }
}

fun Int.toDp(resources: Resources): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), resources.displayMetrics)
}