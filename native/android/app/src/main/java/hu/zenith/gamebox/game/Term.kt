package hu.zenith.gamebox.game

import android.graphics.Color
import kotlin.math.floor
import kotlin.random.Random

/** Terminál-cellás rács: az eredeti shell (ANSI) grafika natív megfelelője. */

const val CW = 12
const val CH = 24

fun hex(s: String): Int = Color.parseColor(s)

val WHITE_I: Int = hex("#ffffff")
val BLACK_I: Int = hex("#000000")

class Cell(
    var ch: String = " ",
    var fg: Int = WHITE_I,
    var bg: Int = BLACK_I,
    var wide: Boolean = false,
    var ox: Float = 0f,
    var oy: Float = 0f,
)

/** HUD-szakasz: szöveg és (opcionális) szín. null szín = alapértelmezett szövegszín. */
data class Seg(val t: String, val c: Int? = null)

private fun isWide(cp: Int): Boolean = cp >= 0x1f000

private fun cpString(cp: Int): String = String(Character.toChars(cp))

fun cpCount(s: String): Int = s.codePointCount(0, s.length)

fun String.rep(n: Int): String = if (n <= 0) "" else this.repeat(n)

class Grid(val cols: Int, val rows: Int, bg: Int = BLACK_I) {
    val cells: Array<Array<Cell>> = Array(rows) { Array(cols) { Cell(" ", WHITE_I, bg) } }

    fun fill(bg: Int) {
        for (r in cells) for (c in r) {
            c.ch = " "; c.bg = bg; c.wide = false; c.ox = 0f; c.oy = 0f
        }
    }

    /** Szöveg kiírása a rács x,y pozíciójára. Az emoji / széles karakter két cellát foglal. */
    fun put(xIn: Number, yIn: Number, str: String, fg: Int, bg: Int? = null, minX: Int = 0, maxX: Int = cols - 1) {
        val xd = xIn.toDouble()
        val yd = yIn.toDouble()
        val rx = floor(xd + 0.5)
        val ry = floor(yd + 0.5)
        val ox = (xd - rx).toFloat()
        val oy = (yd - ry).toFloat()
        var x = rx.toInt()
        val y = ry.toInt()
        if (y < 0 || y >= rows) return
        val row = cells[y]
        var i = 0
        while (i < str.length) {
            val cp = str.codePointAt(i)
            i += Character.charCount(cp)
            val wide = isWide(cp)
            if (x >= minX && x <= maxX && x >= 0 && x < cols) {
                val cell = row[x]
                cell.ch = cpString(cp); cell.fg = fg; cell.wide = wide; cell.ox = ox; cell.oy = oy
                if (bg != null) cell.bg = bg
                if (wide && x + 1 <= maxX && x + 1 < cols) {
                    val n = row[x + 1]
                    n.ch = ""; n.wide = false
                    if (bg != null) n.bg = bg
                }
            }
            x += if (wide) 2 else 1
        }
    }

    /** Zenith keret: ╔═╗ / ║ ║ / ╚═╝ egy W×H mező körül. */
    fun frame(w: Int, h: Int, side: Int, glow: Int, bg: Int) {
        fill(bg)
        put(0, 0, "╔" + "═".rep(w) + "╗", glow)
        put(0, h + 1, "╚" + "═".rep(w) + "╝", glow)
        for (y in 1..h) {
            put(0, y, "║", side)
            put(w + 1, y, "║", side)
        }
    }

    /** Írás a belső mezőbe a shell 0-alapú oszlopindexével. */
    fun inner(i: Number, y: Number, str: String, fg: Int, bg: Int? = null) {
        put(i.toDouble() + 1.0, y, str, fg, bg, 1, cols - 2)
    }

    fun center(y: Number, str: String, fg: Int, w: Int) {
        inner(floor((w - cpCount(str)) / 2.0), y, str, fg)
    }
}

/** A játékok közös felülete. */
abstract class TermGame(val W: Int, val H: Int) {
    var started: Boolean = false
    var over: Boolean = false
    open var score: Double = 0.0

    abstract fun delay(): Double
    open fun start() { started = true }
    abstract fun tick()
    abstract fun draw(g: Grid)
    abstract fun hud(): List<Seg>

    /** Koppintás: belső oszlop (0-alapú) és a képernyő oldala (-1 bal / 1 jobb). */
    open fun tap(col: Int, side: Int) {}

    /** Húzás: vízszintes elmozdulás oszlopban az előző eseményhez képest. */
    open fun drag(dxCols: Double) {}

    /** Négyirányú joystick-lépés (-1/0/1). */
    open fun dir(dx: Int, dy: Int) {}

    /** Joystick-nyomás (kitérés nélkül felengedett koppintás). */
    open fun press() {}

    /** Ha igaz, a vászon koppintása közvetlenül cellára (oszlop, sor) érkezik. */
    open val usesTapAt: Boolean = false
    open fun tapAt(col: Int, row: Int) {}
}

class GameOpts(
    val best: Int,
    val opts: Map<String, String>,
    val vibrate: (Long) -> Unit,
)

fun rnd(n: Int): Int = Random.nextInt(n)

/** Másodperc (monoton óra), az eredeti performance.now()/1000 megfelelője. */
fun nowSec(): Double = System.nanoTime() / 1_000_000_000.0
