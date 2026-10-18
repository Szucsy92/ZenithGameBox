import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

export function dino({ best }: GameOpts): TermGame {
  const W = 58, H = 19, GROUND = H - 2, DINO_X = 8, JUMP = -0.65, MAX_FALL = 0.6;
  const BG = "#eeeeee", BORDER = "#585858", CLOUD = "#bcbcbc";
  const clouds = [[4, 1], [27, 5], [50, 3]] as const;
  let dinoY = GROUND, vel = 0, cactusX = W, cactusH = 3, speed = 0.4, jump = false;
  return {
    W, H, started: false, over: false, score: 0,
    delay() { return 16; },
    start() { this.started = true; vel = JUMP; },
    tap() { jump = true; },
    drag(dx) { if (Math.abs(dx) >= 0.5) jump = true; },
    tick() {
      if (!this.started || this.over) return;
      if (jump) { if (dinoY >= GROUND) vel = JUMP; jump = false; }
      vel = Math.min(MAX_FALL, vel + 0.026);
      dinoY = Math.min(GROUND, dinoY + vel);
      cactusX -= speed;
      if (cactusX < -3) {
        cactusX = W; cactusH = rnd(2) + 2; this.score++;
        if (this.score % 5 === 0) speed = Math.min(0.9, speed + 0.075);
      }
      if (DINO_X + 4 >= cactusX && DINO_X <= cactusX + 2 && dinoY >= GROUND - cactusH + 1) this.over = true;
    },
    draw(g: Grid) {
      g.frame(W, H, BORDER, BORDER, BG);
      if (!this.started) { g.center(Math.floor(H / 2), " TAP TO START ", "#000000", W); return; }
      for (const [cx, cy] of clouds) { g.inner(cx - 1, cy, " ▄▄▄ ", CLOUD); g.inner(cx - 1, cy + 1, "█████", CLOUD); }
      g.inner(0, GROUND + 1, "━".repeat(W), "#808080");
      if (cactusX >= 1 && cactusX <= W - 3)
        for (let y = GROUND - cactusH + 1; y <= GROUND; y++) g.inner(cactusX - 1, y, "█▄█", "#00aa00");
      g.inner(DINO_X - 1, dinoY - 1, "▓▀█▀▓", "#af87ff");
      g.inner(DINO_X - 1, dinoY, "▓███▓", "#af5fff");
    },
    hud() {
      return [{ t: ` HI: ${Math.max(best, this.score)} | SCORE: ${this.score} | SPEED: ${Math.round(speed / 0.4 * 10) / 10} `, c: "#ffffff" }];
    },
  };
}
