package hu.zenith.gamebox.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** A webes konzol oklch-palettájának sRGB megfelelői. */
object Z {
    val Background = Color(0xFF060410)
    val Foreground = Color(0xFFF4EFFE)
    val Card = Color(0xFF110B1F)
    val Primary = Color(0xFFAD8DFD)
    val PrimaryFg = Color(0xFF060410)
    val Secondary = Color(0xFF211B30)
    val MutedFg = Color(0xFF918AAB)
    val Border = Color(0xFF352C4E)
    val Accent = Color(0xFFFBD530)
    val Bird = Color(0xFFFFFF11)
    val Dino = Color(0xFFB98CFF)
    val Pong = Color(0xFFFF6600)
    val Space = Color(0xFF2BFFFF)
    val Jump = Color(0xFF5DFD6D)
    val Term: FontFamily = FontFamily.Monospace
}

fun inkOf(id: String): Color = when (id) {
    "bird" -> Z.Bird
    "dino" -> Z.Dino
    "pong" -> Z.Pong
    "space" -> Z.Space
    "jump" -> Z.Jump
    "snake" -> Z.Jump
    "memory" -> Z.Dino
    else -> Z.Foreground
}
