package hu.zenith.gamebox.game

import kotlin.math.floor
import kotlin.math.max

private val BG = hex("#06140a")
private val GRID = hex("#0f2a16")
private val BORDER = hex("#55ff55")
private val HEAD = hex("#aaffaa")
private val BODY = hex("#33dd55")
private val FOOD = hex("#ff3355")
private val HUD_C = hex("#ccffcc")
private val WHITE = hex("#ffffff")

private class Pt(val x: Int, val y: Int)

class Snake(o: GameOpts) : TermGame(29 * 2, 19) {
    private val best = o.best
    private val vibrate = o.vibrate
    private val gw = 29
    private val gh = 19
    private val body = ArrayList<Pt>().apply { add(Pt(14, 10)); add(Pt(13, 10)); add(Pt(12, 10)) }
    private var dx = 1
    private var dy = 0
    private var nextDx = 1
    private var nextDy = 0
    private var food = Pt(20, 10)

    private fun place() {
        do {
            food = Pt(rnd(gw) + 1, rnd(gh) + 1)
        } while (body.any { it.x == food.x && it.y == food.y })
    }

    private fun turn(nx: Int, ny: Int) {
        if (nx != -dx || ny != -dy) { nextDx = nx; nextDy = ny }
    }

    override fun delay(): Double = max(70.0, 160.0 - score * 4)

    override fun dir(dx: Int, dy: Int) { turn(dx, dy) }

    override fun tick() {
        if (!started || over) return
        dx = nextDx; dy = nextDy
        val head = body[0]
        val h = Pt(head.x + dx, head.y + dy)
        if (h.x < 1 || h.x > gw || h.y < 1 || h.y > gh || body.any { it.x == h.x && it.y == h.y }) {
            over = true
            vibrate(120L)
            return
        }
        body.add(0, h)
        if (h.x == food.x && h.y == food.y) { score += 1.0; place() } else body.removeAt(body.size - 1)
    }

    override fun draw(g: Grid) {
        g.frame(gw * 2, gh, BORDER, BORDER, BG)
        for (y in 1..gh) for (x in 1..gw) if ((x + y) % 2 == 0) g.inner((x - 1) * 2, y, "░░", GRID)
        val blink = floor(System.nanoTime() / 1_000_000.0 / 300.0).toLong() % 2L == 0L
        g.inner((food.x - 1) * 2, food.y, if (blink) "██" else "▓▓", FOOD)
        body.forEachIndexed { i, p -> g.inner((p.x - 1) * 2, p.y, "██", if (i == 0) HEAD else BODY) }
        if (!started) {
            g.center(5, "TAP TO START", WHITE, gw * 2)
            g.center(7, "JOYSTICK: IRÁNY", HUD_C, gw * 2)
        }
    }

    override fun hud(): List<Seg> =
        listOf(Seg(" SCORE: ${score.toInt()}   BEST: ${max(best, score.toInt())}", HUD_C))
}
