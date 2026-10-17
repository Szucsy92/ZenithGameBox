import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { GAMES, type GameDef } from "@/games/engine";
import { CH, CW, Grid, renderGrid, type Seg, type TermGame } from "@/games/term";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Zenith Games – retro játékkonzol" },
      { name: "description", content: "Zenith Bird, Dino, Pong, Space és Jump eredeti terminál grafikával, koppintással és húzással." },
      { property: "og:title", content: "Zenith Games – retro játékkonzol" },
      { property: "og:description", content: "Öt retro játék eredeti figurákkal és funkciókkal, Game Boy-os konzolon." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Index,
});

const hsKey = (id: string) => `zenith_${id}_hs`;
const optKey = (id: string, k: string) => `zenith_${id}_${k}`;

function Index() {
  const [selected, setSelected] = useState<number | null>(null);
  const def = selected === null ? undefined : GAMES[selected];
  return (
    <main className="console-page font-term">
      <h1 className="console-title">ZENITH GAMES</h1>
      <div className="handheld">
        <div className="handheld-screen">
          {def ? <Player key={def.id} def={def} onExit={() => setSelected(null)} /> : <Menu onPick={setSelected} />}
        </div>
        <div className="handheld-controls" aria-hidden="true">
          <span className="handheld-dpad">▲<br />◀ ✚ ▶<br />▼</span>
          <span className="handheld-select">▰ ▰</span>
          <span className="handheld-buttons">● ● ●</span>
        </div>
      </div>
      <p className="console-motto">Life is a game...</p>
    </main>
  );
}

function Menu({ onPick }: { onPick: (i: number) => void }) {
  const [best, setBest] = useState<Record<string, number>>({});
  useEffect(() => {
    const b: Record<string, number> = {};
    GAMES.forEach((g) => (b[g.id] = Number(localStorage.getItem(hsKey(g.id)) || 0)));
    setBest(b);
  }, []);
  return (
    <div className="game-menu">
      {GAMES.map((g, i) => (
        <Button key={g.id} variant="outline" onClick={() => onPick(i)} className={`game-menu-item game-ink-${g.id}`}>
          <span>{i + 1}) {g.name.replace("ZENITH ", "")}</span>
          <span className="game-best">BEST {best[g.id] ?? 0}</span>
        </Button>
      ))}
    </div>
  );
}

function Player({ def, onExit }: { def: GameDef; onExit: () => void }) {
  const ref = useRef<HTMLCanvasElement>(null);
  const game = useRef<TermGame | null>(null);
  const grid = useRef<Grid | null>(null);
  const [phase, setPhase] = useState<"setup" | "play" | "over">("setup");
  const [hud, setHud] = useState<Seg[]>([]);
  const [opts, setOpts] = useState<Record<string, string>>({});
  const [run, setRun] = useState(0);
  const phaseRef = useRef(phase);
  phaseRef.current = phase;

  useEffect(() => {
    const o: Record<string, string> = {};
    def.options.forEach((d) => (o[d.key] = localStorage.getItem(optKey(def.id, d.key)) || d.choices[0]?.value || ""));
    setOpts(o);
  }, [def]);

  // (Re)create the game whenever a run starts or options change on the setup screen.
  useEffect(() => {
    const best = Number(localStorage.getItem(hsKey(def.id)) || 0);
    const gm = def.make({ best, opts });
    game.current = gm;
    grid.current = new Grid(gm.W + 2, gm.H + 2);
    const canvas = ref.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    canvas.width = (gm.W + 2) * CW;
    canvas.height = (gm.H + 2) * CH;
    const cells = grid.current;
    let raf = 0, acc = 0, last = performance.now(), overAt = 0;
    const loop = (t: number) => {
      acc += Math.min(200, t - last);
      last = t;
      let d = gm.delay();
      while (acc >= d) { acc -= d; gm.tick(); d = gm.delay(); }
      if (gm.over && phaseRef.current === "play") {
        if (!overAt) {
          overAt = t;
          if (gm.score > best) localStorage.setItem(hsKey(def.id), String(gm.score));
        } else if (t - overAt > 800) setPhase("over");
      }
      gm.draw(cells);
      renderGrid(ctx, cells);
      setHud(gm.hud());
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(raf);
  }, [def, opts, run]);

  const lastX = useRef<number | null>(null);
  const colOf = (e: React.PointerEvent) => {
    const r = ref.current?.getBoundingClientRect();
    const gm = game.current;
    if (!r || !gm) return { col: 0, side: 1 as const };
    const cols = gm.W + 2;
    return { col: ((e.clientX - r.left) / r.width) * cols - 1, side: (e.clientX - r.left < r.width / 2 ? -1 : 1) as -1 | 1 };
  };
  const down = (e: React.PointerEvent) => {
    e.preventDefault();
    const gm = game.current;
    if (!gm) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    if (phase === "over") { setPhase("setup"); setRun((n) => n + 1); return; }
    if (phase === "setup") { setPhase("play"); gm.start(); return; }
    const { col, side } = colOf(e);
    lastX.current = col;
    gm.tap(Math.floor(col), side);
  };
  const move = (e: React.PointerEvent) => {
    if (phase !== "play" || lastX.current === null) return;
    const { col } = colOf(e);
    const dx = col - lastX.current;
    if (Math.abs(dx) >= 1) { game.current?.drag(dx); lastX.current = col; }
  };
  const up = () => { lastX.current = null; };

  const setOpt = (k: string, v: string) => {
    localStorage.setItem(optKey(def.id, k), v);
    setOpts((o) => ({ ...o, [k]: v }));
  };

  return (
    <div className="game-player select-none">
      <div className={`game-player-title game-ink-${def.id}`}>{def.name}</div>
      <canvas
        ref={ref}
        aria-label={def.name}
        onPointerDown={down}
        onPointerMove={move}
        onPointerUp={up}
        onPointerCancel={up}
        onPointerLeave={up}
        className="block w-full touch-none [image-rendering:pixelated]"
      />
      <div className="min-h-6 bg-background px-2 py-2 text-[10px] sm:text-xs">
        {hud.map((s, i) => <span key={i} style={{ color: s.c }}>{s.t}</span>)}
      </div>
      {phase === "setup" && def.options.length > 0 && (
        <div className="px-3 pt-2 flex flex-col gap-2 text-xs">
          {def.options.map((d) => (
            <div key={d.key} className="flex flex-wrap items-center gap-2">
              <span className="w-16 text-muted-foreground">{d.label}:</span>
              {d.choices.map((c) => (
                <Button
                  variant="outline"
                  size="sm"
                  key={c.value}
                  onClick={() => setOpt(d.key, c.value)}
                  className={`rounded border-2 px-2 py-1 ${opts[d.key] === c.value ? "border-primary" : "border-border"}`}
                  style={{ color: c.color }}
                >
                  {c.label}
                </Button>
              ))}
            </div>
          ))}
        </div>
      )}
      <div className="mt-auto flex items-center gap-3 px-3 py-3 text-xs">
        {phase === "over" && <span className="text-muted-foreground">GAME OVER</span>}
        <Button variant="outline" size="sm" onClick={onExit}>◀ MENÜ</Button>
      </div>
    </div>
  );
}
