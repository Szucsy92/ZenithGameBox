import { rnd, type GameOpts, type Grid, type TermGame } from "./term";

export const SPACE_BULLETS = [
  { label: "YELLOW", c: "#ffff00" },
  { label: "RED", c: "#ff0000" },
  { label: "BLUE", c: "#00afff" },
  { label: "GREEN", c: "#00ff00" },
];
export const SPACE_SHIPS = ["╞═❯✦❮═╡", "«═╦╩╦═»", "▄█▀█▀█▄", "╪═▶█◀═╪"];

const START_X = [4, 12, 20, 28, 36, 44];
const ENEMY = ["👾", "🛸", "👽"];

export function space({ best, opts }: GameOpts): TermGame {
  const W = 56, H = 19;
  const BG = "#0e011c", BORDER = "#af87ff", GLOW = "#d787ff", WHITE = "#e5e5e5", PURPLE = "#af87ff";
  const YELLOW = "#ffff00", RED = "#ff0000", HUD = "#ffd7ff";
  const bulletC = SPACE_BULLETS.find((b) => b.label === opts.bullet)?.c ?? YELLOW;
  const ship = opts.ship || SPACE_SHIPS[0]!;

  let px = 24;
  const bullets = Array.from({ length: 6 }, () => ({ a: false, x: 0, y: 0 }));
  let ex: number[] = [], ey: number[] = [], ea: boolean[] = [], et: number[] = Array(18).fill(0);
  const resetEnemies = () => {
    ex = [...START_X, ...START_X, ...START_X];
    ey = [...Array(6).fill(2), ...Array(6).fill(4), ...Array(6).fill(6)];
    ea = Array(18).fill(true);
  };
  resetEnemies();
  let dir = 1, moveTick = 0, espeed = 6, wave = 1;
  let pwActive = false, pwX = 0, pwY = 0, pwType = 2, pwTick = 0, carrier = -1, mode = 1, pwExpiry = 0;
  let fire = false;
  const now = () => performance.now() / 1000;

  const shoot = (x: number) => { const b = bullets.find((b) => !b.a); if (b) { b.a = true; b.x = x; b.y = 17; } };

  return {
    W, H, started: false, over: false, score: 0,
    delay: () => 15,
    start() { this.started = true; },
    tap() { fire = true; },
    drag(dx) { px = Math.max(0, Math.min(49, px + Math.round(dx))); },
    tick() {
      if (!this.started || this.over) return;
      const t = now();
      if (fire) {
        fire = false;
        if (mode === 1) shoot(px + 3);
        if (mode === 2) { shoot(px + 2); shoot(px + 4); }
        if (mode === 3) { shoot(px + 1); shoot(px + 3); shoot(px + 5); }
      }
      // core: powerups + bullets
      if (mode > 1 && t >= pwExpiry) mode = 1;
      if (pwActive) {
        if (++pwTick > 2) { pwTick = 0; pwY++; if (pwY > 18) pwActive = false; }
        if (pwActive && pwY === 18 && pwX >= px && pwX <= px + 6) { pwActive = false; mode = pwType; pwExpiry = t + 10; }
      }
      bullets.forEach((b) => {
        if (!b.a) return;
        b.y--;
        if (b.y < 2) { b.a = false; return; }
        for (let i = 0; i < 18; i++) {
          if (ea[i] && b.y === ey[i] && b.x >= ex[i]! && b.x <= ex[i]! + 1) {
            ea[i] = false; b.a = false; this.score = Math.min(99999, this.score + 10);
            if (i === carrier) { pwActive = true; pwX = ex[i]!; pwY = b.y; pwTick = 0; carrier = -1; }
            break;
          }
        }
      });
      // enemies
      if (++moveTick >= espeed) {
        moveTick = 0;
        let shift = false;
        for (let i = 0; i < 18; i++) if (ea[i] && ((dir === 1 && ex[i]! >= 53) || (dir === -1 && ex[i]! <= 0))) shift = true;
        if (shift) {
          dir = -dir;
          for (let i = 0; i < 18; i++) { ey[i]!++; if (ea[i] && ey[i]! >= 17) this.over = true; }
        } else for (let i = 0; i < 18; i++) ex[i]! += dir;
      }
      if (!ea.some(Boolean)) {
        wave++;
        const wt = rnd(3);
        resetEnemies(); et = Array(18).fill(wt);
        dir = 1; pwActive = false;
        if (wave === 4) carrier = -1;
        else { carrier = rnd(18); pwType = mode === 1 ? 2 : 3; }
        if (espeed > 1) espeed--;
      }
    },
    draw(g: Grid) {
      g.frame(W, H, BORDER, GLOW, BG);
      if (!this.started) {
        g.center(5, "TAP TO START", WHITE, W);
        g.center(7, "DRAG: MOVE (LEFT / RIGHT)", HUD, W);
        g.center(9, "TAP: SHOOT  |  CATCH THE DROPPED 🔶 OR ⭐", "#00afff", W);
        g.center(12, "[ ARSENAL SETUP ]", YELLOW, W);
        const lbl = SPACE_BULLETS.find((b) => b.c === bulletC)!.label;
        g.inner(5, 13, "BULLET: ", WHITE); g.inner(13, 13, lbl, bulletC); g.inner(20, 13, " | SHIP: ", WHITE); g.inner(29, 13, ship, PURPLE);
        return;
      }
      if (this.over) { g.center(Math.floor(H / 2), " GAME OVER ", RED, W); return; }
      if (pwActive) g.inner(pwX, pwY, pwType === 2 ? "🔶" : "⭐", YELLOW);
      const r3 = wave % 2 === 1 ? "#00ff00" : "#00afff";
      for (let i = 0; i < 18; i++) if (ea[i]) g.inner(ex[i]!, ey[i]!, ENEMY[et[i]!]!, i < 6 ? PURPLE : i < 12 ? YELLOW : r3);
      for (const b of bullets) if (b.a) g.inner(b.x, b.y, "|", bulletC);
      g.inner(px, 18, ship, PURPLE);
    },
    hud() {
      const t = now();
      const m = mode === 1 ? { t: "NORMAL 🔫", c: HUD } : { t: `${mode === 2 ? "DOUBLE" : "TRIPLE"} ${Math.max(0, Math.ceil(pwExpiry - t))}s`, c: mode === 2 ? YELLOW : RED };
      return [{ t: ` 👾  BEST:${Math.max(best, this.score)} | SCORE:${this.score} | WAVE:${wave} | WEAPON:`, c: HUD }, m];
    },
  };
}
