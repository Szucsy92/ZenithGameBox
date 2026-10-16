import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { GAMES, type GameDef } from "@/games/engine";
import { CH, CW, Grid, renderGrid, type Seg, type TermGame } from "@/games/term";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Zenith Games – retro terminál játékkonzol" },
      { name: "description", content: "Zenith Bird, Dino, Pong, Space és Jump eredeti terminál grafikával, koppintással és húzással." },
      { property: "og:title", content: "Zenith Games – retro terminál játékkonzol" },
      { property: "og:description", content: "Öt terminál-játék eredeti shell grafikával, érintéses irányítással." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Index,
});

const hsKey = (id: string) => `zenith_${id}_hs`;
const optKey = (id: string, k: string) => `zenith_${id}_${k}`;

const P = "#af87ff", G = "#00ff00", B = "#00afff", Y = "#ffff00", R = "#ff0000", Wt = "#ffffff", C = "#00ffff";
type Span = [string, string?];
const consoleArt = (o: string): Span[][] => [
  [["                ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄", o]],
  [["           ▄▄▀▀▀                        ▀▀▀▄▄", o]],
  [["        ▄▀▀", o], ["   "], ["▲", G], ["         "], ["▰  ▰", Wt], ["         "], ["●", Y], ["      "], ["▀▀▄", o]],
  [["      ▄▀", o], ["    "], ["◀   ▶", G], ["                  "], ["●", R], ["   "], ["●", B], ["    "], ["▀▄", o]],
  [["    ▄▀", o], ["        "], ["▼", G], ["                      "], ["●", G], ["            "], ["▀▄", o]],
  [["   █", o], ["          "], ["◉", C], ["                      "], ["◉", C], ["          "], ["█", o]],
  [["    ▀▄", o], ["          "], ["▄▀▀", o], ["                      "], ["▀▀▄", o], ["           "], ["▀▄", o]],
  [["      ▀▄", o], ["      "], ["▄▀", o], ["                            "], ["▀▄", o], ["     "], ["▄▀", o]],
  [["        ▀▄▄▄▄▀", o], ["                                "], ["▀▄▄▄▄▀", o]],
];

function Art({ outline }: { outline: string }) {
  return (
    <pre className="overflow-hidden text-[2.4vw] leading-tight sm:text-sm">
      {consoleArt(outline).map((line, i) => (
        <div key={i}>
          {line.map(([t, c], j) => (
            <span key={j} style={{ color: c }}>{t}</span>
          ))}
        </div>
      ))}
    </pre>
  );
}

function Index() {
  const [screen, setScreen] = useState<"boot" | "menu" | number>("boot");
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-background px-2 py-6 font-term text-foreground">
      <div className="w-full max-w-[760px]">
        {screen === "boot" && <Boot onDone={() => setScreen("menu")} />}
        {screen === "menu" && <Menu onPick={(i) => setScreen(i)} />}
        {typeof screen === "number" && <Player key={screen} def={GAMES[screen]!} onExit={() => setScreen("menu")} />}
      </div>
    </main>
  );
}

function Boot({ onDone }: { onDone: () => void }) {
  const [frame, setFrame] = useState(0);
  const [typed, setTyped] = useState("");
  useEffect(() => {
    const msg = "Life is a game...";
    const timers: number[] = [];
    [0, 1, 2].forEach((f) => timers.push(window.setTimeout(() => setFrame(f), f * 120)));
    for (let i = 1; i <= msg.length; i++) timers.push(window.setTimeout(() => setTyped(msg.slice(0, i)), 400 + i * 45));
    timers.push(window.setTimeout(onDone, 400 + msg.length * 45 + 700));
    return () => timers.forEach(clearTimeout);
  }, [onDone]);
  return (
    <button onClick={onDone} className="block w-full text-left">
      <Art outline={frame === 1 ? R : P} />
      <p className="mt-6 pl-[18%] text-lg" style={{ color: Y }}>{typed}</p>
    </button>
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
    <div className="mx-auto max-w-md">
      <div style={{ color: P }}>{"═".repeat(30)}</div>
      <div className="py-1 text-lg" style={{ color: P }}>  ZENITH GAMES 🎮</div>
      <div style={{ color: P }}>{"═".repeat(30)}</div>
      <div className="mt-4 flex flex-col gap-1">
        {GAMES.map((g, i) => (
          <button key={g.id} onClick={() => onPick(i)} className="flex justify-between rounded px-2 py-3 text-left text-lg hover:bg-secondary active:bg-secondary">
            <span><span style={{ color: g.color }}>{i + 1})</span> {g.name}</span>
            <span style={{ color: Y }}>[BEST: {best[g.id] ?? 0}]</span>
          </button>
        ))}
      </div>
      <p className="mt-6 text-sm text-muted-foreground">Koppints egy játékra a kezdéshez.</p>
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
    def.options.forEach((d) => (o[d.key] = localStorage.getItem(optKey(def.id, d.key)) || d.choices[0]!.value));
    setOpts(o);
  }, [def]);

  // (Re)create the game whenever a run starts or options change on the setup screen.
  useEffect(() => {
    const best = Number(localStorage.getItem(hsKey(def.id)) || 0);
    const gm = def.make({ best, opts });
    game.current = gm;
    grid.current = new Grid(gm.W + 2, gm.H + 2);
    const ctx = ref.current!.getContext("2d")!;
    ref.current!.width = (gm.W + 2) * CW;
    ref.current!.height = (gm.H + 2) * CH;
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
      gm.draw(grid.current!);
      renderGrid(ctx, grid.current!);
      setHud(gm.hud());
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(raf);
  }, [def, opts, run]);

  const lastX = useRef<number | null>(null);
  const colOf = (e: React.PointerEvent) => {
    const r = ref.current!.getBoundingClientRect();
    const cols = game.current!.W + 2;
    return { col: ((e.clientX - r.left) / r.width) * cols - 1, side: (e.clientX - r.left < r.width / 2 ? -1 : 1) as -1 | 1 };
  };
  const down = (e: React.PointerEvent) => {
    e.preventDefault();
    const gm = game.current!;
    if (phase === "over") { setPhase("play"); setRun((n) => n + 1); return; }
    if (phase === "setup") { setPhase("play"); gm.start(); return; }
    const { col, side } = colOf(e);
    lastX.current = col;
    gm.tap(Math.floor(col), side);
  };
  const move = (e: React.PointerEvent) => {
    if (phase !== "play" || lastX.current === null) return;
    const { col } = colOf(e);
    const dx = col - lastX.current;
    if (Math.abs(dx) >= 1) { game.current!.drag(dx); lastX.current = col; }
  };
  const up = () => { lastX.current = null; };

  const setOpt = (k: string, v: string) => {
    localStorage.setItem(optKey(def.id, k), v);
    setOpts((o) => ({ ...o, [k]: v }));
  };

  return (
    <div className="select-none">
      <canvas
        ref={ref}
        onPointerDown={down}
        onPointerMove={move}
        onPointerUp={up}
        onPointerCancel={up}
        onPointerLeave={up}
        className="block w-full touch-none [image-rendering:pixelated]"
      />
      <div className="min-h-6 bg-background py-1 text-xs sm:text-sm">
        {hud.map((s, i) => <span key={i} style={{ color: s.c }}>{s.t}</span>)}
      </div>
      {phase === "setup" && def.options.length > 0 && (
        <div className="mt-2 flex flex-col gap-2 text-sm">
          {def.options.map((d) => (
            <div key={d.key} className="flex flex-wrap items-center gap-2">
              <span className="w-24 text-muted-foreground">{d.label}:</span>
              {d.choices.map((c) => (
                <button
                  key={c.value}
                  onClick={() => setOpt(d.key, c.value)}
                  className={`rounded border-2 px-2 py-1 ${opts[d.key] === c.value ? "border-primary" : "border-border"}`}
                  style={{ color: c.color }}
                >
                  {c.label}
                </button>
              ))}
            </div>
          ))}
        </div>
      )}
      <div className="mt-3 flex items-center gap-4 text-sm">
        {phase === "over" && <span>RETRY? (Tap) / </span>}
        <button onClick={onExit} className="rounded border-2 border-border px-3 py-1 hover:border-primary">EXIT</button>
      </div>
    </div>
  );
}
