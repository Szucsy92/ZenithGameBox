import { bird } from "./bird";
import { dino } from "./dino";
import { pong, PONG_BALLS, PONG_PADS } from "./pong";
import { space, SPACE_BULLETS, SPACE_SHIPS } from "./space";
import { jump, JUMP_SKINS } from "./jump";
import type { GameOpts, TermGame } from "./term";

export type OptChoice = { value: string; label: string; color?: string };
export type OptDef = { key: string; label: string; choices: OptChoice[] };

export type GameDef = {
  id: string;
  name: string;
  color: string;
  make: (o: GameOpts) => TermGame;
  options: OptDef[];
};

export const GAMES: GameDef[] = [
  { id: "bird", name: "ZENITH BIRD", color: "#ffff00", make: bird, options: [] },
  { id: "dino", name: "ZENITH DINO", color: "#af87ff", make: dino, options: [] },
  {
    id: "pong", name: "ZENITH PONG", color: "#ff5f00", make: pong,
    options: [
      { key: "ball", label: "Ball", choices: PONG_BALLS.map((b) => ({ value: b, label: b })) },
      { key: "pad", label: "Paddle", choices: PONG_PADS.map((c) => ({ value: c, label: "▓▓▓", color: c })) },
    ],
  },
  {
    id: "space", name: "ZENITH SPACE", color: "#00ffff", make: space,
    options: [
      { key: "bullet", label: "Bullet", choices: SPACE_BULLETS.map((b) => ({ value: b.label, label: "|", color: b.c })) },
      { key: "ship", label: "Ship", choices: SPACE_SHIPS.map((s) => ({ value: s, label: s, color: "#af87ff" })) },
    ],
  },
  {
    id: "jump", name: "ZENITH JUMP", color: "#55ff55", make: jump,
    options: [{ key: "skin", label: "Block Skin", choices: JUMP_SKINS.map((c, i) => ({ value: String(i + 1), label: "██", color: c })) }],
  },
];
