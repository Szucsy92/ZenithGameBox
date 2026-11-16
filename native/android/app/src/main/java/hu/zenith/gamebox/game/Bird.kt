package hu.zenith.gamebox.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val YELLOW = hex("#ffff00")
private val RED = hex("#ff0000")
private val WHITE = hex("#ffffff")
private val ORANGE = hex("#ff5f00")
private val LIGHT_GREEN = hex("#55ff55")
private val CYAN = hex("#00ffff")
private val BG1 = hex("#5f00af")
private val BG2 = hex("#00005f")
private val BG3 = hex("#5f0000")

class Bird(o: GameOpts) : TermGame(58, 19) {
    private val best = o.best
    private val gap = 6
    private val birdX = 10
    private val flapV = -0.5
    private var birdY = 9.0
    private var vel = 0.0
    private var pipeX = 58.0
    private var gapY = 4
    private var flap = false

    override fun delay(): Double = 16.0
    override fun start() { started = true; vel = flapV }
    override fun tap(col: Int, side: Int) { flap = true }
    override fun drag(dxCols: Double) { if (abs(dxCols) >= 0.5) flap = true }

    override fun tick() {
        if (!started || over) return
        if (flap) { vel = flapV; flap = false }
        vel = min(0.48, vel + 0.026)
        birdY += vel
        pipeX -= min(0.7, 0.28 + score * 0.014)
        if (pipeX < 1) { pipeX = W.toDouble(); gapY = rnd(H - gap - 2) + 2; score += 1.0 }
        if (birdY < 1) { birdY = 1.0; over = true }
        if (birdY > H) { birdY = H.toDouble(); over = true }
        if (!over && birdX + 2 >= pipeX && birdX <= pipeX + 3 && (birdY < gapY || birdY >= gapY + gap)) over = true
    }

    override fun draw(g: Grid) {
        val s = score
        val bg = if (s < 20) BG1 else if (s < 40) BG2 else BG3
        val wall = if (s < 20) LIGHT_GREEN else if (s < 40) CYAN else RED
        g.frame(W, H, ORANGE, ORANGE, bg)
        if (!started) {
            g.center(birdY, "TAP TO START", WHITE, W)
            g.inner(birdX - 1, birdY, "╣█╠", YELLOW)
            return
        }
        for (y in 1..H) {
            if (pipeX >= 1 && pipeX <= W && (y < gapY || y >= gapY + gap)) g.inner(pipeX - 1, y, "▓▒▒▓", wall)
        }
        val b = birdX - 1
        if (vel < 0) {
            g.inner(b, birdY, "╣", YELLOW)
            g.inner(b + 1, birdY, "▼", RED)
            g.inner(b + 2, birdY, "╠", YELLOW)
        } else {
            g.inner(b, birdY, if (vel > 0) "╣█╠" else "╣▓╠", YELLOW)
        }
    }

    override fun hud(): List<Seg> = listOf(
        Seg(" SCORE: ", WHITE),
        Seg(score.toInt().toString(), CYAN),
        Seg("   BEST: ", WHITE),
        Seg(max(best, score.toInt()).toString(), YELLOW),
    )
}
