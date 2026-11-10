import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

const BG = "#06140a", GRID = "#0f2a16", BORDER = "#55ff55", HEAD = "#aaffaa", BODY = "#33dd55", FOOD = "#ff3355", HUD = "#ccffcc";

export function snake({ best }: GameOpts): TermGame & { dir(dx: number, dy: number): void } {
  const W = 29, H = 19;
  let body = [{ x: 14, y: 10 }, { x: 13, y: 10 }, { x: 12, y: 10 }];
  let dx = 1, dy = 0, next = { dx: 1, dy: 0 };
  let food = { x: 20, y: 10 };
  const place = () => {
    do food = { x: rnd(W) + 1, y: rnd(H) + 1 }; while (body.some((p) => p.x === food.x && p.y === food.y));
  };
  const turn = (nx: number, ny: number) => { if (nx !== -dx || ny !== -dy) next = { dx: nx, dy: ny }; };
  return {
    W: W * 2, H, started: false, over: false, score: 0,
    delay() { return Math.max(70, 160 - this.score * 4); },
    start() { this.started = true; },
    dir(x, y) { turn(x, y); },
    tap() {},
    drag() {},
    tick() {
      if (!this.started || this.over) return;
      dx = next.dx; dy = next.dy;
      const head = body[0]!;
      const h = { x: head.x + dx, y: head.y + dy };
      if (h.x < 1 || h.x > W || h.y < 1 || h.y > H || body.some((p) => p.x === h.x && p.y === h.y)) {
        this.over = true;
        navigator.vibrate?.(120);
        return;
      }
      body.unshift(h);
      if (h.x === food.x && h.y === food.y) { this.score++; place(); } else body.pop();
    },
    draw(g: Grid) {
      g.frame(W * 2, H, BORDER, BORDER, BG);
      for (let y = 1; y <= H; y++) for (let x = 1; x <= W; x++) if ((x + y) % 2 === 0) g.inner((x - 1) * 2, y, "░░", GRID);
      const blink = Math.floor(performance.now() / 300) % 2 === 0;
      g.inner((food.x - 1) * 2, food.y, blink ? "██" : "▓▓", FOOD);
      body.forEach((p, i) => g.inner((p.x - 1) * 2, p.y, "██", i === 0 ? HEAD : BODY));
      if (!this.started) {
        g.center(5, "TAP TO START", "#ffffff", W * 2);
        g.center(7, "JOYSTICK: IRÁNY", HUD, W * 2);
      }
    },
    hud() {
      return [{ t: ` SCORE: ${this.score}   BEST: ${Math.max(best, this.score)}`, c: HUD }];
    },
  };
}
