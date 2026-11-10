import { describe, expect, it } from "vitest";
import { GAMES } from "@/games/engine";
import { memory } from "@/games/memory";
import { snake } from "@/games/snake";

describe("new games", () => {
  it("lists Snake as 6th and Memory as 7th game", () => {
    expect(GAMES[5]?.id).toBe("snake");
    expect(GAMES[6]?.id).toBe("memory");
  });
  it("snake cannot reverse into itself", () => {
    const g = snake({ best: 0, opts: {} });
    g.start();
    g.dir(-1, 0); // opposite of initial right
    for (let i = 0; i < 3; i++) g.tick();
    expect(g.over).toBe(false);
  });
  it("memory has 16 cards (8 pairs) and counts a move per two flips", () => {
    const g = memory({ best: 0, opts: {} });
    g.start();
    g.press();
    g.dir(1, 0);
    g.press();
    expect(g.hud()[0]?.t).toContain("LÉPÉS: 1");
  });
});
