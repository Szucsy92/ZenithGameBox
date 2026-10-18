import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

export const JUMP_SKINS = ["#00ff00", "#ff5f00", "#ffff00", "#af5fff"];

export function jump({ best, opts }: GameOpts): TermGame {
  const JP = -0.65, MAX_FALL = 0.65, YELLOW_PROB = 6, PURPLE_PROB = 15;
  const W = 58, H = 19;
  const BG = "#262626", BORDER = "#af87ff", GLOW = "#d787ff", HUD = "#ffd7ff", WHITE = "#e5e5e5";
  const CYAN = "#00afff", YELLOW = "#ffff00", GREEN = "#00ff00", PURPLE = "#af5fff";
  const player = JUMP_SKINS[Number(opts['skin'] || 1) - 1] ?? GREEN;

  let px = Math.floor(W / 2), py = H - 3, vy = JP, target = px;
  let groundY = H - 1, ground = true;
  const pl = [
    { x: 6, y: 14, a: true, d: 1, max: -1, b: -1 },
    { x: 22, y: 10, a: true, d: -1, max: -1, b: -1 },
    { x: 38, y: 6, a: true, d: 1, max: -1, b: -1 },
    { x: 18, y: 2, a: true, d: 0, max: -1, b: -1 },
  ];

  return {
    W, H, started: false, over: false, score: 0,
    delay: () => 16,
    start() { this.started = true; },
    tap(col) { target = Math.max(3, Math.min(W - 3, col)); },
    drag(dx) { target = Math.max(3, Math.min(W - 3, target + dx)); },
    tick() {
      if (!this.started || this.over) return;
      px += (target - px) * 0.16;
      vy = Math.min(MAX_FALL, vy + 0.026);
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
      for (const p of pl) if (p.a && p.d) {
        p.x += p.d * 0.09;
        if (p.x < 2) { p.x = 2; p.d = 1; }
        if (p.x > W - 11) { p.x = W - 11; p.d = -1; }
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
      if (ground) g.inner(0, groundY, "█".repeat(W), GREEN);
      for (const p of pl) if (p.a) g.inner(p.x - 1, p.y, "█".repeat(10), p.max === -2 ? PURPLE : p.max !== -1 ? YELLOW : CYAN);
      g.inner(px - 3, py - 1, "▓▀█▀▓", player);
      g.inner(px - 3, py, "▓███▓", player);
    },
    hud() {
      return [{ t: ` BEST:${Math.max(best, Math.floor(this.score))} | SCORE:${Math.floor(this.score)}m | ZONE: ${Math.floor(this.score)}m`, c: HUD }];
    },
  };
}
