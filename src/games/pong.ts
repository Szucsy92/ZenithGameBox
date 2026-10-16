import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

export const PONG_BALLS = ["🟠", "🔵", "🟢", "🟣", "🟡", "🔴"];
export const PONG_PADS = ["#5fd7ff", "#ffff00", "#af87ff"];

export function pong({ best, opts }: GameOpts): TermGame {
  const W = 58, H = 19;
  const BG = "#1a0233", BORDER = "#af87ff", GLOW = "#d787ff", HUD = "#ffd7ff", WHITE = "#e5e5e5";
  const SPEED_START = 75, SPEED_ACCEL = 10, LEVEL_GAP = 10, MAX_LEVEL = 10, WALL_LIFE = 9;
  const BLK = { 1: "#ff00ff", 2: "#00afff", 3: "#ffff00", 4: "#00ff00", 5: "#ff0000" } as Record<number, string>;
  const BASE_PW = 16, PADDLE_Y = H, JUMPX = 6;
  const ball = opts['ball'] || PONG_BALLS[0]!;
  const pad = opts['pad'] || PONG_PADS[0]!;

  let PW = BASE_PW, PX = Math.floor(W / 2 - PW / 2);
  let speedOffset = 0, yellowEnd = 0, greenEnd = 0, redEnd = 0, timer = 0;
  let BX = Math.floor(W / 2), BY = Math.floor(H / 2), DX = rnd(2) ? 1 : -1, DY = -1;
  let wallX = 0, wallY = -1, wallType = 1, wallDie = 0, level = 1;
  let impulse = 0;
  const now = () => performance.now() / 1000;

  const clampPX = () => { PX = Math.max(1, Math.min(W - PW + 1, PX)); };

  return {
    W, H, started: false, over: false, score: 0,
    delay: () => 10,
    start() { this.started = true; },
    tap(_c, side) { PX += side * JUMPX; impulse = side; clampPX(); },
    drag(dx) { PX += Math.round(dx); if (dx) impulse = dx < 0 ? -1 : 1; clampPX(); },
    tick() {
      if (!this.started || this.over) return;
      const t = now();
      level = Math.max(1, Math.min(MAX_LEVEL, Math.floor(this.score / LEVEL_GAP) + 1 + speedOffset));
      const inc = SPEED_START + (level - 1) * SPEED_ACCEL;
      let mod = 0;
      if (t < greenEnd) mod += 4;
      if (t < redEnd) mod -= 5;
      PW = Math.max(4, BASE_PW + mod);
      clampPX();
      if (wallY !== -1 && t >= wallDie) wallY = -1;

      timer += inc;
      const imp = impulse; impulse = 0;
      if (timer < 240) return;
      timer -= 240;
      BX += DX; BY += DY;
      if (wallY === -1 && rnd(150) === 0) {
        wallY = rnd(8) + 3; wallX = rnd(W - 6) + 2; wallDie = t + WALL_LIFE;
        wallType = level >= 2 ? rnd(5) + 1 : [1, 3, 4, 5][rnd(4)]!;
      }
      if (BX <= 1) { DX = -DX; BX = 1; }
      if (BX >= W - 1) { DX = -DX; BX = W - 1; }
      if (BY <= 1) { DY = 1; BY = 1; }
      if (wallY !== -1 && BY === wallY && BX + 1 >= wallX && BX <= wallX + 3) {
        DY = -DY; wallY = -1;
        if (wallType === 1) this.score += 2;
        if (wallType === 2) speedOffset -= 2;
        if (wallType === 3) yellowEnd = t + 7;
        if (wallType === 4) greenEnd = t + 10;
        if (wallType === 5) redEnd = t + 5;
      }
      if (BY === PADDLE_Y - 1) {
        if (BX + 1 >= PX && BX < PX + PW) {
          const off = BX - PX, c = Math.floor(PW / 2);
          let curve = off < c - 5 ? -1 : off > c + 5 ? 1 : 0;
          if (off <= 1) curve = -2; else if (off >= PW - 2) curve = 2;
          DX += curve + imp;
          if (DX > 1 && rnd(4) === 0) DX--; else if (DX < -1 && rnd(4) === 0) DX++;
          DX = Math.max(-3, Math.min(3, DX));
          if (DX === 0) DX = rnd(2) ? 1 : -1;
          DY = -1; this.score++;
        } else if (t < yellowEnd) DY = -1;
      }
      if (BY >= H) { BY = H; this.over = true; }
    },
    draw(g: Grid) {
      g.frame(W, H, BORDER, GLOW, BG);
      const t = now();
      if (this.started && t < yellowEnd) g.inner(0, PADDLE_Y, "▒".repeat(W), BLK[3]!);
      if (wallY !== -1) g.inner(wallX - 1, wallY, "▓▓▓▓", BLK[wallType]!);
      g.inner(PX - 1, PADDLE_Y, "▓".repeat(PW), pad);
      g.inner(BX - 1, BY, ball, WHITE);
      if (!this.started) {
        g.center(Math.floor(H / 2), "TAP TO START", WHITE, W);
        g.center(Math.floor(H / 2) + 2, "DRAG / TAP: MOVE (LEFT SIDE / RIGHT SIDE)", HUD, W);
      }
      if (this.over) g.center(Math.floor(H / 2), " GAME OVER ", WHITE, W);
    },
    hud() {
      const t = now();
      const bar = Array.from({ length: MAX_LEVEL }, (_, i) => (i < level ? "█" : "░")).join("");
      const s: { t: string; c?: string }[] = [{ t: ` BEST:${Math.max(best, this.score)} | SCORE:${this.score} | LVL:${level}/${MAX_LEVEL} [${bar}]`, c: HUD }];
      if (t < yellowEnd) s.push({ t: `  SHIELD:${Math.ceil(yellowEnd - t)}s`, c: BLK[3]! });
      if (t < greenEnd) s.push({ t: `  WIDE:${Math.ceil(greenEnd - t)}s`, c: BLK[4]! });
      if (t < redEnd) s.push({ t: `  NARROW:${Math.ceil(redEnd - t)}s`, c: BLK[5]! });
      return s;
    },
  };
}
