package hu.zenith.gamebox.game

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

val JUMP_SKINS: List<String> = listOf("#00ff00", "#ff5f00", "#ffff00", "#af5fff")

private val BG = hex("#262626")
private val BORDER = hex("#af87ff")
private val GLOW = hex("#d787ff")
private val HUD_C = hex("#ffd7ff")
private val WHITE = hex("#e5e5e5")
private val CYAN = hex("#00afff")
private val YELLOW = hex("#ffff00")
private val GREEN = hex("#00ff00")
private val PURPLE = hex("#af5fff")

private class Plat(var x: Double, var y: Double, var a: Boolean, var d: Int, var max: Int, var b: Int)

class Jump(o: GameOpts) : TermGame(58, 19) {
    private val best = o.best
    private val jp = -0.65
    private val maxFall = 0.65
    private val yellowProb = 6
    private val purpleProb = 15
    private val player: Int = hex(JUMP_SKINS.getOrNull((o.opts["skin"]?.toIntOrNull() ?: 1) - 1) ?: "#00ff00")

    private var px = floor(58 / 2.0)
    private var py = (19 - 3).toDouble()
    private var vy = jp
    private var target = px
    private var groundY = (19 - 1).toDouble()
    private var ground = true
    private val pl = listOf(
        Plat(6.0, 14.0, true, 1, -1, -1),
        Plat(22.0, 10.0, true, -1, -1, -1),
        Plat(38.0, 6.0, true, 1, -1, -1),
        Plat(18.0, 2.0, true, 0, -1, -1),
    )

    override fun delay(): Double = 16.0

    override fun tap(col: Int, side: Int) {
        target = max(3.0, min((W - 3).toDouble(), col.toDouble()))
    }

    override fun drag(dxCols: Double) {
        target = max(3.0, min((W - 3).toDouble(), target + dxCols))
    }

    override fun tick() {
        if (!started || over) return
        px += (target - px) * 0.16
        vy = min(maxFall, vy + 0.026)
        py += vy
        if (vy > 0) {
            if (ground && py >= groundY) {
                py = groundY; vy = jp
            } else {
                for (p in pl) {
                    if (p.a && py >= p.y && py - vy <= p.y && px + 1 >= p.x && px - 3 <= p.x + 9) {
                        py = p.y
                        if (p.max == -2) vy = jp * 3
                        else {
                            vy = jp
                            if (p.max != -1) { p.b--; if (p.b <= 0) p.a = false }
                        }
                        break
                    }
                }
            }
        }
        for (p in pl) {
            if (p.a && p.d != 0) {
                p.x += p.d * 0.09
                if (p.x < 2) { p.x = 2.0; p.d = 1 }
                if (p.x > W - 11) { p.x = (W - 11).toDouble(); p.d = -1 }
            }
        }
        if (py < 7) {
            val shift = 7 - py
            py = 7.0; score += shift
            if (ground) { groundY += shift; if (groundY > H) ground = false }
            for (p in pl) {
                p.y += shift
                if (p.y > H - 1) {
                    p.y -= 16; p.x = (rnd(W - 12) + 2).toDouble(); p.a = true; p.d = rnd(3) - 1
                    if (rnd(purpleProb) == 0) { p.max = -2; p.b = -1 }
                    else if (rnd(yellowProb) == 0) { p.max = 2; p.b = 2 }
                    else { p.max = -1; p.b = -1 }
                }
            }
        }
        if (py > H) over = true
    }

    override fun draw(g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        if (!started) {
            g.center(H / 2 - 2, "TAP TO JUMP START", WHITE, W)
            g.center(H / 2, "CONTROLS: TAP / SWIPE LEFT | RIGHT", HUD_C, W)
            g.inner(px - 3, H - 3, "▓▀█▀▓", player)
            g.inner(px - 3, H - 2, "▓███▓", player)
            return
        }
        if (ground) g.inner(0, groundY, "█".rep(W), GREEN)
        for (p in pl) {
            if (p.a) g.inner(p.x - 1, p.y, "█".rep(10), if (p.max == -2) PURPLE else if (p.max != -1) YELLOW else CYAN)
        }
        g.inner(px - 3, py - 1, "▓▀█▀▓", player)
        g.inner(px - 3, py, "▓███▓", player)
    }

    override fun hud(): List<Seg> {
        val s = floor(score).toInt()
        return listOf(Seg(" BEST:${max(best, s)} | SCORE:${s}m | ZONE: ${s}m", HUD_C))
    }
}
