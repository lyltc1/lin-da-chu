package com.lyltc1.lindachu.util

import java.util.Locale

/** 秒转 mm:ss，用于步骤计时 */
fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
