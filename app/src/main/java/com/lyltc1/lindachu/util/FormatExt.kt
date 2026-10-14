package com.lyltc1.lindachu.util

import java.util.Locale

/** 难度 1–5 转中文说明 */
fun difficultyLabel(level: Int): String = when (level) {
    1 -> "简单"
    2 -> "适中"
    3 -> "进阶"
    4, 5 -> "困难"
    else -> "适中"
}

/** 秒转 mm:ss，用于步骤计时 */
fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
