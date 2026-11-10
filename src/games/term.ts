// Terminal-cell renderer that reproduces the original shell (ANSI) graphics on a canvas.

export type Cell = { ch: string; fg: string; bg: string; wide?: boolean; ox?: number; oy?: number };
export type Seg = { t: string; c?: string };

export class Grid {
  cells: Cell[][];
  constructor(public cols: number, public rows: number, bg = "#000") {
    this.cells = Array.from({ length: rows }, () =>
      Array.from({ length: cols }, () => ({ ch: " ", fg: "#fff", bg })),
    );
  }
  fill(bg: string) {
    for (const r of this.cells) for (const c of r) { c.ch = " "; c.bg = bg; c.wide = false; c.ox = 0; c.oy = 0; }
  }
  /** Write text at grid x,y. Emoji/wide glyphs take two cells. */
  put(x: number, y: number, str: string, fg: string, bg?: string, minX = 0, maxX = this.cols - 1) {
    const ox = x - Math.round(x), oy = y - Math.round(y);
    x = Math.round(x); y = Math.round(y);
    const row = this.cells[y];
    if (!row) return;
    for (const ch of Array.from(str)) {
      const wide = isWide(ch);
      if (x >= minX && x <= maxX) {
        const cell = row[x];
        if (!cell) continue;
        cell.ch = ch; cell.fg = fg; cell.wide = wide; cell.ox = ox; cell.oy = oy;
        if (bg) cell.bg = bg;
        if (wide && x + 1 <= maxX) { const n = row[x + 1]; if (n) { n.ch = ""; n.wide = false; if (bg) n.bg = bg; } }
      }
      x += wide ? 2 : 1;
    }
  }
  /** Standard Zenith frame: ╔═╗ / ║ ║ / ╚═╝ around a W×H field. */
  frame(W: number, H: number, side: string, glow: string, bg: string) {
    this.fill(bg);
    this.put(0, 0, "╔" + "═".repeat(W) + "╗", glow);
    this.put(0, H + 1, "╚" + "═".repeat(W) + "╝", glow);
    for (let y = 1; y <= H; y++) { this.put(0, y, "║", side); this.put(W + 1, y, "║", side); }
  }
  /** Write into the inner field using the shell's 0-based line index. */
  inner(i: number, y: number, str: string, fg: string, bg?: string) {
    this.put(i + 1, y, str, fg, bg, 1, this.cols - 2);
  }
  center(y: number, str: string, fg: string, W: number) {
    this.inner(Math.floor((W - Array.from(str).length) / 2), y, str, fg);
  }
}

const isWide = (ch: string) => (ch.codePointAt(0) ?? 0) > 0x2fff && !/[═║╔╗╚╝█▓▒░▀▄━▲▼◀▶●◉▰]/.test(ch) && (ch.codePointAt(0) ?? 0) >= 0x1f000;

export const CW = 12;
export const CH = 24;

export function renderGrid(ctx: CanvasRenderingContext2D, g: Grid) {
  ctx.textBaseline = "middle";
  ctx.textAlign = "center";
  for (let y = 0; y < g.rows; y++) for (let x = 0; x < g.cols; x++) {
    const cell = g.cells[y]?.[x];
    if (!cell) continue;
    ctx.fillStyle = cell.bg;
    ctx.fillRect(x * CW, y * CH, CW + 0.5, CH + 0.5);
  }
  for (let y = 0; y < g.rows; y++) {
    for (let x = 0; x < g.cols; x++) {
      const c = g.cells[y]?.[x];
      if (!c) continue;
      const px = (x + (c.ox ?? 0)) * CW, py = (y + (c.oy ?? 0)) * CH;
      if (!c.ch || c.ch === " ") continue;
      ctx.fillStyle = c.fg;
      if (drawBlock(ctx, c.ch, px, py)) continue;
      if (c.wide) {
        ctx.font = `${CH - 4}px "Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif`;
        ctx.fillText(c.ch, px + CW, py + CH / 2 + 1);
      } else {
        ctx.font = `bold ${CH - 5}px "DejaVu Sans Mono","Menlo","Consolas",monospace`;
        ctx.fillText(c.ch, px + CW / 2, py + CH / 2 + 1, CW + 2);
      }
    }
  }
}

function drawBlock(ctx: CanvasRenderingContext2D, ch: string, x: number, y: number) {
  const w = CW, h = CH, m = 2;
  const full = () => ctx.fillRect(x, y, w + 0.5, h + 0.5);
  switch (ch) {
    case "█": full(); return true;
    case "▀": ctx.fillRect(x, y, w + 0.5, h / 2); return true;
    case "▄": ctx.fillRect(x, y + h / 2, w + 0.5, h / 2 + 0.5); return true;
    case "▓": ctx.globalAlpha = 0.8; full(); ctx.globalAlpha = 1; return true;
    case "▒": ctx.globalAlpha = 0.5; full(); ctx.globalAlpha = 1; return true;
    case "░": ctx.globalAlpha = 0.25; full(); ctx.globalAlpha = 1; return true;
    case "━": ctx.fillRect(x, y + h / 2 - 2, w + 0.5, 4); return true;
    case "═": ctx.fillRect(x, y + h / 2 - 4, w + 0.5, m); ctx.fillRect(x, y + h / 2 + 2, w + 0.5, m); return true;
    case "║": ctx.fillRect(x + w / 2 - 4, y, m, h + 0.5); ctx.fillRect(x + w / 2 + 2, y, m, h + 0.5); return true;
    case "╔": dbl(ctx, x, y, false, true, false, true); return true;
    case "╗": dbl(ctx, x, y, true, false, false, true); return true;
    case "╚": dbl(ctx, x, y, false, true, true, false); return true;
    case "╝": dbl(ctx, x, y, true, false, true, false); return true;
  }
  return false;
}

// Double-line corner: which arms (left, right, up, down) exist.
function dbl(ctx: CanvasRenderingContext2D, x: number, y: number, L: boolean, R: boolean, U: boolean, D: boolean) {
  const cx = x + CW / 2, cy = y + CH / 2, m = 2;
  // outer line
  const ox = R ? cx - 4 : cx + 2, oy = D ? cy - 4 : cy + 2;
  const ix = R ? cx + 2 : cx - 4, iy = D ? cy + 2 : cy - 4;
  // horizontal arms
  if (R) { ctx.fillRect(ox, oy, x + CW - ox + 0.5, m); ctx.fillRect(ix, iy, x + CW - ix + 0.5, m); }
  if (L) { ctx.fillRect(x, oy, ox - x + m, m); ctx.fillRect(x, iy, ix - x + m, m); }
  // vertical arms
  if (D) { ctx.fillRect(ox, oy, m, y + CH - oy + 0.5); ctx.fillRect(ix, iy, m, y + CH - iy + 0.5); }
  if (U) { ctx.fillRect(ox, y, m, oy - y + m); ctx.fillRect(ix, y, m, iy - y + m); }
}

export interface TermGame {
  W: number;
  H: number;
  started: boolean;
  over: boolean;
  score: number;
  delay(): number;
  start(): void;
  tick(): void;
  draw(g: Grid): void;
  hud(): Seg[];
  /** Tap: inner column (0-based) and side of the screen (-1 left / 1 right). */
  tap(col: number, side: -1 | 1): void;
  /** Drag: horizontal movement in columns since the last event. */
  drag(dxCols: number): void;
  /** Optional four-way joystick step (-1/0/1 each). */
  dir?(dx: number, dy: number): void;
  /** Optional direct tap on grid cell (inner col, row). */
  tapAt?(col: number, row: number): void;
}

export type GameOpts = { best: number; opts: Record<string, string> };
export const rnd = (n: number) => Math.floor(Math.random() * n);
