package hu.zenith.gamebox.ui

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import hu.zenith.gamebox.game.CH
import hu.zenith.gamebox.game.CW
import hu.zenith.gamebox.game.Grid

/** A játék rácsa és egy képkocka-számláló, amelynek változása újrarajzoltatja a vásznat. */
@Stable
class GridHolder {
    var grid: Grid? = null
    val frame = mutableLongStateOf(0L)
}

@androidx.compose.runtime.Composable
fun GridCanvas(holder: GridHolder, modifier: Modifier) {
    Canvas(modifier) {
        holder.frame.longValue // olvasás: ettől rajzolódik újra
        val g = holder.grid ?: return@Canvas
        val s = size.width / (g.cols * CW)
        drawIntoCanvas { cv ->
            val nc = cv.nativeCanvas
            nc.save()
            nc.scale(s, s)
            GridRenderer.render(nc, g)
            nc.restore()
        }
    }
}

/** Az eredeti canvas-rajzoló (renderGrid) natív megfelelője. */
object GridRenderer {
    private val W: Float = CW.toFloat()
    private val H: Float = CH.toFloat()
    private const val M = 2f

    private val fillP = Paint().apply { style = Paint.Style.FILL; isAntiAlias = false }
    private val shapeP = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val textP = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = (CH - 5).toFloat()
        textAlign = Paint.Align.CENTER
    }
    private val emojiP = Paint().apply {
        isAntiAlias = true
        textSize = (CH - 4).toFloat()
        textAlign = Paint.Align.CENTER
    }
    private val path = Path()

    // Ezeket a jeleket mindig szöveg-megjelenítéssel kérjük (ne színes emoji legyen belőlük).
    private const val TEXT_STYLE = "◀▶▲▼●◆♥♦♣♠★❯❮"

    fun render(c: AndroidCanvas, g: Grid) {
        for (y in 0 until g.rows) for (x in 0 until g.cols) {
            val cell = g.cells[y][x]
            fillP.color = cell.bg
            c.drawRect(x * W, y * H, x * W + W + 0.5f, y * H + H + 0.5f, fillP)
        }
        for (y in 0 until g.rows) for (x in 0 until g.cols) {
            val cell = g.cells[y][x]
            val ch = cell.ch
            if (ch.isEmpty() || ch == " ") continue
            val px = (x + cell.ox) * W
            val py = (y + cell.oy) * H
            if (drawBlock(c, ch, px, py, cell.fg)) continue
            if (cell.wide) {
                emojiP.color = cell.fg
                val fm = emojiP.fontMetrics
                c.drawText(ch, px + W, py + H / 2 + 1 - (fm.ascent + fm.descent) / 2, emojiP)
            } else {
                textP.color = cell.fg
                val fm = textP.fontMetrics
                val text = if (ch.length == 1 && TEXT_STYLE.indexOf(ch[0]) >= 0) ch + "\uFE0E" else ch
                val tw = textP.measureText(text)
                val cx = px + W / 2
                val by = py + H / 2 + 1 - (fm.ascent + fm.descent) / 2
                if (tw > W + 2) {
                    c.save()
                    c.scale((W + 2) / tw, 1f, cx, by)
                    c.drawText(text, cx, by, textP)
                    c.restore()
                } else {
                    c.drawText(text, cx, by, textP)
                }
            }
        }
    }

    private fun alpha(color: Int, a: Float): Int = (color and 0x00ffffff) or ((255 * a).toInt() shl 24)

    private fun rect(c: AndroidCanvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        fillP.color = color
        c.drawRect(x, y, x + w, y + h, fillP)
    }

    private fun drawBlock(c: AndroidCanvas, ch: String, x: Float, y: Float, color: Int): Boolean {
        val cx = x + W / 2
        val cy = y + H / 2
        val dl = cx - 4
        val dr = cx + 2
        val dt = cy - 4
        val db = cy + 2
        when (ch) {
            "█" -> rect(c, x, y, W + 0.5f, H + 0.5f, color)
            "▀" -> rect(c, x, y, W + 0.5f, H / 2, color)
            "▄" -> rect(c, x, y + H / 2, W + 0.5f, H / 2 + 0.5f, color)
            "▓" -> rect(c, x, y, W + 0.5f, H + 0.5f, alpha(color, 0.8f))
            "▒" -> rect(c, x, y, W + 0.5f, H + 0.5f, alpha(color, 0.5f))
            "░" -> rect(c, x, y, W + 0.5f, H + 0.5f, alpha(color, 0.25f))
            "━" -> rect(c, x, cy - 2, W + 0.5f, 4f, color)
            "═" -> { rect(c, x, dt, W + 0.5f, M, color); rect(c, x, db, W + 0.5f, M, color) }
            "║" -> { rect(c, dl, y, M, H + 0.5f, color); rect(c, dr, y, M, H + 0.5f, color) }
            "╔" -> dbl(c, x, y, false, true, false, true, color)
            "╗" -> dbl(c, x, y, true, false, false, true, color)
            "╚" -> dbl(c, x, y, false, true, true, false, color)
            "╝" -> dbl(c, x, y, true, false, true, false, color)
            "╣" -> { // bal + fel + le
                rect(c, dr, y, M, H + 0.5f, color)
                rect(c, dl, y, M, dt + M - y, color); rect(c, dl, db, M, y + H - db + 0.5f, color)
                rect(c, x, dt, dl - x + M, M, color); rect(c, x, db, dl - x + M, M, color)
            }
            "╠" -> { // jobb + fel + le
                rect(c, dl, y, M, H + 0.5f, color)
                rect(c, dr, y, M, dt + M - y, color); rect(c, dr, db, M, y + H - db + 0.5f, color)
                rect(c, dr, dt, x + W - dr + 0.5f, M, color); rect(c, dr, db, x + W - dr + 0.5f, M, color)
            }
            "╦" -> { // bal + jobb + le
                rect(c, x, dt, W + 0.5f, M, color)
                rect(c, x, db, dl + M - x, M, color); rect(c, dr, db, x + W - dr + 0.5f, M, color)
                rect(c, dl, db, M, y + H - db + 0.5f, color); rect(c, dr, db, M, y + H - db + 0.5f, color)
            }
            "╩" -> { // bal + jobb + fel
                rect(c, x, db, W + 0.5f, M, color)
                rect(c, x, dt, dl + M - x, M, color); rect(c, dr, dt, x + W - dr + 0.5f, M, color)
                rect(c, dl, y, M, dt + M - y, color); rect(c, dr, y, M, dt + M - y, color)
            }
            "╞" -> { // egyszeres függőleges, jobbra dupla
                rect(c, cx - 1, y, M, H + 0.5f, color)
                rect(c, cx - 1, dt, x + W - (cx - 1) + 0.5f, M, color); rect(c, cx - 1, db, x + W - (cx - 1) + 0.5f, M, color)
            }
            "╡" -> { // egyszeres függőleges, balra dupla
                rect(c, cx - 1, y, M, H + 0.5f, color)
                rect(c, x, dt, cx + 1 - x, M, color); rect(c, x, db, cx + 1 - x, M, color)
            }
            "╪" -> { // egyszeres függőleges, dupla vízszintes
                rect(c, cx - 1, y, M, H + 0.5f, color)
                rect(c, x, dt, W + 0.5f, M, color); rect(c, x, db, W + 0.5f, M, color)
            }
            "▶" -> poly(c, color, x + 2, cy - 6, x + 2, cy + 6, x + W - 1, cy)
            "◀" -> poly(c, color, x + W - 2, cy - 6, x + W - 2, cy + 6, x + 1, cy)
            "▲" -> poly(c, color, cx, cy - 6, x + 1, cy + 5, x + W - 1, cy + 5)
            "▼" -> poly(c, color, x + 1, cy - 5, x + W - 1, cy - 5, cx, cy + 6)
            "◆" -> poly(c, color, cx, cy - 7, x + W - 1, cy, cx, cy + 7, x + 1, cy)
            "●" -> { shapeP.color = color; c.drawCircle(cx, cy, 5.5f, shapeP) }
            else -> return false
        }
        return true
    }

    private fun poly(c: AndroidCanvas, color: Int, vararg p: Float) {
        path.reset()
        path.moveTo(p[0], p[1])
        var i = 2
        while (i + 1 < p.size) { path.lineTo(p[i], p[i + 1]); i += 2 }
        path.close()
        shapeP.color = color
        c.drawPath(path, shapeP)
    }

    // Dupla vonalú sarok: melyik karok (bal, jobb, fel, le) léteznek.
    private fun dbl(c: AndroidCanvas, x: Float, y: Float, l: Boolean, r: Boolean, u: Boolean, d: Boolean, color: Int) {
        val cx = x + W / 2
        val cy = y + H / 2
        val ox = if (r) cx - 4 else cx + 2
        val oy = if (d) cy - 4 else cy + 2
        val ix = if (r) cx + 2 else cx - 4
        val iy = if (d) cy + 2 else cy - 4
        if (r) { rect(c, ox, oy, x + W - ox + 0.5f, M, color); rect(c, ix, iy, x + W - ix + 0.5f, M, color) }
        if (l) { rect(c, x, oy, ox - x + M, M, color); rect(c, x, iy, ix - x + M, M, color) }
        if (d) { rect(c, ox, oy, M, y + H - oy + 0.5f, color); rect(c, ix, iy, M, y + H - iy + 0.5f, color) }
        if (u) { rect(c, ox, y, M, oy - y + M, color); rect(c, ix, y, M, iy - y + M, color) }
    }
}
