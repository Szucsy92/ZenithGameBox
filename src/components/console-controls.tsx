import { useRef, useState, type PointerEvent } from "react";
import { Button } from "@/components/ui/button";

export function ConsoleControls({ onTap, onDrag, onMenu, over = false }: {
  onTap?: ((side: -1 | 1) => void) | undefined;
  onDrag?: ((dx: number) => void) | undefined;
  onMenu?: () => void;
  over?: boolean;
}) {
  return (
    <div className="handheld-controls">
      <TouchControl side={-1} onTap={onTap} onDrag={onDrag} />
      <div className="console-center-controls">
        <span className="console-game-status" role="status">{over ? "GAME OVER" : ""}</span>
        <Button variant="ghost" size="sm" onClick={onMenu} disabled={!onMenu} className="console-menu-button">MENÜ</Button>
      </div>
      <TouchControl side={1} onTap={onTap} onDrag={onDrag} />
    </div>
  );
}

const REACH = 16;

function TouchControl({ side, onTap, onDrag }: {
  side: -1 | 1;
  onTap?: ((side: -1 | 1) => void) | undefined;
  onDrag?: ((dx: number) => void) | undefined;
}) {
  const pointer = useRef<{ id: number; x: number; sx: number; sy: number } | null>(null);
  const [knob, setKnob] = useState({ x: 0, y: 0, active: false });
  const down = (e: PointerEvent<HTMLButtonElement>) => {
    if (pointer.current) return;
    e.preventDefault();
    e.currentTarget.setPointerCapture(e.pointerId);
    pointer.current = { id: e.pointerId, x: e.clientX, sx: e.clientX, sy: e.clientY };
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
    const k = d > REACH ? REACH / d : 1;
    setKnob({ x: x * k, y: y * k, active: true });
  };
  const release = (e: PointerEvent<HTMLButtonElement>) => {
    if (pointer.current?.id === e.pointerId) {
      pointer.current = null;
      setKnob({ x: 0, y: 0, active: false });
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
      disabled={!onTap}
      onPointerDown={down}
      onPointerMove={move}
      onPointerUp={release}
      onPointerCancel={release}
      onLostPointerCapture={release}
      onClick={(e) => { if (e.detail === 0) onTap?.(side); }}
    >
      <span
        aria-hidden="true"
        className={`console-knob${knob.active ? " is-active" : ""}`}
        style={{ transform: `translate(${knob.x.toFixed(2)}px, ${knob.y.toFixed(2)}px) ${jelly}` }}
      />
    </Button>
  );
}
