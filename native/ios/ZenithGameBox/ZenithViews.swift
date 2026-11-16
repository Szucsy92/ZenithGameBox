import SwiftUI
import UIKit

enum Z {
    static let bg = hex("#060410")
    static let fg = hex("#f4effe")
    static let card = hex("#110b1f")
    static let primary = hex("#ad8dfd")
    static let secondary = hex("#211b30")
    static let mutedFg = hex("#918aab")
    static let border = hex("#352c4e")
    static let dino = hex("#b98cff")
}

// MARK: - Gombok fénye (9 mp-es ciklus: lila → narancs → zöld → sárga)

private let glowKeys: [((Double, Double, Double), Double)] = [
    ((0xad, 0x8d, 0xfd), 1), ((0xff, 0x66, 0x00), 0.33), ((0x5d, 0xfd, 0x6d), 1), ((0xff, 0xff, 0x11), 0.33), ((0xad, 0x8d, 0xfd), 1),
]

private func glowAt(_ p: Double) -> (Color, Double) {
    let s = min(max(p, 0), 0.9999) * 4
    let i = Int(s)
    let t0 = s - Double(i)
    let t = t0 * t0 * (3 - 2 * t0)
    let a = glowKeys[i], b = glowKeys[i + 1]
    let r = a.0.0 + (b.0.0 - a.0.0) * t
    let g = a.0.1 + (b.0.1 - a.0.1) * t
    let bl = a.0.2 + (b.0.2 - a.0.2) * t
    return (Color(.sRGB, red: r / 255, green: g / 255, blue: bl / 255, opacity: 1), a.1 + (b.1 - a.1) * t)
}

// MARK: - Vezérlők

final class TouchState {
    var started = false
    var sx = 0.0, sy = 0.0, lastX = 0.0
    var dir = ""
    var maxD = 0.0
    var task: Task<Void, Never>?
}

struct TouchControl: View {
    let side: Int
    let repeatOn: Bool
    var onTap: ((Int) -> Void)?
    var onDrag: ((Double) -> Void)?
    var onStick: ((Int, Int) -> Void)?
    var onPress: (() -> Void)?

    @State private var knob = CGSize.zero
    @State private var active = false
    @State private var ts = TouchState()

    private let reach = 22.0
    private let step = 12.0

    private var enabled: Bool { onTap != nil || onStick != nil || onPress != nil }

    private func stopRepeat() { ts.task?.cancel(); ts.task = nil }

    private func startRepeat(_ dx: Int, _ dy: Int) {
        stopRepeat()
        if !repeatOn { return }
        let cb = onStick
        ts.task = Task {
            try? await Task.sleep(nanoseconds: 380_000_000)
            while !Task.isCancelled {
                await MainActor.run { cb?(dx, dy) }
                try? await Task.sleep(nanoseconds: 160_000_000)
            }
        }
    }

    private func changed(_ v: DragGesture.Value) {
        if !enabled { return }
        if !ts.started {
            ts.started = true
            ts.sx = v.startLocation.x; ts.sy = v.startLocation.y; ts.lastX = v.startLocation.x
            ts.dir = ""; ts.maxD = 0
            active = true
            knob = .zero
            onTap?(side)
        }
        let px = v.location.x
        if px != ts.lastX { onDrag?(px - ts.lastX); ts.lastX = px }
        let x = v.location.x - ts.sx
        let y = v.location.y - ts.sy
        let d = hypot(x, y)
        ts.maxD = max(ts.maxD, d)
        let k = d > reach ? reach / d : 1
        knob = CGSize(width: x * k, height: y * k)
        if onStick != nil {
            var nd = ts.dir
            if d >= step {
                nd = abs(x) > abs(y) ? (x > 0 ? "R" : "L") : (y > 0 ? "D" : "U")
            } else if d < step / 2 {
                nd = ""
            }
            if nd != ts.dir {
                if !nd.isEmpty {
                    let dx = nd == "R" ? 1 : (nd == "L" ? -1 : 0)
                    let dy = nd == "D" ? 1 : (nd == "U" ? -1 : 0)
                    onStick?(dx, dy)
                    startRepeat(dx, dy)
                } else {
                    stopRepeat()
                }
            }
            ts.dir = nd
        }
    }

    private func ended() {
        if !ts.started { return }
        ts.started = false
        stopRepeat()
        active = false
        knob = .zero
        if ts.maxD < step { onPress?() }
    }

    var body: some View {
        let dist = min(reach, hypot(knob.width, knob.height))
        let pull = dist / reach
        let angle = dist > 0.4 ? atan2(knob.height, knob.width) * 180 / .pi : 0
        TimelineView(.animation) { tl in
            let p = (tl.date.timeIntervalSinceReferenceDate / 9).truncatingRemainder(dividingBy: 1)
            let (gc, gs) = glowAt(p)
            ZStack {
                Circle().fill(Z.secondary)
                Circle().strokeBorder(Z.primary, lineWidth: 3)
                Circle()
                    .fill(RadialGradient(colors: [gc.opacity(0.35), gc.opacity(0.85 * gs)], center: .center, startRadius: 5, endRadius: 22))
                    .frame(width: 44, height: 44)
                    .overlay(Circle().stroke(gc.opacity(0.6 + 0.4 * gs), lineWidth: 1))
                    .shadow(color: gc.opacity(0.7), radius: active ? 9 : 5)
                    .scaleEffect(x: 1 + 0.06 * pull, y: 1 - 0.05 * pull)
                    .rotationEffect(.degrees(angle))
                    .offset(knob)
                    .animation(active ? Animation.easeOut(duration: 0.09) : Animation.spring(response: 0.45, dampingFraction: 0.45), value: knob)
            }
            .frame(width: 92, height: 92)
            .shadow(color: gc.opacity(0.5 * gs), radius: 10 + 14 * gs)
        }
        .frame(width: 92, height: 92)
        .opacity(enabled ? 1 : 0.65)
        .contentShape(Circle())
        .gesture(DragGesture(minimumDistance: 0).onChanged { changed($0) }.onEnded { _ in ended() })
        .accessibilityLabel(side < 0 ? "Bal érintőgomb" : "Jobb érintőgomb")
    }
}

struct ConsoleControls: View {
    var onMenu: (() -> Void)? = nil
    var over: Bool = false
    var repeatOn: Bool = false
    var onTap: ((Int) -> Void)? = nil
    var onDrag: ((Double) -> Void)? = nil
    var onStick: ((Int, Int) -> Void)? = nil
    var onPress: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            TouchControl(side: -1, repeatOn: repeatOn, onTap: onTap, onDrag: onDrag, onStick: onStick, onPress: onPress)
            VStack(spacing: 3) {
                Text(over ? "GAME OVER" : " ")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(Z.fg)
                    .lineLimit(1)
                    .frame(minHeight: 16)
                Button(action: { onMenu?() }) {
                    Text("MENÜ")
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundColor(Z.mutedFg)
                        .padding(.horizontal, 8).padding(.vertical, 8)
                }
                .disabled(onMenu == nil)
                .opacity(onMenu == nil ? 0.5 : 1)
            }
            .frame(maxWidth: .infinity)
            TouchControl(side: 1, repeatOn: repeatOn, onTap: onTap, onDrag: onDrag, onStick: onStick, onPress: onPress)
        }
        .padding(.top, 20).padding(.horizontal, 4).padding(.bottom, 4)
    }
}

// MARK: - Menü

private struct FramesKey: PreferenceKey {
    static var defaultValue: [Int: CGRect] = [:]
    static func reduce(value: inout [Int: CGRect], nextValue: () -> [Int: CGRect]) {
        value.merge(nextValue()) { $1 }
    }
}

final class MenuTouch { var moved = false }

struct GameMenu: View {
    @Binding var cursor: Int
    let best: [Int]
    let onPick: (Int) -> Void
    @State private var frames: [Int: CGRect] = [:]
    @State private var touch = MenuTouch()

    private func idxAt(_ p: CGPoint) -> Int? {
        for (i, r) in frames where r.contains(p) { return i }
        return nil
    }

    var body: some View {
        ZStack {
            Image("zenith_start")
                .interpolation(.none)
                .resizable()
                .scaledToFill()
                .opacity(0.75)
            VStack(spacing: 8) {
                ForEach(GAMES.indices, id: \.self) { i in
                    let sel = i == cursor
                    HStack {
                        Text("\(i + 1)) " + GAMES[i].name.replacingOccurrences(of: "ZENITH ", with: ""))
                            .font(.system(size: sel ? 15 : 13, weight: .medium, design: .monospaced))
                            .foregroundColor(inkOf(GAMES[i].id))
                        Spacer(minLength: 4)
                        Text("BEST \(best.indices.contains(i) ? best[i] : 0)")
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(Z.mutedFg)
                    }
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .frame(minHeight: sel ? 46 : 38)
                    .background(Z.bg.opacity(0.88))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(sel ? Z.dino : Z.border, lineWidth: 2))
                    .shadow(color: sel ? Z.dino.opacity(0.6) : .clear, radius: 8)
                    .scaleEffect(sel ? 1.1 : 1)
                    .zIndex(sel ? 1 : 0)
                    .animation(.easeOut(duration: 0.18), value: cursor)
                    .background(GeometryReader { gp in
                        Color.clear.preference(key: FramesKey.self, value: [i: gp.frame(in: .named("menu"))])
                    })
                }
            }
            .padding(.horizontal, 30).padding(.vertical, 18)
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(3.0 / 4.0, contentMode: .fit)
        .clipped()
        .coordinateSpace(name: "menu")
        .onPreferenceChange(FramesKey.self) { frames = $0 }
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0, coordinateSpace: .named("menu"))
                .onChanged { v in
                    if abs(v.translation.height) > 12 { touch.moved = true }
                    if let i = idxAt(v.location) { cursor = i }
                }
                .onEnded { v in
                    if !touch.moved, let i = idxAt(v.location) { onPick(i) }
                    touch.moved = false
                }
        )
    }
}

// MARK: - Játékfelület

enum Phase { case setup, play, over }

final class GameRunner {
    let def: GameDef
    var game: TermGame?
    var grid = Grid(60, 21)
    var phase: Phase = .setup
    var opts: [String: String] = [:]
    var hud: [Seg] = []
    var aspect = 60.0 * CW / (21.0 * CH)
    var onPhase: ((Phase) -> Void)?
    var canvasW = 0.0
    private var best = 0
    private var lastT = 0.0, acc = 0.0, overAt = 0.0
    var gestureOpen = false
    private var touching = false
    private var lastCol = 0.0

    init(def: GameDef) {
        self.def = def
        for d in def.options {
            let stored = UserDefaults.standard.string(forKey: "zenith_\(def.id)_\(d.key)") ?? ""
            opts[d.key] = stored.isEmpty ? (d.choices.first?.value ?? "") : stored
        }
        makeGame()
    }

    private func notify() {
        let p = phase
        DispatchQueue.main.async { self.onPhase?(p) }
    }

    func makeGame() {
        best = UserDefaults.standard.integer(forKey: "zenith_\(def.id)_hs")
        let gm = def.make(GameOpts(best: best, opts: opts, vibrate: {
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        }))
        if phase == .play { gm.start() }
        game = gm
        grid = Grid(gm.W + 2, gm.H + 2)
        aspect = Double((gm.W + 2)) * CW / (Double(gm.H + 2) * CH)
        acc = 0; lastT = 0; overAt = 0
    }

    func setOpt(_ k: String, _ v: String) {
        UserDefaults.standard.set(v, forKey: "zenith_\(def.id)_\(k)")
        opts[k] = v
        makeGame()
    }

    func restart() {
        phase = .play
        makeGame()
        notify()
    }

    func advance(_ now: Double) {
        guard let gm = game else { return }
        if lastT == 0 { lastT = now; return }
        acc += min(200, (now - lastT) * 1000)
        lastT = now
        var d = gm.delay()
        while acc >= d { acc -= d; gm.tick(); d = gm.delay() }
        if gm.over && phase == .play {
            if overAt == 0 {
                overAt = now
                let sc = Int(gm.score)
                if sc > best { UserDefaults.standard.set(sc, forKey: "zenith_\(def.id)_hs") }
            } else if (now - overAt) * 1000 > 800 {
                phase = .over
                notify()
            }
        }
        gm.draw(grid)
        hud = gm.hud()
    }

    // Vászon-érintés
    func canvasDown(_ x: Double, _ y: Double, _ w: Double, _ h: Double) {
        guard let gm = game, w > 0, h > 0 else { return }
        if phase == .over { restart(); return }
        if phase == .setup { phase = .play; gm.start(); notify(); return }
        let cols = Double(gm.W + 2)
        lastCol = x / w * cols - 1
        touching = true
        let side = x < w / 2 ? -1 : 1
        if gm.usesTapAt {
            gm.tapAt(Int(lastCol.rounded(.down)), Int((y / h * Double(gm.H + 2)).rounded(.down)))
        } else {
            gm.tap(Int(lastCol.rounded(.down)), side)
        }
    }

    func canvasMove(_ x: Double, _ w: Double) {
        guard let gm = game, touching, phase == .play, w > 0 else { return }
        let col = x / w * Double(gm.W + 2) - 1
        let dx = col - lastCol
        if abs(dx) >= 1 { gm.drag(dx); lastCol = col }
    }

    func canvasUp() { touching = false }

    // Érintőgombok
    func controlTap(_ side: Int) {
        guard let gm = game else { return }
        if phase == .over || gm.over { restart(); return }
        if phase == .setup { phase = .play; gm.start(); notify() }
        gm.tap(Int((Double(gm.W) * (side < 0 ? 0.25 : 0.75)).rounded(.down)), side)
    }

    func controlDrag(_ dx: Double) {
        guard let gm = game, canvasW > 0, phase == .play, !gm.over else { return }
        gm.drag(dx * Double(gm.W + 2) / canvasW)
    }

    func stick(_ dx: Int, _ dy: Int) {
        if let gm = game, phase == .play, !gm.over { gm.dir(dx, dy) }
    }

    func press() {
        if let gm = game, phase == .play, !gm.over { gm.press() }
    }
}

struct PlayerView: View {
    let def: GameDef
    let screenW: Double
    let onExit: () -> Void
    @State private var runner: GameRunner
    @State private var phase: Phase = .setup
    @State private var opts: [String: String] = [:]

    init(def: GameDef, screenW: Double, onExit: @escaping () -> Void) {
        self.def = def
        self.screenW = screenW
        self.onExit = onExit
        let r = GameRunner(def: def)
        _runner = State(initialValue: r)
        _opts = State(initialValue: r.opts)
    }

    private func hudText(_ segs: [Seg]) -> Text {
        segs.reduce(Text("")) { acc, s in
            acc + Text(s.t).foregroundColor(s.c ?? Z.fg)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            TimelineView(.animation) { tl in
                let _ = runner.advance(tl.date.timeIntervalSinceReferenceDate)
                VStack(alignment: .leading, spacing: 0) {
                    Text(def.name)
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(inkOf(def.id))
                        .padding(12)
                    GeometryReader { geo in
                        Canvas { ctx, size in
                            renderGrid(ctx, size, runner.grid)
                        }
                        .contentShape(Rectangle())
                        .gesture(
                            DragGesture(minimumDistance: 0)
                                .onChanged { v in
                                    runner.canvasW = geo.size.width
                                    if !runner.gestureOpen {
                                        runner.gestureOpen = true
                                        runner.canvasDown(v.startLocation.x, v.startLocation.y, geo.size.width, geo.size.height)
                                    } else {
                                        runner.canvasMove(v.location.x, geo.size.width)
                                    }
                                }
                                .onEnded { _ in runner.gestureOpen = false; runner.canvasUp() }
                        )
                    }
                    .aspectRatio(runner.aspect, contentMode: .fit)
                    hudText(runner.hud)
                        .font(.system(size: 10, design: .monospaced))
                        .frame(maxWidth: .infinity, minHeight: 24, alignment: .leading)
                        .padding(.horizontal, 8).padding(.vertical, 8)
                        .background(Z.bg)
                    if phase == .setup && !def.options.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(def.options.indices, id: \.self) { oi in
                                let d = def.options[oi]
                                HStack(alignment: .top, spacing: 8) {
                                    Text("\(d.label):")
                                        .font(.system(size: 12, design: .monospaced))
                                        .foregroundColor(Z.mutedFg)
                                        .frame(width: 64, alignment: .leading)
                                        .padding(.top, 8)
                                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 40), spacing: 8)], alignment: .leading, spacing: 8) {
                                        ForEach(d.choices.indices, id: \.self) { ci in
                                            let c = d.choices[ci]
                                            Button(action: {
                                                runner.setOpt(d.key, c.value)
                                                opts[d.key] = c.value
                                            }) {
                                                Text(c.label)
                                                    .font(.system(size: 12, design: .monospaced))
                                                    .foregroundColor(c.color ?? Z.fg)
                                                    .lineLimit(1)
                                                    .padding(.horizontal, 8).padding(.vertical, 4)
                                                    .frame(minHeight: 32)
                                                    .background(Z.bg)
                                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                                                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(opts[d.key] == c.value ? Z.primary : Z.border, lineWidth: 2))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 12).padding(.vertical, 8)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: screenW * 4 / 3, alignment: .topLeading)
            }
            .background(Z.bg)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Z.border, lineWidth: 2))
            ConsoleControls(
                onMenu: onExit,
                over: phase == .over,
                onTap: { runner.controlTap($0) },
                onDrag: { runner.controlDrag($0) },
                onStick: { runner.stick($0, $1) },
                onPress: { runner.press() }
            )
        }
        .onAppear { runner.onPhase = { phase = $0 } }
    }
}

// MARK: - Fő nézet

struct ZenithRootView: View {
    @State private var selected: Int? = nil
    @State private var cursor = 0
    @State private var best: [Int] = []

    private func loadBest() {
        best = GAMES.map { UserDefaults.standard.integer(forKey: "zenith_\($0.id)_hs") }
    }

    var body: some View {
        ZStack {
            Z.bg.ignoresSafeArea()
            GeometryReader { geo in
                let small = geo.size.width <= 380
                let gap = small ? 10.0 : 12.0
                let padX = small ? 8.0 : 16.0
                let padY = small ? 12.0 : 24.0
                let availW = geo.size.width - padX * 2
                let availH = geo.size.height - padY * 2
                let fixed = 40.0 + 16.0 + gap * 2 + 40.0 + 116.0
                let byHeight = 40.0 + (availH - fixed) * 0.75
                let hw = max(220.0, min(480.0, min(availW, byHeight)))
                let pad = small ? 10.0 : 14.0
                let screenW = hw - 2 * pad - 8 - 4
                VStack(spacing: gap) {
                    HStack(spacing: 10) {
                        Image("zenith_icon").interpolation(.none).resizable().frame(width: 36, height: 36)
                        Text("ZENITH GAMES")
                            .font(.system(size: small ? 20 : 24, weight: .bold, design: .monospaced))
                            .foregroundColor(Z.primary)
                    }
                    VStack(spacing: 0) {
                        if let sel = selected, GAMES.indices.contains(sel) {
                            PlayerView(def: GAMES[sel], screenW: screenW, onExit: { selected = nil; loadBest() })
                                .id(GAMES[sel].id)
                        } else {
                            GameMenu(cursor: $cursor, best: best, onPick: { selected = $0 })
                                .background(Z.bg)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Z.border, lineWidth: 2))
                            ConsoleControls(
                                repeatOn: true,
                                onStick: { _, dy in if dy != 0 { cursor = (cursor + dy + GAMES.count) % GAMES.count } },
                                onPress: { selected = cursor }
                            )
                        }
                    }
                    .padding(pad)
                    .frame(width: hw)
                    .background(RoundedRectangle(cornerRadius: 44).fill(Z.card))
                    .overlay(RoundedRectangle(cornerRadius: 44).stroke(Z.primary, lineWidth: 4))
                    .shadow(color: Z.primary.opacity(0.35), radius: 22)
                    Text("Life is a game...")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(Z.mutedFg)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .preferredColorScheme(.dark)
        .onAppear { loadBest() }
    }
}
