import { fireEvent, render, cleanup } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ConsoleControls } from "@/components/console-controls";
import { pong } from "@/games/pong";
import { space } from "@/games/space";
import { Grid } from "@/games/term";

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

describe("Console input", () => {
  for (const [name, side] of [["Bal érintőgomb", -1], ["Jobb érintőgomb", 1]] as const) {
    it(`${name} supports taps and captured swipes`, () => {
      const tap = vi.fn(), drag = vi.fn();
      const ui = render(<ConsoleControls onTap={tap} onDrag={drag} />);
      const button = ui.getByRole("button", { name });
      button.setPointerCapture = vi.fn();
      const pointer = (type: string, x: number) => {
        const event = new Event(type, { bubbles: true, cancelable: true });
        Object.assign(event, { pointerId: 7, clientX: x });
        fireEvent(button, event);
      };
      pointer("pointerdown", 100);
      pointer("pointermove", 124);
      pointer("pointerup", 124);
      pointer("pointermove", 150);
      expect(tap).toHaveBeenCalledExactlyOnceWith(side);
      expect(drag).toHaveBeenCalledExactlyOnceWith(24);
    });
  }
});

describe("Relaxed game motion", () => {
  it("Pong has a wider paddle and slower fractional ball movement", () => {
    vi.spyOn(Math, "random").mockReturnValue(0.5);
    const game = pong({ best: 0, opts: {} });
    const grid = new Grid(60, 21);
    game.draw(grid);
    expect(grid.cells[19]?.filter(c => c.ch === "▓")).toHaveLength(20);
    game.start();
    game.tick();
    game.draw(grid);
    const ball = grid.cells.flat().find(c => c.ch === "🟠");
    expect(ball?.oy).toBeCloseTo(-0.30);
    expect(game.over).toBe(false);
  });
  it("Pong still scores a crossed paddle collision", () => {
    vi.spyOn(Math, "random").mockReturnValue(0.5);
    const game = pong({ best: 0, opts: {} });
    game.start();
    game.tap(0, 1); game.tap(0, 1); game.tap(0, 1);
    for (let i = 0; i < 87; i++) game.tick();
    expect(game.score).toBe(1);
    expect(game.over).toBe(false);
  });
  it("Space fires automatically and moves with fractional swipes", () => {
    const game = space({ best: 0, opts: {} });
    game.start(); game.drag(2.5); game.tick();
    const grid = new Grid(58, 21);
    game.draw(grid);
    expect(grid.cells.flat().some(c => c.ch === "|")).toBe(true);
    const ship = grid.cells.flat().find(c => c.ch === "╞");
    expect(ship?.ox).toBeCloseTo(-0.4);
  });
});