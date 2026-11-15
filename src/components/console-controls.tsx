import { useEffect, useRef, useState, type PointerEvent } from "react";
import { Button } from "@/components/ui/button";

type Handlers = {
  onTap?: ((side: -1 | 1) => void) | undefined;
  onDrag?: ((dx: number) => void) | undefined;
  /** Four-way step when the knob is pushed out in a direction. */
  onStick?: ((dx: number, dy: number) => void) | undefined;
  /** Short press released without pushing the knob. */
  onPress?: (() => void) | undefined;
};

export function ConsoleControls({ onMenu, over = false, repeat = false, ...h }: Handlers & {
  onMenu?: () => void;
  /** Keep stepping while the knob is held out in a direction. */
  repeat?: boolean;
  over?: boolean;
}) {
  return (
    <div className="handheld-controls">
      <TouchControl side={-1} repeat={repeat} {...h} />
      <div className="console-center-controls">
        <span className="console-game-status" role="status">{over ? "GAME OVER" : ""}</span>
        <Button variant="ghost" size="sm" onClick={onMenu} disabled={!onMenu} className="console-menu-button">MENÜ</Button>
      </div>
      <TouchControl side={1} repeat={repeat} {...h} />
    </div>
  );
}

const REACH = 22;
const STEP = 12; // knob pixels needed to count as a direction push

function TouchControl({ side, onTap, onDrag, onStick, onPress, repeat }: Handlers & { side: -1 | 1; repeat: boolean }) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const stick = useRef(onStick);
  stick.current = onStick;
  const stopRepeat = () => { if (timer.current) clearTimeout(timer.current); timer.current = null; };
  useEffect(() => stopRepeat, []);
  const startRepeat = (dx: number, dy: number) => {
    stopRepeat();
    if (!repeat) return;
    const step = (wait: number) => { timer.current = setTimeout(() => { stick.current?.(dx, dy); step(160); }, wait); };
    step(380);
  };
  const pointer = useRef<{ id: number; x: number; sx: number; sy: number; dir: string; max: number } | null>(null);
  const [knob, setKnob] = useState({ x: 0, y: 0, active: false });
  const down = (e: PointerEvent<HTMLButtonElement>) => {
    if (pointer.current) return;
    e.preventDefault();
    e.currentTarget.setPointerCapture(e.pointerId);
    pointer.current = { id: e.pointerId, x: e.clientX, sx: e.clientX, sy: e.clientY, dir: "", max: 0 };
    setKnob({ x: 0, y: 0, active: true });
    onTap?.(side);
  };
  const move = (e: PointerEvent<HTMLButtonElement>) => {
    const active = pointer.current;
    if (!active || active.id !== e.pointerId) return;
    onDrag?.(e.clientX - active.x);
    active.x = e.clientX;
    const x = e.clientX - active.sx;
    const y = (e.clientY ?? active.sy) - active.sy;
    const d = Math.hypot(x, y);
    active.max = Math.max(active.max, d);
    const k = d > REACH ? REACH / d : 1;
    setKnob({ x: x * k, y: y * k, active: true });
    if (onStick) {
      let dir = "";
      if (d >= STEP) dir = Math.abs(x) > Math.abs(y) ? (x > 0 ? "R" : "L") : (y > 0 ? "D" : "U");
      else if (d < STEP / 2) dir = "";
      else dir = active.dir;
      if (dir !== active.dir) {
        if (dir) {
          const dx = dir === "R" ? 1 : dir === "L" ? -1 : 0, dy = dir === "D" ? 1 : dir === "U" ? -1 : 0;
          onStick(dx, dy);
          startRepeat(dx, dy);
        } else stopRepeat();
      }
      active.dir = dir;
    }
  };
  const release = (e: PointerEvent<HTMLButtonElement>) => {
    const active = pointer.current;
    if (active?.id === e.pointerId) {
      pointer.current = null;
      stopRepeat();
      setKnob({ x: 0, y: 0, active: false });
      if (e.type === "pointerup" && active.max < STEP) onPress?.();
    }
  };
  const dist = Math.min(REACH, Math.hypot(knob.x, knob.y));
  const pull = dist / REACH;
  const angle = dist > 0.4 ? (Math.atan2(knob.y, knob.x) * 180) / Math.PI : 0;
  const jelly = `rotate(${angle.toFixed(2)}deg) scale(${(1 + 0.06 * pull).toFixed(3)}, ${(1 - 0.05 * pull).toFixed(3)}) rotate(${(-angle).toFixed(2)}deg)`;
  return (
    <Button
      variant="outline"
      className="console-touch-button"
      aria-label={side < 0 ? "Bal érintőgomb" : "Jobb érintőgomb"}
      title={side < 0 ? "Bal érintőgomb" : "Jobb érintőgomb"}
      disabled={!onTap && !onStick && !onPress}
      onPointerDown={down}
      onPointerMove={move}
      onPointerUp={release}
      onPointerCancel={release}
      onLostPointerCapture={release}
      onClick={(e) => { if (e.detail === 0) { onTap?.(side); onPress?.(); } }}
    >
      <span
        aria-hidden="true"
        className={`console-knob${knob.active ? " is-active" : ""}`}
        style={{ transform: `translate(${knob.x.toFixed(2)}px, ${knob.y.toFixed(2)}px) ${jelly}` }}
      />
    </Button>
  );
}
