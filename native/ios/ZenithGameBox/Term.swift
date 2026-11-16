import SwiftUI

/// Terminál-cellás rács: az eredeti shell (ANSI) grafika natív megfelelője.
let CW: Double = 12
let CH: Double = 24

func hex(_ s: String) -> Color {
    var h = s
    if h.hasPrefix("#") { h.removeFirst() }
    let v = UInt32(h, radix: 16) ?? 0xffffff
    return Color(.sRGB,
                 red: Double((v >> 16) & 0xff) / 255,
                 green: Double((v >> 8) & 0xff) / 255,
                 blue: Double(v & 0xff) / 255,
                 opacity: 1)
}

func rnd(_ n: Int) -> Int { Int.random(in: 0..<n) }

/// Másodperc (monoton óra), az eredeti performance.now()/1000 megfelelője.
func nowSec() -> Double { ProcessInfo.processInfo.systemUptime }

protocol GridNum { var d: Double { get } }
extension Int: GridNum { var d: Double { Double(self) } }
extension Double: GridNum { var d: Double { self } }

struct Cell {
    var ch: String = " "
    var fg: Color = .white
    var bg: Color = .black
    var wide: Bool = false
    var ox: Double = 0
    var oy: Double = 0
}

struct Seg {
    var t: String
    var c: Color? = nil
}

func rep(_ s: String, _ n: Int) -> String { String(repeating: s, count: max(0, n)) }

final class Grid {
    let cols: Int
    let rows: Int
    var cells: [[Cell]]

    init(_ cols: Int, _ rows: Int, bg: Color = .black) {
        self.cols = cols
        self.rows = rows
        cells = Array(repeating: Array(repeating: Cell(ch: " ", fg: .white, bg: bg), count: cols), count: rows)
    }

    func fill(_ bg: Color) {
        for y in 0..<rows {
            for x in 0..<cols {
                cells[y][x].ch = " "
                cells[y][x].bg = bg
                cells[y][x].wide = false
                cells[y][x].ox = 0
                cells[y][x].oy = 0
            }
        }
    }

    /// Szöveg kiírása a rács x,y pozíciójára. Az emoji / széles karakter két cellát foglal.
    func put(_ xIn: Double, _ yIn: Double, _ str: String, _ fg: Color, bg: Color? = nil, minX: Int = 0, maxX: Int? = nil) {
        let maxXv = maxX ?? (cols - 1)
        let rx = (xIn + 0.5).rounded(.down)
        let ry = (yIn + 0.5).rounded(.down)
        let ox = xIn - rx
        let oy = yIn - ry
        var x = Int(rx)
        let y = Int(ry)
        if y < 0 || y >= rows { return }
        for sc in str.unicodeScalars {
            let wide = sc.value >= 0x1f000
            if x >= minX && x <= maxXv && x >= 0 && x < cols {
                cells[y][x].ch = String(sc)
                cells[y][x].fg = fg
                cells[y][x].wide = wide
                cells[y][x].ox = ox
                cells[y][x].oy = oy
                if let bg = bg { cells[y][x].bg = bg }
                if wide && x + 1 <= maxXv && x + 1 < cols {
                    cells[y][x + 1].ch = ""
                    cells[y][x + 1].wide = false
                    if let bg = bg { cells[y][x + 1].bg = bg }
                }
            }
            x += wide ? 2 : 1
        }
    }

    /// Zenith keret: ╔═╗ / ║ ║ / ╚═╝ egy W×H mező körül.
    func frame(_ w: Int, _ h: Int, _ side: Color, _ glow: Color, _ bg: Color) {
        fill(bg)
        put(0, 0, "╔" + rep("═", w) + "╗", glow)
        put(0, Double(h + 1), "╚" + rep("═", w) + "╝", glow)
        if h >= 1 {
            for y in 1...h {
                put(0, Double(y), "║", side)
                put(Double(w + 1), Double(y), "║", side)
            }
        }
    }

    /// Írás a belső mezőbe a shell 0-alapú oszlopindexével.
    func inner(_ i: some GridNum, _ y: some GridNum, _ str: String, _ fg: Color, bg: Color? = nil) {
        put(i.d + 1, y.d, str, fg, bg: bg, minX: 1, maxX: cols - 2)
    }

    func center(_ y: some GridNum, _ str: String, _ fg: Color, _ w: Int) {
        let n = str.unicodeScalars.count
        inner(((Double(w - n)) / 2).rounded(.down), y, str, fg)
    }
}

class TermGame {
    let W: Int
    let H: Int
    var started = false
    var over = false
    var score: Double = 0

    init(_ w: Int, _ h: Int) { W = w; H = h }

    func delay() -> Double { 16 }
    func start() { started = true }
    func tick() {}
    func draw(_ g: Grid) {}
    func hud() -> [Seg] { [] }
    /// Koppintás: belső oszlop (0-alapú) és a képernyő oldala (-1 bal / 1 jobb).
    func tap(_ col: Int, _ side: Int) {}
    /// Húzás: vízszintes elmozdulás oszlopban az előző eseményhez képest.
    func drag(_ dx: Double) {}
    /// Négyirányú joystick-lépés (-1/0/1).
    func dir(_ dx: Int, _ dy: Int) {}
    /// Joystick-nyomás (kitérés nélkül felengedett koppintás).
    func press() {}
    /// Ha igaz, a vászon koppintása közvetlenül cellára (oszlop, sor) érkezik.
    var usesTapAt: Bool { false }
    func tapAt(_ col: Int, _ row: Int) {}
}

struct GameOpts {
    var best: Int
    var opts: [String: String]
    var vibrate: () -> Void
}

struct OptChoice {
    var value: String
    var label: String
    var color: Color? = nil
}

struct OptDef {
    var key: String
    var label: String
    var choices: [OptChoice]
}

struct GameDef {
    var id: String
    var name: String
    var make: (GameOpts) -> TermGame
    var options: [OptDef]
}

let GAMES: [GameDef] = [
    GameDef(id: "bird", name: "ZENITH BIRD", make: { Bird($0) }, options: []),
    GameDef(id: "dino", name: "ZENITH DINO", make: { Dino($0) }, options: []),
    GameDef(id: "pong", name: "ZENITH PONG", make: { Pong($0) }, options: [
        OptDef(key: "ball", label: "Ball", choices: PONG_BALLS.map { OptChoice(value: $0, label: $0) }),
        OptDef(key: "pad", label: "Paddle", choices: PONG_PADS.map { OptChoice(value: $0, label: "▓▓▓", color: hex($0)) }),
    ]),
    GameDef(id: "space", name: "ZENITH SPACE", make: { Space($0) }, options: [
        OptDef(key: "bullet", label: "Bullet", choices: SPACE_BULLETS.map { OptChoice(value: $0.0, label: "|", color: hex($0.1)) }),
        OptDef(key: "ship", label: "Ship", choices: SPACE_SHIPS.map { OptChoice(value: $0, label: $0, color: hex("#af87ff")) }),
    ]),
    GameDef(id: "jump", name: "ZENITH JUMP", make: { Jump($0) }, options: [
        OptDef(key: "skin", label: "Block Skin", choices: JUMP_SKINS.enumerated().map { OptChoice(value: String($0.offset + 1), label: "██", color: hex($0.element)) }),
    ]),
    GameDef(id: "snake", name: "ZENITH SNAKE", make: { Snake($0) }, options: []),
    GameDef(id: "memory", name: "ZENITH MEMORY", make: { Memory($0) }, options: []),
]

func inkOf(_ id: String) -> Color {
    switch id {
    case "bird": return hex("#ffff11")
    case "dino", "memory": return hex("#b98cff")
    case "pong": return hex("#ff6600")
    case "space": return hex("#2bffff")
    case "jump", "snake": return hex("#5dfd6d")
    default: return .white
    }
}
