import SwiftUI

// MARK: - Bird

final class Bird: TermGame {
    private let best: Int
    private let YELLOW = hex("#ffff00"), RED = hex("#ff0000"), WHITE = hex("#ffffff"), ORANGE = hex("#ff5f00")
    private let LGREEN = hex("#55ff55"), CYAN = hex("#00ffff")
    private let BG1 = hex("#5f00af"), BG2 = hex("#00005f"), BG3 = hex("#5f0000")
    private let gap = 6
    private let birdX = 10
    private let flapV = -0.5
    private var birdY = 9.0
    private var vel = 0.0
    private var pipeX = 58.0
    private var gapY = 4
    private var flap = false

    init(_ o: GameOpts) { best = o.best; super.init(58, 19) }

    override func start() { started = true; vel = flapV }
    override func tap(_ col: Int, _ side: Int) { flap = true }
    override func drag(_ dx: Double) { if abs(dx) >= 0.5 { flap = true } }

    override func tick() {
        if !started || over { return }
        if flap { vel = flapV; flap = false }
        vel = min(0.48, vel + 0.026)
        birdY += vel
        pipeX -= min(0.7, 0.28 + score * 0.014)
        if pipeX < 1 { pipeX = Double(W); gapY = rnd(H - gap - 2) + 2; score += 1 }
        if birdY < 1 { birdY = 1; over = true }
        if birdY > Double(H) { birdY = Double(H); over = true }
        if !over && Double(birdX + 2) >= pipeX && Double(birdX) <= pipeX + 3 && (birdY < Double(gapY) || birdY >= Double(gapY + gap)) { over = true }
    }

    override func draw(_ g: Grid) {
        let s = score
        let bg = s < 20 ? BG1 : (s < 40 ? BG2 : BG3)
        let wall = s < 20 ? LGREEN : (s < 40 ? CYAN : RED)
        g.frame(W, H, ORANGE, ORANGE, bg)
        if !started {
            g.center(birdY, "TAP TO START", WHITE, W)
            g.inner(birdX - 1, birdY, "╣█╠", YELLOW)
            return
        }
        for y in 1...H {
            if pipeX >= 1 && pipeX <= Double(W) && (Double(y) < Double(gapY) || y >= gapY + gap) {
                g.inner(pipeX - 1, y, "▓▒▒▓", wall)
            }
        }
        let b = birdX - 1
        if vel < 0 {
            g.inner(b, birdY, "╣", YELLOW)
            g.inner(b + 1, birdY, "▼", RED)
            g.inner(b + 2, birdY, "╠", YELLOW)
        } else {
            g.inner(b, birdY, vel > 0 ? "╣█╠" : "╣▓╠", YELLOW)
        }
    }

    override func hud() -> [Seg] {
        [Seg(t: " SCORE: ", c: WHITE), Seg(t: String(Int(score)), c: CYAN), Seg(t: "   BEST: ", c: WHITE), Seg(t: String(max(best, Int(score))), c: YELLOW)]
    }
}

// MARK: - Dino

final class Dino: TermGame {
    private let best: Int
    private let WHITE = hex("#ffffff"), BLACK = hex("#000000"), BG = hex("#eeeeee"), BORDER = hex("#585858")
    private let CLOUD = hex("#bcbcbc"), GROUNDC = hex("#808080"), CACTUS = hex("#00aa00")
    private let DINOA = hex("#af87ff"), DINOB = hex("#af5fff")
    private let ground = 17
    private let dinoX = 8
    private let jumpV = -0.65
    private let maxFall = 0.6
    private let clouds: [(Int, Int)] = [(4, 1), (27, 5), (50, 3)]
    private var dinoY = 17.0
    private var vel = 0.0
    private var cactusX = 58.0
    private var cactusH = 3
    private var speed = 0.4
    private var jump = false

    init(_ o: GameOpts) { best = o.best; super.init(58, 19) }

    override func start() { started = true; vel = jumpV }
    override func tap(_ col: Int, _ side: Int) { jump = true }
    override func drag(_ dx: Double) { if abs(dx) >= 0.5 { jump = true } }

    override func tick() {
        if !started || over { return }
        if jump { if dinoY >= Double(ground) { vel = jumpV }; jump = false }
        vel = min(maxFall, vel + 0.026)
        dinoY = min(Double(ground), dinoY + vel)
        cactusX -= speed
        if cactusX < -3 {
            cactusX = Double(W); cactusH = rnd(2) + 2; score += 1
            if Int(score) % 5 == 0 { speed = min(0.9, speed + 0.075) }
        }
        if Double(dinoX + 4) >= cactusX && Double(dinoX) <= cactusX + 2 && dinoY >= Double(ground - cactusH + 1) { over = true }
    }

    override func draw(_ g: Grid) {
        g.frame(W, H, BORDER, BORDER, BG)
        if !started { g.center(H / 2, " TAP TO START ", BLACK, W); return }
        for c in clouds {
            g.inner(c.0 - 1, c.1, " ▄▄▄ ", CLOUD)
            g.inner(c.0 - 1, c.1 + 1, "█████", CLOUD)
        }
        g.inner(0, ground + 1, rep("━", W), GROUNDC)
        if cactusX >= 1 && cactusX <= Double(W - 3) {
            for y in (ground - cactusH + 1)...ground { g.inner(cactusX - 1, y, "█▄█", CACTUS) }
        }
        g.inner(dinoX - 1, dinoY - 1, "▓▀█▀▓", DINOA)
        g.inner(dinoX - 1, dinoY, "▓███▓", DINOB)
    }

    override func hud() -> [Seg] {
        let r = (speed / 0.4 * 10).rounded() / 10
        let rs = r == r.rounded(.down) ? String(Int(r)) : String(r)
        return [Seg(t: " HI: \(max(best, Int(score))) | SCORE: \(Int(score)) | SPEED: \(rs) ", c: WHITE)]
    }
}

// MARK: - Pong

let PONG_BALLS = ["🟠", "🔵", "🟢", "🟣", "🟡", "🔴"]
let PONG_PADS = ["#5fd7ff", "#ffff00", "#af87ff"]

final class Pong: TermGame {
    private let best: Int
    private let BG = hex("#1a0233"), BORDER = hex("#af87ff"), GLOW = hex("#d787ff"), HUDC = hex("#ffd7ff"), WHITE = hex("#e5e5e5")
    private let BLK: [Int: Color] = [1: hex("#ff00ff"), 2: hex("#00afff"), 3: hex("#ffff00"), 4: hex("#00ff00"), 5: hex("#ff0000")]
    private let speedStart = 0.30, speedAccel = 0.025
    private let levelGap = 10, maxLevel = 10
    private let wallLife = 9.0
    private let basePw = 20
    private let paddleY = 19
    private let jumpX = 6.0
    private let ball: String
    private let pad: Color

    private var pw = 20
    private var px = 19.0
    private var target = 19.0
    private var speedOffset = 0
    private var yellowEnd = 0.0, greenEnd = 0.0, redEnd = 0.0
    private var bx = 29.0, by = 9.0
    private var dx = 1.0, dy = -1.0
    private var wallX = 0, wallY = -1, wallType = 1
    private var wallDie = 0.0
    private var level = 1
    private var impulse = 0.0

    init(_ o: GameOpts) {
        best = o.best
        let b = o.opts["ball"] ?? ""
        ball = b.isEmpty ? PONG_BALLS[0] : b
        let p = o.opts["pad"] ?? ""
        pad = hex(p.isEmpty ? PONG_PADS[0] : p)
        super.init(58, 19)
        dx = rnd(2) != 0 ? 1 : -1
    }

    private func clampPX() {
        let hi = Double(W - pw + 1)
        target = max(1, min(hi, target))
        px = max(1, min(hi, px))
    }

    override func tap(_ col: Int, _ side: Int) {
        target += Double(side) * jumpX
        impulse = Double(side)
        clampPX()
    }

    override func drag(_ d: Double) {
        target += d
        if d != 0 { impulse = d < 0 ? -1 : 1 }
        clampPX()
    }

    override func tick() {
        if !started || over { return }
        let t = nowSec()
        level = max(1, min(maxLevel, Int((score / Double(levelGap)).rounded(.down)) + 1 + speedOffset))
        let inc = speedStart + Double(level - 1) * speedAccel
        var mod = 0
        if t < greenEnd { mod += 4 }
        if t < redEnd { mod -= 5 }
        pw = max(4, basePw + mod)
        clampPX()
        px += (target - px) * 0.28
        if wallY != -1 && t >= wallDie { wallY = -1 }

        let previousY = by
        bx += dx * inc
        by += dy * inc
        if wallY == -1 && rnd(300) == 0 {
            wallY = rnd(8) + 3
            wallX = rnd(W - 6) + 2
            wallDie = t + wallLife
            wallType = level >= 2 ? rnd(5) + 1 : [1, 3, 4, 5][rnd(4)]
        }
        if bx <= 1 { dx = -dx; bx = 1 }
        if bx >= Double(W - 1) { dx = -dx; bx = Double(W - 1) }
        if by <= 1 { dy = 1; by = 1 }
        if wallY != -1 && min(previousY, by) <= Double(wallY) && max(previousY, by) >= Double(wallY) && bx + 1 >= Double(wallX) && bx <= Double(wallX + 3) {
            dy = -dy
            wallY = -1
            if wallType == 1 { score += 2 }
            if wallType == 2 { speedOffset -= 2 }
            if wallType == 3 { yellowEnd = t + 7 }
            if wallType == 4 { greenEnd = t + 10 }
            if wallType == 5 { redEnd = t + 5 }
        }
        if dy > 0 && previousY < Double(paddleY - 1) && by >= Double(paddleY - 1) {
            if bx + 1 >= px && bx < px + Double(pw) {
                let offset = (bx + 0.5 - (px + Double(pw) / 2)) / (Double(pw) / 2)
                dx = max(-1.35, min(1.35, offset * 1.15 + impulse * 0.12))
                if abs(dx) < 0.25 { dx = dx < 0 ? -0.25 : 0.25 }
                impulse = 0
                by = Double(paddleY - 1); dy = -1; score += 1
            } else if t < yellowEnd {
                by = Double(paddleY - 1); dy = -1
            }
        }
        if by >= Double(H) { by = Double(H); over = true }
    }

    override func draw(_ g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        let t = nowSec()
        if started && t < yellowEnd { g.inner(0, paddleY, rep("▒", W), BLK[3] ?? WHITE) }
        if wallY != -1 { g.inner(wallX - 1, wallY, "▓▓▓▓", BLK[wallType] ?? WHITE) }
        g.inner(px - 1, paddleY, rep("▓", pw), pad)
        g.inner(bx - 1, by, ball, WHITE)
        if !started {
            g.center(H / 2, "TAP TO START", WHITE, W)
            g.center(H / 2 + 2, "DRAG / TAP: MOVE (LEFT SIDE / RIGHT SIDE)", HUDC, W)
        }
    }

    override func hud() -> [Seg] {
        let t = nowSec()
        let bar = (0..<maxLevel).map { $0 < level ? "█" : "░" }.joined()
        let s = Int(score)
        var out = [Seg(t: " BEST:\(max(best, s)) | SCORE:\(s) | LVL:\(level)/\(maxLevel) [\(bar)]", c: HUDC)]
        if t < yellowEnd { out.append(Seg(t: "  SHIELD:\(Int((yellowEnd - t).rounded(.up)))s", c: BLK[3] ?? WHITE)) }
        if t < greenEnd { out.append(Seg(t: "  WIDE:\(Int((greenEnd - t).rounded(.up)))s", c: BLK[4] ?? WHITE)) }
        if t < redEnd { out.append(Seg(t: "  NARROW:\(Int((redEnd - t).rounded(.up)))s", c: BLK[5] ?? WHITE)) }
        return out
    }
}

// MARK: - Space

let SPACE_BULLETS: [(String, String)] = [("YELLOW", "#ffff00"), ("RED", "#ff0000"), ("BLUE", "#00afff"), ("GREEN", "#00ff00")]
let SPACE_SHIPS = ["╞═❯✦❮═╡", "«═╦╩╦═»", "▄█▀█▀█▄", "╪═▶█◀═╪"]

final class Space: TermGame {
    private struct Bullet { var a = false; var x = 0.0; var y = 0.0 }
    private let best: Int
    private let START_X: [Double] = [4, 12, 20, 28, 36, 44]
    private let ENEMY = ["👾", "🛸", "👽"]
    private let BG = hex("#0e011c"), BORDER = hex("#af87ff"), GLOW = hex("#d787ff"), WHITE = hex("#e5e5e5"), PURPLE = hex("#af87ff")
    private let YELLOW = hex("#ffff00"), RED = hex("#ff0000"), HUDC = hex("#ffd7ff"), BLUE = hex("#00afff"), GREEN = hex("#00ff00")
    private let bulletC: Color
    private let bulletLabel: String
    private let ship: String

    private var px = 24.0, target = 24.0
    private var bullets = [Bullet](repeating: Bullet(), count: 6)
    private var ex = [Double](repeating: 0, count: 18)
    private var ey = [Double](repeating: 0, count: 18)
    private var ea = [Bool](repeating: true, count: 18)
    private var et = [Int](repeating: 0, count: 18)
    private var dirv = 1
    private var espeed = 6
    private var wave = 1
    private var fireTick = 0
    private var pwActive = false
    private var pwX = 0.0, pwY = 0.0
    private var pwType = 2
    private var carrier = -1
    private var mode = 1
    private var pwExpiry = 0.0
    private var fire = false

    init(_ o: GameOpts) {
        best = o.best
        let found = SPACE_BULLETS.first { $0.0 == o.opts["bullet"] }
        bulletC = hex(found?.1 ?? "#ffff00")
        bulletLabel = found?.0 ?? "YELLOW"
        let s = o.opts["ship"] ?? ""
        ship = s.isEmpty ? SPACE_SHIPS[0] : s
        super.init(56, 19)
        resetEnemies()
    }

    private func resetEnemies() {
        for i in 0..<18 {
            ex[i] = START_X[i % 6]
            ey[i] = Double(2 + 2 * (i / 6))
            ea[i] = true
        }
    }

    private func shoot(_ x: Double) {
        if let i = bullets.firstIndex(where: { !$0.a }) {
            bullets[i].a = true; bullets[i].x = x; bullets[i].y = 17
        }
    }

    override func delay() -> Double { 15 }
    override func tap(_ col: Int, _ side: Int) { target = max(0, min(49, target + Double(side * 4))); fire = true }
    override func drag(_ dx: Double) { target = max(0, min(49, target + dx)) }

    override func tick() {
        if !started || over { return }
        let t = nowSec()
        px += (target - px) * 0.24
        if fire || fireTick <= 0 {
            fire = false
            fireTick = 15
            if mode == 1 { shoot(px + 3) }
            if mode == 2 { shoot(px + 2); shoot(px + 4) }
            if mode == 3 { shoot(px + 1); shoot(px + 3); shoot(px + 5) }
        }
        fireTick -= 1
        if mode > 1 && t >= pwExpiry { mode = 1 }
        if pwActive {
            pwY += 0.14
            if pwY >= 17.5 && pwX + 1 >= px && pwX <= px + 6 { pwActive = false; mode = pwType; pwExpiry = t + 10 }
            if pwY > 19 { pwActive = false }
        }
        for bi in 0..<bullets.count {
            if !bullets[bi].a { continue }
            let previousY = bullets[bi].y
            bullets[bi].y -= 0.45
            if bullets[bi].y < 2 { bullets[bi].a = false; continue }
            for i in 0..<18 {
                let enemyX = ex[i], enemyY = ey[i]
                if ea[i] && bullets[bi].y <= enemyY && previousY >= enemyY && bullets[bi].x >= enemyX - 0.5 && bullets[bi].x <= enemyX + 2 {
                    ea[i] = false; bullets[bi].a = false; score = min(99999, score + 10)
                    if i == carrier { pwActive = true; pwX = enemyX; pwY = bullets[bi].y; carrier = -1 }
                    break
                }
            }
        }
        var shift = false
        for i in 0..<18 {
            let x = ex[i]
            if ea[i] && ((dirv == 1 && x >= 53) || (dirv == -1 && x <= 0)) { shift = true }
        }
        if shift {
            dirv = -dirv
            for i in 0..<18 {
                ey[i] += 1
                if ea[i] && ey[i] >= 17 { over = true }
            }
        } else {
            for i in 0..<18 { ex[i] += Double(dirv) / Double(espeed + 4) }
        }
        if !ea.contains(true) {
            wave += 1
            let wt = rnd(3)
            resetEnemies(); et = [Int](repeating: wt, count: 18)
            dirv = 1; pwActive = false
            if wave == 4 { carrier = -1 } else { carrier = rnd(18); pwType = mode == 1 ? 2 : 3 }
            if espeed > 1 { espeed -= 1 }
        }
    }

    override func draw(_ g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        if !started {
            g.center(5, "TAP TO START", WHITE, W)
            g.center(7, "DRAG: MOVE (LEFT / RIGHT)", HUDC, W)
            g.center(9, "TAP: SHOOT  |  CATCH THE DROPPED 🔶 OR ⭐", BLUE, W)
            g.center(12, "[ ARSENAL SETUP ]", YELLOW, W)
            g.inner(5, 13, "BULLET: ", WHITE); g.inner(13, 13, bulletLabel, bulletC)
            g.inner(20, 13, " | SHIP: ", WHITE); g.inner(29, 13, ship, PURPLE)
            return
        }
        if pwActive { g.inner(pwX, pwY, pwType == 2 ? "🔶" : "⭐", YELLOW) }
        let r3 = wave % 2 == 1 ? GREEN : BLUE
        for i in 0..<18 where ea[i] {
            g.inner(ex[i], ey[i], ENEMY[et[i]], i < 6 ? PURPLE : (i < 12 ? YELLOW : r3))
        }
        for b in bullets where b.a { g.inner(b.x, b.y, "|", bulletC) }
        g.inner(px, 18, ship, PURPLE)
    }

    override func hud() -> [Seg] {
        let t = nowSec()
        let s = Int(score)
        let m: Seg
        if mode == 1 {
            m = Seg(t: "NORMAL 🔫", c: HUDC)
        } else {
            m = Seg(t: "\(mode == 2 ? "DOUBLE" : "TRIPLE") \(max(0, Int((pwExpiry - t).rounded(.up))))s", c: mode == 2 ? YELLOW : RED)
        }
        return [Seg(t: " 👾  BEST:\(max(best, s)) | SCORE:\(s) | WAVE:\(wave) | WEAPON:", c: HUDC), m]
    }
}

// MARK: - Jump

let JUMP_SKINS = ["#00ff00", "#ff5f00", "#ffff00", "#af5fff"]

final class Jump: TermGame {
    private struct Plat { var x: Double; var y: Double; var a: Bool; var d: Int; var max: Int; var b: Int }
    private let best: Int
    private let BG = hex("#262626"), BORDER = hex("#af87ff"), GLOW = hex("#d787ff"), HUDC = hex("#ffd7ff"), WHITE = hex("#e5e5e5")
    private let CYAN = hex("#00afff"), YELLOW = hex("#ffff00"), GREEN = hex("#00ff00"), PURPLE = hex("#af5fff")
    private let jp = -0.65, maxFall = 0.65
    private let yellowProb = 6, purpleProb = 15
    private let player: Color
    private var px = 29.0
    private var py = 16.0
    private var vy = -0.65
    private var target = 29.0
    private var groundY = 18.0
    private var ground = true
    private var pl: [Plat] = [
        Plat(x: 6, y: 14, a: true, d: 1, max: -1, b: -1),
        Plat(x: 22, y: 10, a: true, d: -1, max: -1, b: -1),
        Plat(x: 38, y: 6, a: true, d: 1, max: -1, b: -1),
        Plat(x: 18, y: 2, a: true, d: 0, max: -1, b: -1),
    ]

    init(_ o: GameOpts) {
        best = o.best
        let idx = (Int(o.opts["skin"] ?? "") ?? 1) - 1
        player = hex(idx >= 0 && idx < JUMP_SKINS.count ? JUMP_SKINS[idx] : "#00ff00")
        super.init(58, 19)
    }

    override func tap(_ col: Int, _ side: Int) { target = max(3, min(Double(W - 3), Double(col))) }
    override func drag(_ dx: Double) { target = max(3, min(Double(W - 3), target + dx)) }

    override func tick() {
        if !started || over { return }
        px += (target - px) * 0.16
        vy = min(maxFall, vy + 0.026)
        py += vy
        if vy > 0 {
            if ground && py >= groundY {
                py = groundY; vy = jp
            } else {
                for i in 0..<pl.count {
                    let p = pl[i]
                    if p.a && py >= p.y && py - vy <= p.y && px + 1 >= p.x && px - 3 <= p.x + 9 {
                        py = p.y
                        if p.max == -2 {
                            vy = jp * 3
                        } else {
                            vy = jp
                            if p.max != -1 { pl[i].b -= 1; if pl[i].b <= 0 { pl[i].a = false } }
                        }
                        break
                    }
                }
            }
        }
        for i in 0..<pl.count where pl[i].a && pl[i].d != 0 {
            pl[i].x += Double(pl[i].d) * 0.09
            if pl[i].x < 2 { pl[i].x = 2; pl[i].d = 1 }
            if pl[i].x > Double(W - 11) { pl[i].x = Double(W - 11); pl[i].d = -1 }
        }
        if py < 7 {
            let shift = 7 - py
            py = 7; score += shift
            if ground { groundY += shift; if groundY > Double(H) { ground = false } }
            for i in 0..<pl.count {
                pl[i].y += shift
                if pl[i].y > Double(H - 1) {
                    pl[i].y -= 16; pl[i].x = Double(rnd(W - 12) + 2); pl[i].a = true; pl[i].d = rnd(3) - 1
                    if rnd(purpleProb) == 0 { pl[i].max = -2; pl[i].b = -1 }
                    else if rnd(yellowProb) == 0 { pl[i].max = 2; pl[i].b = 2 }
                    else { pl[i].max = -1; pl[i].b = -1 }
                }
            }
        }
        if py > Double(H) { over = true }
    }

    override func draw(_ g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        if !started {
            g.center(H / 2 - 2, "TAP TO JUMP START", WHITE, W)
            g.center(H / 2, "CONTROLS: TAP / SWIPE LEFT | RIGHT", HUDC, W)
            g.inner(px - 3, H - 3, "▓▀█▀▓", player)
            g.inner(px - 3, H - 2, "▓███▓", player)
            return
        }
        if ground { g.inner(0, groundY, rep("█", W), GREEN) }
        for p in pl where p.a {
            g.inner(p.x - 1, p.y, rep("█", 10), p.max == -2 ? PURPLE : (p.max != -1 ? YELLOW : CYAN))
        }
        g.inner(px - 3, py - 1, "▓▀█▀▓", player)
        g.inner(px - 3, py, "▓███▓", player)
    }

    override func hud() -> [Seg] {
        let s = Int(score.rounded(.down))
        return [Seg(t: " BEST:\(max(best, s)) | SCORE:\(s)m | ZONE: \(s)m", c: HUDC)]
    }
}

// MARK: - Snake

final class Snake: TermGame {
    private struct Pt { var x: Int; var y: Int }
    private let best: Int
    private let vibrate: () -> Void
    private let BG = hex("#06140a"), GRID = hex("#0f2a16"), BORDER = hex("#55ff55"), HEAD = hex("#aaffaa")
    private let BODY = hex("#33dd55"), FOOD = hex("#ff3355"), HUDC = hex("#ccffcc"), WHITE = hex("#ffffff")
    private let gw = 29, gh = 19
    private var body = [Pt(x: 14, y: 10), Pt(x: 13, y: 10), Pt(x: 12, y: 10)]
    private var dx = 1, dy = 0
    private var nextDx = 1, nextDy = 0
    private var food = Pt(x: 20, y: 10)

    init(_ o: GameOpts) { best = o.best; vibrate = o.vibrate; super.init(58, 19) }

    private func place() {
        repeat {
            food = Pt(x: rnd(gw) + 1, y: rnd(gh) + 1)
        } while body.contains(where: { $0.x == food.x && $0.y == food.y })
    }

    override func delay() -> Double { max(70, 160 - score * 4) }

    override func dir(_ nx: Int, _ ny: Int) {
        if nx != -dx || ny != -dy { nextDx = nx; nextDy = ny }
    }

    override func tick() {
        if !started || over { return }
        dx = nextDx; dy = nextDy
        let head = body[0]
        let h = Pt(x: head.x + dx, y: head.y + dy)
        if h.x < 1 || h.x > gw || h.y < 1 || h.y > gh || body.contains(where: { $0.x == h.x && $0.y == h.y }) {
            over = true
            vibrate()
            return
        }
        body.insert(h, at: 0)
        if h.x == food.x && h.y == food.y { score += 1; place() } else { body.removeLast() }
    }

    override func draw(_ g: Grid) {
        g.frame(gw * 2, gh, BORDER, BORDER, BG)
        for y in 1...gh { for x in 1...gw where (x + y) % 2 == 0 { g.inner((x - 1) * 2, y, "░░", GRID) } }
        let blink = Int(nowSec() * 1000 / 300) % 2 == 0
        g.inner((food.x - 1) * 2, food.y, blink ? "██" : "▓▓", FOOD)
        for (i, p) in body.enumerated() { g.inner((p.x - 1) * 2, p.y, "██", i == 0 ? HEAD : BODY) }
        if !started {
            g.center(5, "TAP TO START", WHITE, gw * 2)
            g.center(7, "JOYSTICK: IRÁNY", HUDC, gw * 2)
        }
    }

    override func hud() -> [Seg] { [Seg(t: " SCORE: \(Int(score))   BEST: \(max(best, Int(score)))", c: HUDC)] }
}

// MARK: - Memory

final class Memory: TermGame {
    private let best: Int
    private let BG = hex("#14051f"), BORDER = hex("#af87ff"), GLOW = hex("#d787ff"), BACK = hex("#5f2f9f")
    private let MATCH = hex("#d787ff"), CUR = hex("#ffffff"), HUDC = hex("#ffd7ff"), WHITE = hex("#ffffff"), BLACK = hex("#000000")
    private let SYMBOLS = ["▲", "●", "♥", "♦", "♣", "♠", "★", "◆"]
    private let COLORS: [Color] = ["#00ffff", "#ffff00", "#ff3355", "#55ff55", "#ff5f00", "#5fd7ff", "#ffd7ff", "#af87ff"].map { hex($0) }
    private let cardW = 12, cardH = 4
    private var deck: [Int] = (Array(0..<8) + Array(0..<8)).shuffled()
    private var shown = [Bool](repeating: false, count: 16)
    private var done = [Bool](repeating: false, count: 16)
    private var open: [Int] = []
    private var wait = 0
    private var cur = 0
    private var moves = 0

    init(_ o: GameOpts) { best = o.best; super.init(4 * (12 + 2) + 2, 4 * (4 + 1) - 1) }

    override var score: Double {
        get { Double(max(0, 100 - moves * 2)) }
        set { }
    }

    override var usesTapAt: Bool { true }

    private func flip(_ i: Int) {
        if i < 0 || i > 15 { return }
        if wait != 0 || done[i] || shown[i] { return }
        shown[i] = true; open.append(i)
        if open.count == 2 {
            moves += 1
            let a = open[0], b = open[1]
            if deck[a] == deck[b] {
                done[a] = true; done[b] = true; open = []
                if !done.contains(false) { over = true }
            } else {
                wait = 45
            }
        }
    }

    private func x0(_ c: Int) -> Int { 1 + c * (cardW + 2) }
    private func y0(_ r: Int) -> Int { 1 + r * (cardH + 1) }

    override func dir(_ dx: Int, _ dy: Int) {
        cur = (((cur / 4) + dy + 4) % 4) * 4 + ((cur % 4) + dx + 4) % 4
    }

    override func press() { flip(cur) }

    override func tapAt(_ col: Int, _ row: Int) {
        let c = Int((Double(col - 1) / Double(cardW + 2)).rounded(.down))
        let r = Int((Double(row - 1) / Double(cardH + 1)).rounded(.down))
        if c >= 0 && c < 4 && r >= 0 && r < 4 { cur = r * 4 + c; flip(cur) }
    }

    override func tick() {
        if wait != 0 {
            wait -= 1
            if wait == 0 { for i in open { shown[i] = false }; open = [] }
        }
    }

    override func draw(_ g: Grid) {
        g.frame(W, H, BORDER, GLOW, BG)
        for i in 0..<16 {
            let c = i % 4, r = i / 4
            let face = shown[i] || done[i]
            let col = done[i] ? MATCH : (face ? COLORS[deck[i]] : BACK)
            for y in 0..<cardH { g.inner(x0(c), y0(r) + y, face ? rep("▒", cardW) : rep("▓", cardW), col) }
            if face { g.inner(x0(c) + cardW / 2 - 1, y0(r) + 1, SYMBOLS[deck[i]] + " ", WHITE, bg: BLACK) }
            if i == cur && started {
                g.inner(x0(c) - 1, y0(r), "▶", CUR)
                g.inner(x0(c) + cardW, y0(r) + cardH - 1, "◀", CUR)
            }
        }
        if !started { g.center(H / 2, " TAP TO START ", WHITE, W) }
        if over { g.center(H / 2, " KÉSZ! \(moves) LÉPÉS ", WHITE, W) }
    }

    override func hud() -> [Seg] {
        let s = Int(score)
        return [Seg(t: " LÉPÉS: \(moves)   PONT: \(s)   BEST: \(max(best, s))", c: HUDC)]
    }
}
