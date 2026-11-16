package hu.zenith.gamebox.game

class OptChoice(val value: String, val label: String, val color: Int? = null)
class OptDef(val key: String, val label: String, val choices: List<OptChoice>)

class GameDef(
    val id: String,
    val name: String,
    val make: (GameOpts) -> TermGame,
    val options: List<OptDef>,
)

val GAMES: List<GameDef> = listOf(
    GameDef("bird", "ZENITH BIRD", { o -> Bird(o) }, emptyList()),
    GameDef("dino", "ZENITH DINO", { o -> Dino(o) }, emptyList()),
    GameDef(
        "pong", "ZENITH PONG", { o -> Pong(o) },
        listOf(
            OptDef("ball", "Ball", PONG_BALLS.map { OptChoice(it, it) }),
            OptDef("pad", "Paddle", PONG_PADS.map { OptChoice(it, "▓▓▓", hex(it)) }),
        ),
    ),
    GameDef(
        "space", "ZENITH SPACE", { o -> Space(o) },
        listOf(
            OptDef("bullet", "Bullet", SPACE_BULLETS.map { OptChoice(it.first, "|", hex(it.second)) }),
            OptDef("ship", "Ship", SPACE_SHIPS.map { OptChoice(it, it, hex("#af87ff")) }),
        ),
    ),
    GameDef(
        "jump", "ZENITH JUMP", { o -> Jump(o) },
        listOf(OptDef("skin", "Block Skin", JUMP_SKINS.mapIndexed { i, c -> OptChoice((i + 1).toString(), "██", hex(c)) })),
    ),
    GameDef("snake", "ZENITH SNAKE", { o -> Snake(o) }, emptyList()),
    GameDef("memory", "ZENITH MEMORY", { o -> Memory(o) }, emptyList()),
)
