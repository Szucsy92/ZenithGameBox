import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

const YELLOW = "#ffff00", RED = "#ff0000", WHITE = "#ffffff", ORANGE = "#ff5f00";
const LIGHT_GREEN = "#55ff55", CYAN = "#00ffff";

export function bird({ best }: GameOpts): TermGame {
  const W = 58, H = 19, GAP = 6, BIRD_X = 10, FLAP = -0.5;
  let birdY = Math.floor(H / 2), vel = 0, pipeX = W, gapY = 4, flap = false;
  return {
    W, H, started: false, over: false, score: 0,
    delay() { return 16; },
    start() { this.started = true; vel = FLAP; },
    tap() { flap = true; },
    drag(dx) { if (Math.abs(dx) >= 0.5) flap = true; },
    tick() {
      if (!this.started || this.over) return;
      if (flap) { vel = FLAP; flap = false; }
      vel = Math.min(0.48, vel + 0.026);
      birdY += vel;
      pipeX -= Math.min(0.7, 0.28 + this.score * 0.014);
      if (pipeX < 1) { pipeX = W; gapY = rnd(H - GAP - 2) + 2; this.score++; }
      if (birdY < 1) { birdY = 1; this.over = true; }
      if (birdY > H) { birdY = H; this.over = true; }
      if (!this.over && BIRD_X + 2 >= pipeX && BIRD_X <= pipeX + 3 && (birdY < gapY || birdY >= gapY + GAP)) this.over = true;
    },
    draw(g: Grid) {
      const s = this.score;
      const bg = s < 20 ? "#5f00af" : s < 40 ? "#00005f" : "#5f0000";
      const wall = s < 20 ? LIGHT_GREEN : s < 40 ? CYAN : RED;
      g.frame(W, H, ORANGE, ORANGE, bg);
      if (!this.started) {
        g.center(birdY, "TAP TO START", WHITE, W);
        g.inner(BIRD_X - 1, birdY, "╣█╠", YELLOW);
        return;
      }
      for (let y = 1; y <= H; y++) {
        if (pipeX >= 1 && pipeX <= W && (y < gapY || y >= gapY + GAP)) g.inner(pipeX - 1, y, "▓▒▒▓", wall);
      }
      const b = BIRD_X - 1;
      if (vel < 0) { g.inner(b, birdY, "╣", YELLOW); g.inner(b + 1, birdY, "▼", RED); g.inner(b + 2, birdY, "╠", YELLOW); }
      else g.inner(b, birdY, vel > 0 ? "╣█╠" : "╣▓╠", YELLOW);
    },
    hud() {
      return [{ t: " SCORE: ", c: WHITE }, { t: String(this.score), c: CYAN }, { t: "   BEST: ", c: WHITE }, { t: String(Math.max(best, this.score)), c: YELLOW }];
    },
  };
}
