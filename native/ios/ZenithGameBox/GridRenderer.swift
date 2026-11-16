import SwiftUI

/// Az eredeti canvas-rajzoló (renderGrid) SwiftUI Canvas megfelelője.
func renderGrid(_ ctxIn: GraphicsContext, _ size: CGSize, _ g: Grid) {
    var ctx = ctxIn
    let s = size.width / (Double(g.cols) * CW)
    ctx.scaleBy(x: s, y: s)
    for y in 0..<g.rows {
        for x in 0..<g.cols {
            let cell = g.cells[y][x]
            fillRect(&ctx, Double(x) * CW, Double(y) * CH, CW + 0.5, CH + 0.5, cell.bg)
        }
    }
    for y in 0..<g.rows {
        for x in 0..<g.cols {
            let cell = g.cells[y][x]
            let ch = cell.ch
            if ch.isEmpty || ch == " " { continue }
            let px = (Double(x) + cell.ox) * CW
            let py = (Double(y) + cell.oy) * CH
            if drawBlock(&ctx, ch, px, py, cell.fg) { continue }
            if cell.wide {
                let t = Text(ch).font(.system(size: CH - 4))
                ctx.draw(t, at: CGPoint(x: px + CW, y: py + CH / 2 + 1), anchor: .center)
            } else {
                let symbols = "◀▶▲▼●◆♥♦♣♠★❯❮"
                let text = symbols.contains(ch) ? ch + "\u{FE0E}" : ch
                let t = Text(text).font(.system(size: CH - 5, weight: .bold, design: .monospaced)).foregroundColor(cell.fg)
                ctx.draw(t, at: CGPoint(x: px + CW / 2, y: py + CH / 2 + 1), anchor: .center)
            }
        }
    }
}

private func fillRect(_ ctx: inout GraphicsContext, _ x: Double, _ y: Double, _ w: Double, _ h: Double, _ color: Color) {
    ctx.fill(Path(CGRect(x: x, y: y, width: w, height: h)), with: .color(color))
}

private func poly(_ ctx: inout GraphicsContext, _ color: Color, _ pts: [Double]) {
    var p = Path()
    p.move(to: CGPoint(x: pts[0], y: pts[1]))
    var i = 2
    while i + 1 < pts.count { p.addLine(to: CGPoint(x: pts[i], y: pts[i + 1])); i += 2 }
    p.closeSubpath()
    ctx.fill(p, with: .color(color))
}

private func drawBlock(_ ctx: inout GraphicsContext, _ ch: String, _ x: Double, _ y: Double, _ color: Color) -> Bool {
    let w = CW, h = CH, m = 2.0
    let cx = x + w / 2, cy = y + h / 2
    let dl = cx - 4, dr = cx + 2, dt = cy - 4, db = cy + 2
    switch ch {
    case "█": fillRect(&ctx, x, y, w + 0.5, h + 0.5, color)
    case "▀": fillRect(&ctx, x, y, w + 0.5, h / 2, color)
    case "▄": fillRect(&ctx, x, y + h / 2, w + 0.5, h / 2 + 0.5, color)
    case "▓": fillRect(&ctx, x, y, w + 0.5, h + 0.5, color.opacity(0.8))
    case "▒": fillRect(&ctx, x, y, w + 0.5, h + 0.5, color.opacity(0.5))
    case "░": fillRect(&ctx, x, y, w + 0.5, h + 0.5, color.opacity(0.25))
    case "━": fillRect(&ctx, x, cy - 2, w + 0.5, 4, color)
    case "═":
        fillRect(&ctx, x, dt, w + 0.5, m, color); fillRect(&ctx, x, db, w + 0.5, m, color)
    case "║":
        fillRect(&ctx, dl, y, m, h + 0.5, color); fillRect(&ctx, dr, y, m, h + 0.5, color)
    case "╔": dbl(&ctx, x, y, false, true, false, true, color)
    case "╗": dbl(&ctx, x, y, true, false, false, true, color)
    case "╚": dbl(&ctx, x, y, false, true, true, false, color)
    case "╝": dbl(&ctx, x, y, true, false, true, false, color)
    case "╣":
        fillRect(&ctx, dr, y, m, h + 0.5, color)
        fillRect(&ctx, dl, y, m, dt + m - y, color); fillRect(&ctx, dl, db, m, y + h - db + 0.5, color)
        fillRect(&ctx, x, dt, dl - x + m, m, color); fillRect(&ctx, x, db, dl - x + m, m, color)
    case "╠":
        fillRect(&ctx, dl, y, m, h + 0.5, color)
        fillRect(&ctx, dr, y, m, dt + m - y, color); fillRect(&ctx, dr, db, m, y + h - db + 0.5, color)
        fillRect(&ctx, dr, dt, x + w - dr + 0.5, m, color); fillRect(&ctx, dr, db, x + w - dr + 0.5, m, color)
    case "╦":
        fillRect(&ctx, x, dt, w + 0.5, m, color)
        fillRect(&ctx, x, db, dl + m - x, m, color); fillRect(&ctx, dr, db, x + w - dr + 0.5, m, color)
        fillRect(&ctx, dl, db, m, y + h - db + 0.5, color); fillRect(&ctx, dr, db, m, y + h - db + 0.5, color)
    case "╩":
        fillRect(&ctx, x, db, w + 0.5, m, color)
        fillRect(&ctx, x, dt, dl + m - x, m, color); fillRect(&ctx, dr, dt, x + w - dr + 0.5, m, color)
        fillRect(&ctx, dl, y, m, dt + m - y, color); fillRect(&ctx, dr, y, m, dt + m - y, color)
    case "╞":
        fillRect(&ctx, cx - 1, y, m, h + 0.5, color)
        fillRect(&ctx, cx - 1, dt, x + w - (cx - 1) + 0.5, m, color); fillRect(&ctx, cx - 1, db, x + w - (cx - 1) + 0.5, m, color)
    case "╡":
        fillRect(&ctx, cx - 1, y, m, h + 0.5, color)
        fillRect(&ctx, x, dt, cx + 1 - x, m, color); fillRect(&ctx, x, db, cx + 1 - x, m, color)
    case "╪":
        fillRect(&ctx, cx - 1, y, m, h + 0.5, color)
        fillRect(&ctx, x, dt, w + 0.5, m, color); fillRect(&ctx, x, db, w + 0.5, m, color)
    case "▶": poly(&ctx, color, [x + 2, cy - 6, x + 2, cy + 6, x + w - 1, cy])
    case "◀": poly(&ctx, color, [x + w - 2, cy - 6, x + w - 2, cy + 6, x + 1, cy])
    case "▲": poly(&ctx, color, [cx, cy - 6, x + 1, cy + 5, x + w - 1, cy + 5])
    case "▼": poly(&ctx, color, [x + 1, cy - 5, x + w - 1, cy - 5, cx, cy + 6])
    case "◆": poly(&ctx, color, [cx, cy - 7, x + w - 1, cy, cx, cy + 7, x + 1, cy])
    case "●": ctx.fill(Path(ellipseIn: CGRect(x: cx - 5.5, y: cy - 5.5, width: 11, height: 11)), with: .color(color))
    default: return false
    }
    return true
}

// Dupla vonalú sarok: melyik karok (bal, jobb, fel, le) léteznek.
private func dbl(_ ctx: inout GraphicsContext, _ x: Double, _ y: Double, _ l: Bool, _ r: Bool, _ u: Bool, _ d: Bool, _ color: Color) {
    let w = CW, h = CH, m = 2.0
    let cx = x + w / 2, cy = y + h / 2
    let ox = r ? cx - 4 : cx + 2
    let oy = d ? cy - 4 : cy + 2
    let ix = r ? cx + 2 : cx - 4
    let iy = d ? cy + 2 : cy - 4
    if r { fillRect(&ctx, ox, oy, x + w - ox + 0.5, m, color); fillRect(&ctx, ix, iy, x + w - ix + 0.5, m, color) }
    if l { fillRect(&ctx, x, oy, ox - x + m, m, color); fillRect(&ctx, x, iy, ix - x + m, m, color) }
    if d { fillRect(&ctx, ox, oy, m, y + h - oy + 0.5, color); fillRect(&ctx, ix, iy, m, y + h - iy + 0.5, color) }
    if u { fillRect(&ctx, ox, y, m, oy - y + m, color); fillRect(&ctx, ix, y, m, iy - y + m, color) }
}
