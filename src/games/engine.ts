// Canvas games ported from zenith_games.sh. Logical size 360x480.
export const W = 360;
export const H = 480;

export const C = {
  bg: "#14101f",
  purple: "#a78bfa",
  green: "#22e55a",
  blue: "#38a8ff",
  yellow: "#ffe14a",
  red: "#ff3b4f",
  white: "#f4f1ff",
  cyan: "#3ef0ff",
  orange: "#ff7a1a",
  gray: "#5b5470",
};

export interface Game {
  score: number;
  over: boolean;
  update(dt: number): void;
  draw(g: CanvasRenderingContext2D): void;
  tap(x: number, y: number): void;
  drag(x: number, y: number): void;
}

const rand = (a: number, b: number) => a + Math.random() * (b - a);
const hit = (ax: number, ay: number, aw: number, ah: number, bx: number, by: number, bw: number, bh: number) =>
  ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;

function clear(g: CanvasRenderingContext2D) {
  g.fillStyle = C.bg;
  g.fillRect(0, 0, W, H);
}
function text(g: CanvasRenderingContext2D, s: string, x: number, y: number, col = C.white, size = 16, align: CanvasTextAlign = "left") {
  g.fillStyle = col;
  g.font = `${size}px "Silkscreen", monospace`;
  g.textAlign = align;
  g.fillText(s, x, y);
}

/* ---------- BIRD ---------- */
export function bird(): Game {
  let y = H / 2, v = 0, t = 0;
  const pipes: { x: number; gap: number; passed: boolean }[] = [];
  const GAP = 130;
  return {
    score: 0, over: false,
    update(dt) {
      if (this.over) return;
      v += 1400 * dt; y += v * dt; t += dt;
      const speed = 120 + Math.min(this.score, 40) * 4;
      if (t > 1.6) { t = 0; pipes.push({ x: W, gap: rand(80, H - 80 - GAP), passed: false }); }
      for (const p of pipes) {
        p.x -= speed * dt;
        if (!p.passed && p.x + 40 < 70) { p.passed = true; this.score++; }
        if (hit(70, y, 24, 18, p.x, 0, 40, p.gap) || hit(70, y, 24, 18, p.x, p.gap + GAP, 40, H)) this.over = true;
      }
      while (pipes.length && pipes[0]!.x < -50) pipes.shift();
      if (y > H - 18 || y < 0) this.over = true;
    },
    draw(g) {
      clear(g);
      for (const p of pipes) {
        g.fillStyle = C.green; g.fillRect(p.x, 0, 40, p.gap); g.fillRect(p.x, p.gap + GAP, 40, H);
        g.fillStyle = "#118a35"; g.fillRect(p.x - 4, p.gap - 12, 48, 12); g.fillRect(p.x - 4, p.gap + GAP, 48, 12);
      }
      g.fillStyle = C.yellow; g.fillRect(70, y, 24, 18);
      g.fillStyle = C.orange; g.fillRect(92, y + 7, 8, 6);
      g.fillStyle = C.bg; g.fillRect(84, y + 4, 4, 4);
    },
    tap() { v = -420; },
    drag() {},
  };
}

/* ---------- DINO ---------- */
export function dino(): Game {
  const GROUND = H - 80;
  let y = GROUND, v = 0, speed = 200, t = 0, next = 1.2;
  const obs: { x: number; w: number; h: number; passed: boolean }[] = [];
  return {
    score: 0, over: false,
    update(dt) {
      if (this.over) return;
      v += 1800 * dt; y = Math.min(GROUND, y + v * dt);
      t += dt;
      if (t > next) { t = 0; next = rand(0.8, 1.8) * (200 / speed) + 0.4; obs.push({ x: W, w: rand(14, 30), h: rand(28, 50), passed: false }); }
      for (const o of obs) {
        o.x -= speed * dt;
        if (!o.passed && o.x + o.w < 40) { o.passed = true; this.score++; if (this.score % 5 === 0) speed = Math.min(420, speed + 30); }
        if (hit(44, y - 40, 26, 40, o.x, GROUND - o.h, o.w, o.h)) this.over = true;
      }
      while (obs.length && obs[0]!.x < -40) obs.shift();
    },
    draw(g) {
      clear(g);
      g.fillStyle = C.gray; g.fillRect(0, GROUND, W, 2);
      g.fillStyle = C.purple; g.fillRect(44, y - 40, 22, 30); g.fillRect(56, y - 50, 22, 16); g.fillRect(48, y - 10, 6, 10); g.fillRect(60, y - 10, 6, 10);
      g.fillStyle = C.bg; g.fillRect(70, y - 46, 4, 4);
      g.fillStyle = C.green;
      for (const o of obs) { g.fillRect(o.x, GROUND - o.h, o.w, o.h); g.fillRect(o.x - 6, GROUND - o.h + 10, 6, 12); }
      text(g, `SPEED ${Math.round(speed / 100)}`, W - 12, 52, C.gray, 12, "right");
    },
    tap() { if (y >= GROUND) v = -640; },
    drag() {},
  };
}

/* ---------- PONG (vs CPU, score = your points) ---------- */
export function pong(): Game {
  const PW = 70;
  let px = W / 2 - PW / 2, cx = W / 2 - PW / 2;
  let bx = W / 2, by = H / 2, vx = 140, vy = 220, cpu = 0, lives = 3;
  const reset = (dir: number) => { bx = W / 2; by = H / 2; vx = rand(-160, 160); vy = 220 * dir; };
  return {
    score: 0, over: false,
    update(dt) {
      if (this.over) return;
      const s = 1 + this.score * 0.06;
      bx += vx * s * dt; by += vy * s * dt;
      if (bx < 6 || bx > W - 6) vx = -vx;
      cx += Math.sign(bx - (cx + PW / 2)) * Math.min(Math.abs(bx - (cx + PW / 2)), (200 + this.score * 10) * dt);
      if (vy > 0 && by > H - 34 && by < H - 20 && bx > px && bx < px + PW) { vy = -Math.abs(vy); vx = (bx - (px + PW / 2)) * 6; }
      if (vy < 0 && by < 34 && by > 20 && bx > cx && bx < cx + PW) { vy = Math.abs(vy); vx += rand(-60, 60); }
      if (by < 0) { this.score++; reset(1); }
      if (by > H) { cpu++; lives--; if (lives <= 0) this.over = true; else reset(-1); }
    },
    draw(g) {
      clear(g);
      g.fillStyle = C.gray; for (let x = 0; x < W; x += 16) g.fillRect(x, H / 2 - 1, 8, 2);
      g.fillStyle = C.red; g.fillRect(cx, 20, PW, 10);
      g.fillStyle = C.orange; g.fillRect(px, H - 30, PW, 10);
      g.fillStyle = C.white; g.fillRect(bx - 5, by - 5, 10, 10);
      text(g, `CPU ${cpu}`, 12, H / 2 - 12, C.red, 12);
      text(g, "♥".repeat(lives), W - 12, H / 2 + 22, C.orange, 14, "right");
    },
    tap(x) { px = Math.max(0, Math.min(W - PW, x - PW / 2)); },
    drag(x) { px = Math.max(0, Math.min(W - PW, x - PW / 2)); },
  };
}

/* ---------- SPACE INVADERS ---------- */
export function space(): Game {
  let sx = W / 2, cool = 0, dir = 1, wave = 1;
  let shots: { x: number; y: number }[] = [];
  let bombs: { x: number; y: number }[] = [];
  let enemies: { x: number; y: number; alive: boolean }[] = [];
  const spawn = () => {
    enemies = [];
    for (let r = 0; r < 4; r++) for (let c = 0; c < 7; c++) enemies.push({ x: 30 + c * 42, y: 50 + r * 34, alive: true });
  };
  spawn();
  return {
    score: 0, over: false,
    update(dt) {
      if (this.over) return;
      cool -= dt;
      if (cool <= 0) { shots.push({ x: sx, y: H - 60 }); cool = 0.35; }
      const alive = enemies.filter((e) => e.alive);
      const sp = (30 + wave * 10) * (1 + (28 - alive.length) * 0.05);
      let edge = false;
      for (const e of alive) { e.x += dir * sp * dt; if (e.x < 10 || e.x > W - 34) edge = true; }
      if (edge) { dir = -dir; for (const e of alive) e.y += 14; }
      if (Math.random() < dt * (0.8 + wave * 0.3) && alive.length) { const e = alive[Math.floor(Math.random() * alive.length)]!; bombs.push({ x: e.x + 12, y: e.y + 20 }); }
      for (const s of shots) s.y -= 420 * dt;
      for (const b of bombs) b.y += 200 * dt;
      for (const s of shots) for (const e of alive) if (e.alive && hit(s.x - 2, s.y, 4, 10, e.x, e.y, 26, 20)) { e.alive = false; s.y = -99; this.score++; }
      for (const b of bombs) if (hit(b.x - 3, b.y, 6, 10, sx - 16, H - 50, 32, 20)) this.over = true;
      for (const e of alive) if (e.y > H - 80) this.over = true;
      shots = shots.filter((s) => s.y > -20);
      bombs = bombs.filter((b) => b.y < H);
      if (!enemies.some((e) => e.alive)) { wave++; spawn(); }
    },
    draw(g) {
      clear(g);
      const cols = [C.red, C.yellow, C.green, C.blue];
      enemies.forEach((e, i) => {
        if (!e.alive) return;
        g.fillStyle = cols[Math.floor(i / 7)]!;
        g.fillRect(e.x + 4, e.y, 18, 14); g.fillRect(e.x, e.y + 6, 26, 6); g.fillRect(e.x + 2, e.y + 14, 4, 6); g.fillRect(e.x + 20, e.y + 14, 4, 6);
        g.fillStyle = C.bg; g.fillRect(e.x + 8, e.y + 4, 3, 3); g.fillRect(e.x + 15, e.y + 4, 3, 3);
      });
      g.fillStyle = C.cyan; g.fillRect(sx - 16, H - 40, 32, 10); g.fillRect(sx - 4, H - 52, 8, 12);
      g.fillStyle = C.white; for (const s of shots) g.fillRect(s.x - 2, s.y, 4, 10);
      g.fillStyle = C.red; for (const b of bombs) g.fillRect(b.x - 3, b.y, 6, 10);
      text(g, `WAVE ${wave}`, W - 12, 52, C.gray, 12, "right");
    },
    tap(x) { sx = Math.max(16, Math.min(W - 16, x)); },
    drag(x) { sx = Math.max(16, Math.min(W - 16, x)); },
  };
}

/* ---------- JUMP (doodle-style; yellow = spring, purple = breaks) ---------- */
export function jump(): Game {
  let x = W / 2, y = H - 100, vy = -600, target = x, height = 0;
  type P = { x: number; y: number; kind: "n" | "y" | "p"; broken: boolean };
  const plats: P[] = [];
  const make = (py: number): P => {
    const r = Math.random() * 100;
    return { x: rand(0, W - 60), y: py, kind: r < 6 ? "y" : r < 21 ? "p" : "n", broken: false };
  };
  plats.push({ x: W / 2 - 30, y: H - 40, kind: "n", broken: false });
  for (let py = H - 110; py > -40; py -= 70) plats.push(make(py));
  return {
    score: 0, over: false,
    update(dt) {
      if (this.over) return;
      x += (target - x) * Math.min(1, dt * 10);
      vy += 1300 * dt; y += vy * dt;
      if (vy > 0) for (const p of plats) {
        if (!p.broken && x + 12 > p.x && x - 12 < p.x + 60 && y + 20 > p.y && y + 20 < p.y + 14) {
          if (p.kind === "p") { p.broken = true; continue; }
          vy = p.kind === "y" ? -1050 : -640;
        }
      }
      if (y < H * 0.4) {
        const d = H * 0.4 - y; y = H * 0.4; height += d;
        for (const p of plats) p.y += d;
        this.score = Math.floor(height / 20);
      }
      while (plats.length && plats[0]!.y > H) plats.shift();
      let top = Math.min(...plats.map((p) => p.y));
      while (top > -40) { top -= rand(55, 85 + Math.min(40, this.score / 10)); plats.push(make(top)); }
      plats.sort((a, b) => b.y - a.y);
      if (y > H + 20) this.over = true;
    },
    draw(g) {
      clear(g);
      for (const p of plats) {
        if (p.broken) continue;
        g.fillStyle = p.kind === "y" ? C.yellow : p.kind === "p" ? C.purple : C.green;
        g.fillRect(p.x, p.y, 60, 10);
      }
      g.fillStyle = C.cyan; g.fillRect(x - 12, y - 10, 24, 30);
      g.fillStyle = C.bg; g.fillRect(x - 6, y - 4, 4, 4); g.fillRect(x + 2, y - 4, 4, 4);
    },
    tap(tx) { target = tx; },
    drag(tx) { target = tx; },
  };
}

export const GAMES = [
  { id: "bird", name: "ZENITH BIRD", color: C.yellow, make: bird, hint: "Koppints a repüléshez" },
  { id: "dino", name: "ZENITH DINO", color: C.purple, make: dino, hint: "Koppints az ugráshoz" },
  { id: "pong", name: "ZENITH PONG", color: C.orange, make: pong, hint: "Húzd az ütőt" },
  { id: "space", name: "ZENITH SPACE", color: C.cyan, make: space, hint: "Húzd a hajót – automatikusan lő" },
  { id: "jump", name: "ZENITH JUMP", color: C.green, make: jump, hint: "Húzd balra-jobbra" },
] as const;
