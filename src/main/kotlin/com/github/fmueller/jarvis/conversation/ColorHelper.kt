package com.github.fmueller.jarvis.conversation

import java.awt.Color

object ColorHelper {

    fun Color.darker(factor: Double): Color {
        val r = (red * factor).toInt().coerceIn(0, 255)
        val g = (green * factor).toInt().coerceIn(0, 255)
        val b = (blue * factor).toInt().coerceIn(0, 255)
        return Color(r, g, b, alpha)
    }
}
