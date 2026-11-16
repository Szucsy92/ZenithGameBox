package hu.zenith.gamebox.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val WHITE = hex("#ffffff")
private val BLACK = hex("#000000")
private val BG = hex("#eeeeee")
private val BORDER = hex("#585858")
private val CLOUD = hex("#bcbcbc")
private val GROUND_C = hex("#808080")
private val CACTUS = hex("#00aa00")
private val DINO_A = hex("#af87ff")
private val DINO_B = hex("#af5fff")

class Dino(o: GameOpts) : TermGame(58, 19) {
    private val best = o.best
    private val ground = 19 - 2
    private val dinoX = 8
    private val jumpV = -0.65
    private val maxFall = 0.6
    private val clouds = listOf(intArrayOf(4, 1), intArrayOf(27, 5), intArrayOf(50, 3))
    private var dinoY = ground.toDouble()
    private var vel = 0.0
    private var cactusX = 58.0
    private var cactusH = 3
    private var speed = 0.4
    private var jump = false

    override fun delay(): Double = 16.0
    override fun start() { started = true; vel = jumpV }
    override fun tap(col: Int, side: Int) { jump = true }
    override fun drag(dxCols: Double) { if (abs(dxCols) >= 0.5) jump = true }

    override fun tick() {
        if (!started || over) return
        if (jump) { if (dinoY >= ground) vel = jumpV; jump = false }
        vel = min(maxFall, vel + 0.026)
        dinoY = min(ground.toDouble(), dinoY + vel)
        cactusX -= speed
        if (cactusX < -3) {
            cactusX = W.toDouble(); cactusH = rnd(2) + 2; score += 1.0
            if (score.toInt() % 5 == 0) speed = min(0.9, speed + 0.075)
        }
        if (dinoX + 4 >= cactusX && dinoX <= cactusX + 2 && dinoY >= ground - cactusH + 1) over = true
    }

    override fun draw(g: Grid) {
        g.frame(W, H, BORDER, BORDER, BG)
        if (!started) { g.center(H / 2, " TAP TO START ", BLACK, W); return }
        for (c in clouds) {
            g.inner(c[0] - 1, c[1], " ▄▄▄ ", CLOUD)
            g.inner(c[0] - 1, c[1] + 1, "█████", CLOUD)
        }
        g.inner(0, ground + 1, "━".rep(W), GROUND_C)
        if (cactusX >= 1 && cactusX <= W - 3) {
            for (y in (ground - cactusH + 1)..ground) g.inner(cactusX - 1, y, "█▄█", CACTUS)
        }
        g.inner(dinoX - 1, dinoY - 1, "▓▀█▀▓", DINO_A)
        g.inner(dinoX - 1, dinoY, "▓███▓", DINO_B)
    }

    override fun hud(): List<Seg> {
        val r = Math.round(speed / 0.4 * 10) / 10.0
        val rs = if (r == Math.floor(r)) r.toInt().toString() else r.toString()
        return listOf(Seg(" HI: ${max(best, score.toInt())} | SCORE: ${score.toInt()} | SPEED: $rs ", WHITE))
    }
}
