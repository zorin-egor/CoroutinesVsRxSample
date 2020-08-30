package com.sample.coroutinevsrxjava

fun Long.toMinutes(): String {
    val s = this / 1000 % 60
    val m = this / (1000 * 60) % 60
    return String.format("%02d:%02d", m, s)
}