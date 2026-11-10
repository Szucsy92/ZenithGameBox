import { type GameOpts, type Grid, type TermGame } from "./term";

const BG = "#14051f", BORDER = "#af87ff", GLOW = "#d787ff", BACK = "#5f2f9f", MATCH = "#d787ff", CUR = "#ffffff", HUD = "#ffd7ff";
const SYMBOLS = ["▲", "●", "♥", "♦", "♣", "♠", "★", "◆"];
const COLORS = ["#00ffff", "#ffff00", "#ff3355", "#55ff55", "#ff5f00", "#5fd7ff", "#ffd7ff", "#af87ff"];
const CW = 12, CHH = 4; // card cell size

export function memory({ best }: GameOpts): TermGame & { dir(dx: number, dy: number): void; press(): void; tapAt(col: number, row: number): void } {
  const W = 4 * (CW + 2) + 2, H = 4 * (CHH + 1) - 1;
  const deck = [...SYMBOLS.keys(), ...SYMBOLS.keys()];
  for (let i = deck.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [deck[i], deck[j]] = [deck[j]!, deck[i]!]; }
  const shown = deck.map(() => false), done = deck.map(() => false);
  let open: number[] = [], wait = 0, cur = 0, moves = 0;
  const flip = (game: TermGame, i: number) => {
    if (wait || done[i] || shown[i] || i < 0 || i > 15) return;
    shown[i] = true; open.push(i);
    if (open.length === 2) {
      moves++;
      const [a, b] = open as [number, number];
      if (deck[a] === deck[b]) { done[a] = done[b] = true; open = []; if (done.every(Boolean)) game.over = true; }
      else wait = 45;
    }
  };
  const x0 = (c: number) => 1 + c * (CW + 2), y0 = (r: number) => 1 + r * (CHH + 1);
  return {
    W, H, started: false, over: false,
    get score() { return Math.max(0, 100 - moves * 2); },
    set score(_v: number) {},
    delay: () => 16,
    start() { this.started = true; },
    dir(dx, dy) { cur = ((Math.floor(cur / 4) + dy + 4) % 4) * 4 + ((cur % 4) + dx + 4) % 4; },
    tap() {},
    press() { flip(this, cur); },
    tapAt(col, row) {
      const c = Math.floor((col - 1) / (CW + 2)), r = Math.floor((row - 1) / (CHH + 1));
      if (c >= 0 && c < 4 && r >= 0 && r < 4) { cur = r * 4 + c; flip(this, cur); }
    },
    drag() {},
    tick() {
      if (wait && --wait === 0) { for (const i of open) shown[i] = false; open = []; }
    },
    draw(g: Grid) {
      g.frame(W, H, BORDER, GLOW, BG);
      for (let i = 0; i < 16; i++) {
        const c = i % 4, r = Math.floor(i / 4), face = shown[i] || done[i];
        const col = done[i] ? MATCH : face ? COLORS[deck[i]!]! : BACK;
        for (let y = 0; y < CHH; y++) g.inner(x0(c), y0(r) + y, face ? "▒".repeat(CW) : "▓".repeat(CW), col);
        if (face) g.inner(x0(c) + CW / 2 - 1, y0(r) + 1, SYMBOLS[deck[i]!]! + " ", "#ffffff", "#000000");
        if (i === cur && this.started) {
          g.inner(x0(c) - 1, y0(r), "▶", CUR);
          g.inner(x0(c) + CW, y0(r) + CHH - 1, "◀", CUR);
        }
      }
      if (!this.started) g.center(Math.floor(H / 2), " TAP TO START ", "#ffffff", W);
      if (this.over) g.center(Math.floor(H / 2), ` KÉSZ! ${moves} LÉPÉS `, "#ffffff", W);
    },
    hud() { return [{ t: ` LÉPÉS: ${moves}   PONT: ${this.score}   BEST: ${Math.max(best, this.score)}`, c: HUD }]; },
  };
}
