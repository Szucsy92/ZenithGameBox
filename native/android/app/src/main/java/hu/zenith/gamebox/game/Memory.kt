package hu.zenith.gamebox.game

import kotlin.math.floor
import kotlin.math.max

private val BG = hex("#14051f")
private val BORDER = hex("#af87ff")
private val GLOW = hex("#d787ff")
private val BACK = hex("#5f2f9f")
private val MATCH = hex("#d787ff")
private val CUR = hex("#ffffff")
private val HUD_C = hex("#ffd7ff")
private val WHITE = hex("#ffffff")
private val BLACK = hex("#000000")
private val SYMBOLS = listOf("▲", "●", "♥", "♦", "♣", "♠", "★", "◆")
private val COLORS = listOf("#00ffff", "#ffff00", "#ff3355", "#55ff55", "#ff5f00", "#5fd7ff", "#ffd7ff", "#af87ff").map { hex(it) }
private const val CARD_W = 12
private const val CARD_H = 4

class Memory(o: GameOpts) : TermGame(4 * (CARD_W + 2) + 2, 4 * (CARD_H + 1) - 1) {
    private val best = o.best
    private val deck: IntArray = (SYMBOLS.indices.toList() + SYMBOLS.indices.toList()).shuffled().toIntArray()
    private val shown = BooleanArray(16)
    private val done = BooleanArray(16)
    private var open = ArrayList<Int>()
    private var wait = 0
    private var cur = 0
    private var moves = 0

    override var score: Double
        get() = max(0, 100 - moves * 2).toDouble()
        set(_) {}

    override val usesTapAt: Boolean = true

    private fun flip(i: Int) {
        if (i < 0 || i > 15) return
        if (wait != 0 || done[i] || shown[i]) return
        shown[i] = true; open.add(i)
        if (open.size == 2) {
            moves++
            val a = open[0]
            val b = open[1]
            if (deck[a] == deck[b]) {
                done[a] = true; done[b] = true; open = ArrayList()
                if (done.all { it }) over = true
            } else wait = 45
        }
    }

    private fun x0(c: Int) = 1 + c * (CARD_W + 2)
    private fun y0(r: Int) = 1 + r * (CARD_H + 1)

    override fun delay(): Double = 16.0

    override fun dir(dx: Int, dy: Int) {
        cur = ((cur / 4 + dy + 4) % 4) * 4 + ((cur % 4) + dx + 4) % 4
    }

    override fun press() { flip(cur) }

    override fun tapAt(col: Int, row: Int) {
        val c = floor((col - 1) / (CARD_W + 2).toDouble()).toInt()
        val r = floor((row - 1) / (CARD_H + 1).toDouble()).toInt()
        if (c in 0..3 && r in 0..3) { cur = r * 4 + c; flip(cur) }
    }

    override fun tick() {
        if (wait != 0) {
            wait--
            if (wait == 0) { for (i in open) shown[i] = false; open = ArrayList() }
        }
    }

    override fun draw(g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        for (i in 0 until 16) {
            val c = i % 4
            val r = i / 4
            val face = shown[i] || done[i]
            val col = if (done[i]) MATCH else if (face) COLORS[deck[i]] else BACK
            for (y in 0 until CARD_H) g.inner(x0(c), y0(r) + y, if (face) "▒".rep(CARD_W) else "▓".rep(CARD_W), col)
            if (face) g.inner(x0(c) + CARD_W / 2 - 1, y0(r) + 1, SYMBOLS[deck[i]] + " ", WHITE, BLACK)
            if (i == cur && started) {
                g.inner(x0(c) - 1, y0(r), "▶", CUR)
                g.inner(x0(c) + CARD_W, y0(r) + CARD_H - 1, "◀", CUR)
            }
        }
        if (!started) g.center(H / 2, " TAP TO START ", WHITE, W)
        if (over) g.center(H / 2, " KÉSZ! $moves LÉPÉS ", WHITE, W)
    }

    override fun hud(): List<Seg> {
        val s = score.toInt()
        return listOf(Seg(" LÉPÉS: $moves   PONT: $s   BEST: ${max(best, s)}", HUD_C))
    }
}
