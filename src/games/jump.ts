import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

export const JUMP_SKINS = ["#00ff00", "#ff5f00", "#ffff00", "#af5fff"];

export function jump({ best, opts }: GameOpts): TermGame {
  const PUSH = 3, FRICTION = 1, JP = -3, MAX_FALL = 2, GRAV_DELAY = 3, YELLOW_PROB = 6, PURPLE_PROB = 15;
  const W = 58, H = 19;
  const BG = "#262626", BORDER = "#af87ff", GLOW = "#d787ff", HUD = "#ffd7ff", WHITE = "#e5e5e5";
  const CYAN = "#00afff", YELLOW = "#ffff00", GREEN = "#00ff00", PURPLE = "#af5fff";
  const player = JUMP_SKINS[Number(opts['skin'] || 1) - 1] ?? GREEN;

  let px = Math.floor(W / 2), py = H - 3, vx = 0, vy = JP, gravTick = 0, platTick = 0;
  let groundY = H - 1, ground = true, push = 0;
  const pl = [
    { x: 6, y: 14, a: true, d: 1, max: -1, b: -1 },
    { x: 22, y: 10, a: true, d: -1, max: -1, b: -1 },
    { x: 38, y: 6, a: true, d: 1, max: -1, b: -1 },
    { x: 18, y: 2, a: true, d: 0, max: -1, b: -1 },
  ];

  return {
    W, H, started: false, over: false, score: 0,
    delay: () => 30,
    start() { this.started = true; },
    tap(_c, side) { push = side; },
    drag(dx) { if (Math.abs(dx) >= 1) push = dx < 0 ? -1 : 1; },
    tick() {
      if (!this.started || this.over) return;
      if (push) { vx = push * PUSH; push = 0; }
      px += vx;
      if (px < 3) { px = 3; vx = 0; }
      if (px > W - 3) { px = W - 3; vx = 0; }
      if (vx > 0) vx -= FRICTION;
      if (vx < 0) vx += FRICTION;
      if (++gravTick >= GRAV_DELAY) { vy = Math.min(MAX_FALL, vy + 1); gravTick = 0; }
      py += vy;
      if (vy > 0) {
        if (ground && py >= groundY) { py = groundY; vy = JP; }
        else for (const p of pl) {
          if (p.a && py >= p.y && py - vy <= p.y && px + 1 >= p.x && px - 3 <= p.x + 9) {
            py = p.y;
            if (p.max === -2) vy = JP * 3;
            else { vy = JP; if (p.max !== -1) { p.b--; if (p.b <= 0) p.a = false; } }
            break;
          }
        }
      }
      if (++platTick >= 3) {
        platTick = 0;
        for (const p of pl) if (p.a && p.d) {
          p.x += p.d;
          if (p.x < 2) { p.x = 2; p.d = 1; }
          if (p.x > W - 11) { p.x = W - 11; p.d = -1; }
        }
      }
      if (py < 7) {
        const shift = 7 - py; py = 7; this.score += shift;
        if (ground) { groundY += shift; if (groundY > H) ground = false; }
        for (const p of pl) {
          p.y += shift;
          if (p.y > H - 1) {
            p.y -= 16; p.x = rnd(W - 12) + 2; p.a = true; p.d = rnd(3) - 1;
            if (rnd(PURPLE_PROB) === 0) { p.max = -2; p.b = -1; }
            else if (rnd(YELLOW_PROB) === 0) { p.max = 2; p.b = 2; }
            else { p.max = -1; p.b = -1; }
          }
        }
      }
      if (py > H) this.over = true;
    },
    draw(g: Grid) {
      g.frame(W, H, BORDER, GLOW, BG);
      if (!this.started) {
        g.center(Math.floor(H / 2) - 2, "TAP TO JUMP START", WHITE, W);
        g.center(Math.floor(H / 2), "CONTROLS: TAP / SWIPE LEFT | RIGHT", HUD, W);
        g.inner(px - 3, H - 3, "▓▀█▀▓", player);
        g.inner(px - 3, H - 2, "▓███▓", player);
        return;
      }
      if (this.over) { g.center(Math.floor(H / 2), " GAME OVER ", WHITE, W); }
      for (let y = 1; y <= H; y++) {
        if (this.over && y === Math.floor(H / 2)) continue;
        if (ground && y === groundY) g.inner(0, y, "█".repeat(W), GREEN);
        else {
          const p = pl.find((q) => q.a && q.y === y);
          if (p) g.inner(p.x - 1, y, "█".repeat(10), p.max === -2 ? PURPLE : p.max !== -1 ? YELLOW : CYAN);
        }
        if (y === py - 1) g.inner(px - 3, y, "▓▀█▀▓", player);
        else if (y === py) g.inner(px - 3, y, "▓███▓", player);
      }
    },
    hud() {
      return [{ t: ` BEST:${Math.max(best, this.score)} | SCORE:${this.score}m | ZONE: ${this.score}m`, c: HUD }];
    },
  };
}
