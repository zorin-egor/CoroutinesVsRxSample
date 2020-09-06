package com.sample.coroutinesvsrxjava.managers

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*


fun Long.toMinutes(): String {
    val s = this / 1000 % 60
    val m = this / (1000 * 60) % 60
    return String.format("%02d:%02d", m, s)
}

fun String.toSpanned(context: Context, @ColorRes color: Int, style: Int = Typeface.NORMAL): Spanned {
    return toSpanned(ContextCompat.getColor(context, color), style)
}

fun String.toSpanned(@ColorInt color: Int, style: Int = Typeface.NORMAL): Spanned {
    return SpannableStringBuilder(this).apply {
        setSpan(StyleSpan(style), 0, this@toSpanned.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(ForegroundColorSpan(color), 0, this@toSpanned.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun getTime(format: String = "HH:mm:ss"): String {
    return SimpleDateFormat(format).format(Date())
}

operator fun Spanned.plus(value: Spanned): Spanned {
    return TextUtils.concat(this, value) as Spanned
}