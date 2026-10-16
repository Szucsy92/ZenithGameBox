import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { GAMES, H, W, C, type Game } from "@/games/engine";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Zenith Games – retro játékkonzol" },
      { name: "description", content: "Öt retro játék egy ablakban: Bird, Dino, Pong, Space és Jump – koppintással és húzással." },
      { property: "og:title", content: "Zenith Games – retro játékkonzol" },
      { property: "og:description", content: "Öt retro játék koppintással és húzással játszható." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: Index,
});

const hsKey = (id: string) => `zenith_${id}_hs`;

function Index() {
  const [sel, setSel] = useState<number | null>(null);
  const [best, setBest] = useState<Record<string, number>>({});
  useEffect(() => {
    const b: Record<string, number> = {};
    GAMES.forEach((g) => (b[g.id] = Number(localStorage.getItem(hsKey(g.id)) || 0)));
    setBest(b);
  }, [sel]);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 bg-background px-4 py-8 font-pixel text-foreground">
      <h1 className="text-center text-2xl text-primary sm:text-3xl">ZENITH GAMES</h1>
      <div className="w-full max-w-[400px] rounded-[2.5rem] border-4 border-primary bg-card p-4 shadow-[0_0_40px_-8px_var(--primary)]">
        <div className="overflow-hidden rounded-2xl border-2 border-border">
          {sel === null ? (
            <Menu best={best} onPick={setSel} />
          ) : (
            <Player key={sel} index={sel} onExit={() => setSel(null)} />
          )}
        </div>
        <div className="mt-4 flex items-center justify-between px-3 text-xs text-muted-foreground">
          <span>▲◀▶▼</span>
          <span>▰ ▰</span>
          <span>● ● ●</span>
        </div>
      </div>
      <p className="text-center text-xs text-muted-foreground">Life is a game...</p>
    </main>
  );
}

function Menu({ best, onPick }: { best: Record<string, number>; onPick: (i: number) => void }) {
  return (
    <div className="flex aspect-[3/4] flex-col justify-center gap-3 p-5" style={{ background: C.bg }}>
      {GAMES.map((g, i) => (
        <button
          key={g.id}
          onClick={() => onPick(i)}
          className="flex items-center justify-between rounded-lg border-2 border-border px-4 py-3 text-left transition active:scale-95 hover:border-primary"
        >
          <span style={{ color: g.color }}>{i + 1}) {g.name}</span>
          <span className="text-xs text-muted-foreground">BEST {best[g.id] ?? 0}</span>
        </button>
      ))}
    </div>
  );
}

function Player({ index, onExit }: { index: number; onExit: () => void }) {
  const def = GAMES[index];
  const ref = useRef<HTMLCanvasElement>(null);
  const game = useRef<Game | null>(null);
  const [state, setState] = useState<"ready" | "play" | "over">("ready");
  const [score, setScore] = useState(0);
  const stateRef = useRef(state);
  stateRef.current = state;
  const hs = useRef(0);

  useEffect(() => {
    hs.current = Number(localStorage.getItem(hsKey(def.id)) || 0);
    game.current = def.make();
    const ctx = ref.current!.getContext("2d")!;
    let last = performance.now(), raf = 0;
    const loop = (t: number) => {
      const dt = Math.min(0.05, (t - last) / 1000);
      last = t;
      const gm = game.current!;
      if (stateRef.current === "play") {
        gm.update(dt);
        setScore(gm.score);
        if (gm.over) {
          if (gm.score > hs.current) { hs.current = gm.score; localStorage.setItem(hsKey(def.id), String(gm.score)); }
          setState("over");
        }
      }
      gm.draw(ctx);
      raf = requestAnimationFrame(loop);
    };
    raf = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(raf);
  }, [def]);

  const pos = (e: React.PointerEvent) => {
    const r = ref.current!.getBoundingClientRect();
    return [((e.clientX - r.left) / r.width) * W, ((e.clientY - r.top) / r.height) * H] as const;
  };
  const down = (e: React.PointerEvent) => {
    e.preventDefault();
    if (state !== "play") {
      game.current = def.make();
      setScore(0);
      setState("play");
      return;
    }
    const [x, y] = pos(e);
    game.current!.tap(x, y);
  };
  const move = (e: React.PointerEvent) => {
    if (state !== "play" || (e.pointerType !== "mouse" && e.buttons === 0)) return;
    const [x, y] = pos(e);
    game.current!.drag(x, y);
  };

  return (
    <div className="relative select-none" style={{ background: C.bg }}>
      <canvas
        ref={ref}
        width={W}
        height={H}
        onPointerDown={down}
        onPointerMove={move}
        className="block aspect-[3/4] w-full touch-none"
      />
      <div className="pointer-events-none absolute inset-x-0 top-0 flex justify-between p-3 text-xs">
        <span style={{ color: def.color }}>{def.name}</span>
        <span className="text-foreground">{score} · HI {Math.max(hs.current, score)}</span>
      </div>
      <button
        onClick={onExit}
        className="absolute bottom-3 left-3 rounded-md border-2 border-border bg-card px-3 py-1 text-xs text-muted-foreground hover:text-foreground"
      >
        ◀ MENÜ
      </button>
      {state !== "play" && (
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-3 bg-background/70 text-center">
          <p className="text-xl" style={{ color: state === "over" ? C.red : def.color }}>
            {state === "over" ? "GAME OVER" : def.name}
          </p>
          {state === "over" && <p className="text-sm">PONT: {score}</p>}
          <p className="px-6 text-xs text-muted-foreground">{def.hint}</p>
          <p className="animate-pulse text-sm text-accent-foreground">KOPPINTS A KEZDÉSHEZ</p>
        </div>
      )}
    </div>
  );
}
