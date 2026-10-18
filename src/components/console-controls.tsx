import { useRef, type PointerEvent } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function ConsoleControls({ onTap, onDrag, onMenu, over = false }: {
  onTap?: (side: -1 | 1) => void;
  onDrag?: (dx: number) => void;
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

function TouchControl({ side, onTap, onDrag }: {
  side: -1 | 1;
  onTap?: (side: -1 | 1) => void;
  onDrag?: (dx: number) => void;
}) {
  const pointer = useRef<{ id: number; x: number } | null>(null);
  const down = (e: PointerEvent<HTMLButtonElement>) => {
    if (pointer.current) return;
    e.preventDefault();
    e.currentTarget.setPointerCapture(e.pointerId);
    pointer.current = { id: e.pointerId, x: e.clientX };
    onTap?.(side);
  };
  const move = (e: PointerEvent<HTMLButtonElement>) => {
    const active = pointer.current;
    if (!active || active.id !== e.pointerId) return;
    onDrag?.(e.clientX - active.x);
    active.x = e.clientX;
  };
  const release = (e: PointerEvent<HTMLButtonElement>) => {
    if (pointer.current?.id === e.pointerId) pointer.current = null;
  };
  const Icon = side < 0 ? ArrowLeft : ArrowRight;
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
    ><Icon aria-hidden="true" /></Button>
  );
}