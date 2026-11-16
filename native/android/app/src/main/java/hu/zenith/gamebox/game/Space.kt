package hu.zenith.gamebox.game

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** (címke, szín) párok a lövedékekhez. */
val SPACE_BULLETS: List<Pair<String, String>> = listOf(
    "YELLOW" to "#ffff00", "RED" to "#ff0000", "BLUE" to "#00afff", "GREEN" to "#00ff00",
)
val SPACE_SHIPS: List<String> = listOf("╞═❯✦❮═╡", "«═╦╩╦═»", "▄█▀█▀█▄", "╪═▶█◀═╪")

private val START_X = intArrayOf(4, 12, 20, 28, 36, 44)
private val ENEMY = listOf("👾", "🛸", "👽")
private val BG = hex("#0e011c")
private val BORDER = hex("#af87ff")
private val GLOW = hex("#d787ff")
private val WHITE = hex("#e5e5e5")
private val PURPLE = hex("#af87ff")
private val YELLOW = hex("#ffff00")
private val RED = hex("#ff0000")
private val HUD_C = hex("#ffd7ff")
private val BLUE = hex("#00afff")
private val GREEN = hex("#00ff00")

private class Bullet { var a = false; var x = 0.0; var y = 0.0 }

class Space(o: GameOpts) : TermGame(56, 19) {
    private val best = o.best
    private val bulletC: Int = hex(SPACE_BULLETS.firstOrNull { it.first == o.opts["bullet"] }?.second ?: "#ffff00")
    private val bulletLabel: String = SPACE_BULLETS.firstOrNull { hex(it.second) == bulletC }?.first ?: "YELLOW"
    private val ship: String = o.opts["ship"]?.takeIf { it.isNotEmpty() } ?: SPACE_SHIPS[0]

    private var px = 24.0
    private var target = 24.0
    private val bullets = Array(6) { Bullet() }
    private val ex = DoubleArray(18)
    private val ey = DoubleArray(18)
    private val ea = BooleanArray(18)
    private var et = IntArray(18)
    private var dir = 1
    private var espeed = 6
    private var wave = 1
    private var fireTick = 0
    private var pwActive = false
    private var pwX = 0.0
    private var pwY = 0.0
    private var pwType = 2
    private var carrier = -1
    private var mode = 1
    private var pwExpiry = 0.0
    private var fire = false

    init { resetEnemies() }

    private fun resetEnemies() {
        for (i in 0 until 18) {
            ex[i] = START_X[i % 6].toDouble()
            ey[i] = (2 + 2 * (i / 6)).toDouble()
            ea[i] = true
        }
    }

    private fun shoot(x: Double) {
        val b = bullets.firstOrNull { !it.a } ?: return
        b.a = true; b.x = x; b.y = 17.0
    }

    override fun delay(): Double = 15.0

    override fun tap(col: Int, side: Int) {
        target = max(0.0, min(49.0, target + side * 4))
        fire = true
    }

    override fun drag(dxCols: Double) {
        target = max(0.0, min(49.0, target + dxCols))
    }

    override fun tick() {
        if (!started || over) return
        val t = nowSec()
        px += (target - px) * 0.24
        if (fire || fireTick <= 0) {
            fire = false
            fireTick = 15
            if (mode == 1) shoot(px + 3)
            if (mode == 2) { shoot(px + 2); shoot(px + 4) }
            if (mode == 3) { shoot(px + 1); shoot(px + 3); shoot(px + 5) }
        }
        fireTick--
        // erőforrások: powerup + lövedékek
        if (mode > 1 && t >= pwExpiry) mode = 1
        if (pwActive) {
            pwY += 0.14
            if (pwY >= 17.5 && pwX + 1 >= px && pwX <= px + 6) { pwActive = false; mode = pwType; pwExpiry = t + 10 }
            if (pwY > 19) pwActive = false
        }
        for (b in bullets) {
            if (!b.a) continue
            val previousY = b.y
            b.y -= 0.45
            if (b.y < 2) { b.a = false; continue }
            for (i in 0 until 18) {
                val enemyX = ex[i]
                val enemyY = ey[i]
                if (ea[i] && b.y <= enemyY && previousY >= enemyY && b.x >= enemyX - 0.5 && b.x <= enemyX + 2) {
                    ea[i] = false; b.a = false; score = min(99999.0, score + 10)
                    if (i == carrier) { pwActive = true; pwX = enemyX; pwY = b.y; carrier = -1 }
                    break
                }
            }
        }
        // ellenségek
        var shift = false
        for (i in 0 until 18) {
            val x = ex[i]
            if (ea[i] && ((dir == 1 && x >= 53) || (dir == -1 && x <= 0))) shift = true
        }
        if (shift) {
            dir = -dir
            for (i in 0 until 18) {
                ey[i] = ey[i] + 1
                if (ea[i] && ey[i] >= 17) over = true
            }
        } else {
            for (i in 0 until 18) ex[i] = ex[i] + dir.toDouble() / (espeed + 4)
        }
        if (!ea.any { it }) {
            wave++
            val wt = rnd(3)
            resetEnemies(); et = IntArray(18) { wt }
            dir = 1; pwActive = false
            if (wave == 4) carrier = -1
            else { carrier = rnd(18); pwType = if (mode == 1) 2 else 3 }
            if (espeed > 1) espeed--
        }
    }

    override fun draw(g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        if (!started) {
            g.center(5, "TAP TO START", WHITE, W)
            g.center(7, "DRAG: MOVE (LEFT / RIGHT)", HUD_C, W)
            g.center(9, "TAP: SHOOT  |  CATCH THE DROPPED 🔶 OR ⭐", BLUE, W)
            g.center(12, "[ ARSENAL SETUP ]", YELLOW, W)
            g.inner(5, 13, "BULLET: ", WHITE); g.inner(13, 13, bulletLabel, bulletC)
            g.inner(20, 13, " | SHIP: ", WHITE); g.inner(29, 13, ship, PURPLE)
            return
        }
        if (pwActive) g.inner(pwX, pwY, if (pwType == 2) "🔶" else "⭐", YELLOW)
        val r3 = if (wave % 2 == 1) GREEN else BLUE
        for (i in 0 until 18) {
            if (ea[i]) g.inner(ex[i], ey[i], ENEMY[et[i]], if (i < 6) PURPLE else if (i < 12) YELLOW else r3)
        }
        for (b in bullets) if (b.a) g.inner(b.x, b.y, "|", bulletC)
        g.inner(px, 18, ship, PURPLE)
    }

    override fun hud(): List<Seg> {
        val t = nowSec()
        val s = score.toInt()
        val m = if (mode == 1) Seg("NORMAL 🔫", HUD_C)
        else Seg("${if (mode == 2) "DOUBLE" else "TRIPLE"} ${max(0, ceil(pwExpiry - t).toInt())}s", if (mode == 2) YELLOW else RED)
        return listOf(Seg(" 👾  BEST:${max(best, s)} | SCORE:$s | WAVE:$wave | WEAPON:", HUD_C), m)
    }
}
