package hu.zenith.gamebox.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

val PONG_BALLS: List<String> = listOf("🟠", "🔵", "🟢", "🟣", "🟡", "🔴")
val PONG_PADS: List<String> = listOf("#5fd7ff", "#ffff00", "#af87ff")

private val BG = hex("#1a0233")
private val BORDER = hex("#af87ff")
private val GLOW = hex("#d787ff")
private val HUD_C = hex("#ffd7ff")
private val WHITE = hex("#e5e5e5")
private val BLK = mapOf(
    1 to hex("#ff00ff"), 2 to hex("#00afff"), 3 to hex("#ffff00"), 4 to hex("#00ff00"), 5 to hex("#ff0000"),
)

class Pong(o: GameOpts) : TermGame(58, 19) {
    private val best = o.best
    private val speedStart = 0.30
    private val speedAccel = 0.025
    private val levelGap = 10
    private val maxLevel = 10
    private val wallLife = 9.0
    private val basePw = 20
    private val paddleY = 19
    private val jumpX = 6.0
    private val ball: String = o.opts["ball"]?.takeIf { it.isNotEmpty() } ?: PONG_BALLS[0]
    private val pad: Int = hex(o.opts["pad"]?.takeIf { it.isNotEmpty() } ?: PONG_PADS[0])

    private var pw = basePw
    private var px = floor(58 / 2.0 - basePw / 2.0)
    private var target = px
    private var speedOffset = 0
    private var yellowEnd = 0.0
    private var greenEnd = 0.0
    private var redEnd = 0.0
    private var bx = floor(58 / 2.0)
    private var by = floor(19 / 2.0)
    private var dx = if (rnd(2) != 0) 1.0 else -1.0
    private var dy = -1.0
    private var wallX = 0
    private var wallY = -1
    private var wallType = 1
    private var wallDie = 0.0
    private var level = 1
    private var impulse = 0.0

    private fun clampPX() {
        target = max(1.0, min((W - pw + 1).toDouble(), target))
        px = max(1.0, min((W - pw + 1).toDouble(), px))
    }

    override fun delay(): Double = 16.0

    override fun tap(col: Int, side: Int) {
        target += side * jumpX
        impulse = side.toDouble()
        clampPX()
    }

    override fun drag(dxCols: Double) {
        target += dxCols
        if (dxCols != 0.0) impulse = if (dxCols < 0) -1.0 else 1.0
        clampPX()
    }

    override fun tick() {
        if (!started || over) return
        val t = nowSec()
        level = max(1, min(maxLevel, floor(score / levelGap).toInt() + 1 + speedOffset))
        val inc = speedStart + (level - 1) * speedAccel
        var mod = 0
        if (t < greenEnd) mod += 4
        if (t < redEnd) mod -= 5
        pw = max(4, basePw + mod)
        clampPX()
        px += (target - px) * 0.28
        if (wallY != -1 && t >= wallDie) wallY = -1

        val previousY = by
        bx += dx * inc
        by += dy * inc
        if (wallY == -1 && rnd(300) == 0) {
            wallY = rnd(8) + 3
            wallX = rnd(W - 6) + 2
            wallDie = t + wallLife
            wallType = if (level >= 2) rnd(5) + 1 else listOf(1, 3, 4, 5)[rnd(4)]
        }
        if (bx <= 1) { dx = -dx; bx = 1.0 }
        if (bx >= W - 1) { dx = -dx; bx = (W - 1).toDouble() }
        if (by <= 1) { dy = 1.0; by = 1.0 }
        if (wallY != -1 && min(previousY, by) <= wallY && max(previousY, by) >= wallY && bx + 1 >= wallX && bx <= wallX + 3) {
            dy = -dy
            wallY = -1
            if (wallType == 1) score += 2.0
            if (wallType == 2) speedOffset -= 2
            if (wallType == 3) yellowEnd = t + 7
            if (wallType == 4) greenEnd = t + 10
            if (wallType == 5) redEnd = t + 5
        }
        if (dy > 0 && previousY < paddleY - 1 && by >= paddleY - 1) {
            if (bx + 1 >= px && bx < px + pw) {
                val offset = (bx + 0.5 - (px + pw / 2.0)) / (pw / 2.0)
                dx = max(-1.35, min(1.35, offset * 1.15 + impulse * 0.12))
                if (abs(dx) < 0.25) dx = if (dx < 0) -0.25 else 0.25
                impulse = 0.0
                by = (paddleY - 1).toDouble(); dy = -1.0; score += 1.0
            } else if (t < yellowEnd) {
                by = (paddleY - 1).toDouble(); dy = -1.0
            }
        }
        if (by >= H) { by = H.toDouble(); over = true }
    }

    override fun draw(g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        val t = nowSec()
        if (started && t < yellowEnd) g.inner(0, paddleY, "▒".rep(W), BLK[3] ?: WHITE)
        if (wallY != -1) g.inner(wallX - 1, wallY, "▓▓▓▓", BLK[wallType] ?: WHITE)
        g.inner(px - 1, paddleY, "▓".rep(pw), pad)
        g.inner(bx - 1, by, ball, WHITE)
        if (!started) {
            g.center(H / 2, "TAP TO START", WHITE, W)
            g.center(H / 2 + 2, "DRAG / TAP: MOVE (LEFT SIDE / RIGHT SIDE)", HUD_C, W)
        }
    }

    override fun hud(): List<Seg> {
        val t = nowSec()
        val bar = (0 until maxLevel).joinToString("") { if (it < level) "█" else "░" }
        val s = score.toInt()
        val out = mutableListOf(Seg(" BEST:${max(best, s)} | SCORE:$s | LVL:$level/$maxLevel [$bar]", HUD_C))
        if (t < yellowEnd) out.add(Seg("  SHIELD:${ceil(yellowEnd - t).toInt()}s", BLK[3] ?: WHITE))
        if (t < greenEnd) out.add(Seg("  WIDE:${ceil(greenEnd - t).toInt()}s", BLK[4] ?: WHITE))
        if (t < redEnd) out.add(Seg("  NARROW:${ceil(redEnd - t).toInt()}s", BLK[5] ?: WHITE))
        return out
    }
}
