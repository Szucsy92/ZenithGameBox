package hu.zenith.gamebox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import hu.zenith.gamebox.R
import hu.zenith.gamebox.app.Prefs
import hu.zenith.gamebox.app.UpdateViewModel
import hu.zenith.gamebox.game.CH
import hu.zenith.gamebox.game.CW
import hu.zenith.gamebox.game.GAMES
import hu.zenith.gamebox.game.GameDef
import hu.zenith.gamebox.game.GameOpts
import hu.zenith.gamebox.game.Grid
import hu.zenith.gamebox.game.Seg
import hu.zenith.gamebox.game.TermGame
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

@Composable
fun ZenithApp(prefs: Prefs, vibrate: (Long) -> Unit, updateVm: UpdateViewModel, onInstall: (File) -> Unit) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var cursor by remember { mutableIntStateOf(0) }
    BackHandler(enabled = selected != null) { selected = null }
    Box(Modifier.fillMaxSize().background(Z.Background)) {
        ConsolePage(
            selected = selected,
            cursor = cursor,
            onCursor = { cursor = it },
            onSelect = { selected = it },
            prefs = prefs,
            vibrate = vibrate,
        )
        UpdateOverlay(updateVm, onInstall)
    }
}

// ---------------------------------------------------------------------------------------------
// Konzol-oldal: cím, kézi konzol (képernyő + gombok), jelmondat
// ---------------------------------------------------------------------------------------------

@Composable
private fun ConsolePage(
    selected: Int?,
    cursor: Int,
    onCursor: (Int) -> Unit,
    onSelect: (Int?) -> Unit,
    prefs: Prefs,
    vibrate: (Long) -> Unit,
) {
    val ins = WindowInsets.safeDrawing.asPaddingValues()
    val ld = LocalLayoutDirection.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val small = maxWidth <= 380.dp
        val padT = maxOf(if (small) 12.dp else 24.dp, ins.calculateTopPadding())
        val padB = maxOf(if (small) 16.dp else 24.dp, ins.calculateBottomPadding())
        val padL = maxOf(if (small) 8.dp else 16.dp, ins.calculateLeftPadding(ld))
        val padR = maxOf(if (small) 8.dp else 16.dp, ins.calculateRightPadding(ld))
        val availW = maxWidth - padL - padR
        val availH = maxHeight - padT - padB
        val gap = if (small) 10.dp else 12.dp
        // Cím (40) + jelmondat (16) + két hézag + a konzol kerete/belső margója (40) + gombsor (116) levonása után
        // a képernyő 3:4 arányú; ha nem fér el magasságban, a konzol keskenyebb lesz.
        val fixed = 40.dp + 16.dp + gap + gap + 40.dp + 116.dp
        val byHeight = 40.dp + (availH - fixed) * 0.75f
        val hw = minOf(480.dp, availW, byHeight).coerceAtLeast(220.dp)

        Column(
            modifier = Modifier.fillMaxSize().padding(start = padL, end = padR, top = padT, bottom = padB),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.zenith_icon),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    filterQuality = FilterQuality.None,
                )
                Text(
                    "ZENITH GAMES", color = Z.Primary, fontSize = if (small) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold, fontFamily = Z.Term, textAlign = TextAlign.Center,
                )
            }
            Handheld(hw = hw, small = small) {
                val def = if (selected == null) null else GAMES.getOrNull(selected)
                if (def != null) {
                    PlayerScreen(def = def, prefs = prefs, vibrate = vibrate, onExit = { onSelect(null) })
                } else {
                    Screen {
                        val best = remember { GAMES.map { prefs.best(it.id) } }
                        GameMenu(cursor = cursor, best = best, onHover = onCursor, onPick = { onSelect(it) })
                    }
                    ConsoleControls(
                        repeat = true,
                        onStick = { _, dy -> if (dy != 0) onCursor((cursor + dy + GAMES.size) % GAMES.size) },
                        onPress = { onSelect(cursor) },
                    )
                }
            }
            Text("Life is a game...", color = Z.MutedFg, fontSize = 12.sp, fontFamily = Z.Term)
        }
    }
}

/** Puha, színes külső fény egy lekerekített téglalap körül (a CSS box-shadow glow megfelelője). */
private fun Modifier.softGlow(color: Color, extent: Dp, corner: Dp, peak: Float): Modifier = drawBehind {
    val e = extent.toPx()
    val n = 10
    for (i in 0 until n) {
        val t = (i + 1) / n.toFloat()
        val off = e * t
        drawRoundRect(
            color = color.copy(alpha = peak * (1f - t) * (1f - t)),
            topLeft = Offset(-off, -off),
            size = Size(size.width + 2f * off, size.height + 2f * off),
            cornerRadius = CornerRadius(corner.toPx() + off),
            style = Stroke(width = e / n + 1f),
        )
    }
}

@Composable
private fun Handheld(hw: Dp, small: Boolean, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(44.dp)
    Column(
        modifier = Modifier
            .width(hw)
            .softGlow(Z.Primary, 28.dp, 44.dp, 0.32f)
            .clip(shape)
            .background(Z.Card)
            .border(4.dp, Z.Primary, shape)
            .padding(if (small) 10.dp else 14.dp),
    ) { content() }
}

@Composable
private fun Screen(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape).background(Z.Background).border(2.dp, Z.Border, shape).padding(2.dp),
    ) { content() }
}

// ---------------------------------------------------------------------------------------------
// Játékmenü
// ---------------------------------------------------------------------------------------------

@Composable
private fun GameMenu(cursor: Int, best: List<Int>, onHover: (Int) -> Unit, onPick: (Int) -> Unit) {
    val scroll = rememberScrollState()
    val originX = remember { floatArrayOf(0f, 0f) }
    val bounds = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(GAMES.size) }
    val tops = remember { FloatArray(GAMES.size) }
    val heights = remember { FloatArray(GAMES.size) }
    val hoverCb by rememberUpdatedState(onHover)
    val pickCb by rememberUpdatedState(onPick)
    val density = LocalDensity.current
    val itemShape = RoundedCornerShape(8.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .onGloballyPositioned { c -> val p = c.positionInRoot(); originX[0] = p.x; originX[1] = p.y }
            .pointerInput(Unit) {
                fun idxAt(local: Offset): Int? {
                    val rx = originX[0] + local.x
                    val ry = originX[1] + local.y
                    for (i in bounds.indices) {
                        val b = bounds[i] ?: continue
                        if (rx >= b.left && rx <= b.right && ry >= b.top && ry <= b.bottom) return i
                    }
                    return null
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val startY = down.position.y
                    var moved = false
                    idxAt(down.position)?.let { hoverCb(it) }
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (ch.changedToUpIgnoreConsumed()) {
                            if (!moved) idxAt(ch.position)?.let { pickCb(it) }
                            break
                        }
                        if (abs(ch.position.y - startY) > 12.dp.toPx()) moved = true
                        idxAt(ch.position)?.let { hoverCb(it) }
                        ch.consume()
                    }
                }
            },
    ) {
        val viewportPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 18.dp.toPx() }
        LaunchedEffect(cursor) {
            val top = tops[cursor] + padPx
            val bottom = top + heights[cursor]
            if (top < scroll.value) scroll.animateScrollTo((top - padPx).toInt().coerceAtLeast(0))
            else if (bottom > scroll.value + viewportPx) scroll.animateScrollTo((bottom + padPx - viewportPx).toInt())
        }
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.zenith_start),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.75f,
            filterQuality = FilterQuality.None,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll, enabled = false)
                .heightIn(min = maxHeight)
                .padding(horizontal = 30.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            GAMES.forEachIndexed { i, g ->
                val sel = i == cursor
                val scale by animateFloatAsState(
                    targetValue = if (sel) 1.1f else 1f,
                    animationSpec = tween<Float>(180),
                    label = "menuScale",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (sel) 1f else 0f)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .onGloballyPositioned { c ->
                            bounds[i] = c.boundsInRoot()
                            tops[i] = c.positionInParent().y
                            heights[i] = c.size.height.toFloat()
                        }
                        .then(if (sel) Modifier.softGlow(Z.Dino, 14.dp, 8.dp, 0.55f) else Modifier)
                        .clip(itemShape)
                        .background(Z.Background.copy(alpha = 0.88f))
                        .border(2.dp, if (sel) Z.Dino else Z.Border, itemShape)
                        .heightIn(min = if (sel) 46.dp else 38.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${i + 1}) ${g.name.replace("ZENITH ", "")}",
                            color = inkOf(g.id), fontSize = if (sel) 15.sp else 13.sp,
                            fontFamily = Z.Term, fontWeight = FontWeight.Medium,
                        )
                        Text("BEST ${best.getOrElse(i) { 0 }}", color = Z.MutedFg, fontSize = 10.sp, fontFamily = Z.Term)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Játékfelület
// ---------------------------------------------------------------------------------------------

private enum class Phase { SETUP, PLAY, OVER }

private class PlayerState(opts: Map<String, String>) {
    var phase by mutableStateOf(Phase.SETUP)
    var run by mutableIntStateOf(0)
    var hud by mutableStateOf<List<Seg>>(emptyList())
    var opts by mutableStateOf(opts)
    var aspect by mutableFloatStateOf(60f * CW / (21f * CH))
    var game: TermGame? = null
    var canvasW = 0f
}

private fun loadOpts(def: GameDef, prefs: Prefs): Map<String, String> =
    def.options.associate { d ->
        val stored = prefs.opt(def.id, d.key)
        d.key to (if (!stored.isNullOrEmpty()) stored else (d.choices.firstOrNull()?.value ?: ""))
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerScreen(def: GameDef, prefs: Prefs, vibrate: (Long) -> Unit, onExit: () -> Unit) {
    val st = remember(def) { PlayerState(loadOpts(def, prefs)) }
    val holder = remember(def) { GridHolder() }

    // A játék (újra)létrehozása indításkor, újraindításkor és a beállítás-képernyőn tett választáskor.
    LaunchedEffect(def, st.opts, st.run) {
        val best = prefs.best(def.id)
        val gm = def.make(GameOpts(best, st.opts, vibrate))
        if (st.phase == Phase.PLAY) gm.start()
        st.game = gm
        st.aspect = ((gm.W + 2) * CW).toFloat() / ((gm.H + 2) * CH).toFloat()
        val grid = Grid(gm.W + 2, gm.H + 2)
        holder.grid = grid
        var acc = 0.0
        var last = withFrameMillis { it }.toDouble()
        var overAt = 0.0
        while (isActive) {
            val t = withFrameMillis { it }.toDouble()
            acc += min(200.0, t - last)
            last = t
            var d = gm.delay()
            while (acc >= d) { acc -= d; gm.tick(); d = gm.delay() }
            if (gm.over && st.phase == Phase.PLAY) {
                if (overAt == 0.0) {
                    overAt = t
                    val sc = gm.score.toInt()
                    if (sc > best) prefs.setBest(def.id, sc)
                } else if (t - overAt > 800) st.phase = Phase.OVER
            }
            gm.draw(grid)
            st.hud = gm.hud()
            holder.frame.longValue = holder.frame.longValue + 1L
        }
    }

    fun controlTap(side: Int) {
        val gm = st.game ?: return
        if (st.phase == Phase.OVER || gm.over) {
            st.phase = Phase.PLAY
            st.run += 1
            return
        }
        if (st.phase == Phase.SETUP) {
            st.phase = Phase.PLAY
            gm.start()
        }
        gm.tap(floor(gm.W * (if (side < 0) 0.25 else 0.75)).toInt(), side)
    }

    fun controlDrag(dx: Float) {
        val gm = st.game ?: return
        if (st.canvasW <= 0f || st.phase != Phase.PLAY || gm.over) return
        gm.drag((dx * (gm.W + 2) / st.canvasW).toDouble())
    }

    Screen {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val minH = maxWidth * 4f / 3f
                Column(Modifier.fillMaxWidth().heightIn(min = minH)) {
                    Text(
                        def.name, color = inkOf(def.id), fontSize = 12.sp, fontFamily = Z.Term,
                        modifier = Modifier.padding(12.dp),
                    )
                    GridCanvas(
                        holder = holder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(st.aspect)
                            .onSizeChanged { st.canvasW = it.width.toFloat() }
                            .pointerInput(def) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    val gm = st.game ?: return@awaitEachGesture
                                    if (st.phase == Phase.OVER) { st.phase = Phase.PLAY; st.run += 1; return@awaitEachGesture }
                                    if (st.phase == Phase.SETUP) { st.phase = Phase.PLAY; gm.start(); return@awaitEachGesture }
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val cols = gm.W + 2
                                    var lastX = down.position.x / w * cols - 1f
                                    val side = if (down.position.x < w / 2) -1 else 1
                                    if (gm.usesTapAt) {
                                        gm.tapAt(floor(lastX).toInt(), floor(down.position.y / h * (gm.H + 2)).toInt())
                                    } else {
                                        gm.tap(floor(lastX).toInt(), side)
                                    }
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                        if (ch.changedToUpIgnoreConsumed()) break
                                        if (st.phase == Phase.PLAY) {
                                            val col = ch.position.x / w * cols - 1f
                                            val dx = col - lastX
                                            if (abs(dx) >= 1f) { gm.drag(dx.toDouble()); lastX = col }
                                        }
                                        ch.consume()
                                    }
                                }
                            },
                    )
                    // HUD
                    Box(Modifier.fillMaxWidth().heightIn(min = 24.dp).background(Z.Background).padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(
                            text = buildAnnotatedString {
                                for (s in st.hud) {
                                    if (s.c != null) withStyle(SpanStyle(color = Color(s.c))) { append(s.t) } else append(s.t)
                                }
                            },
                            color = Z.Foreground, fontSize = 10.sp, fontFamily = Z.Term,
                        )
                    }
                    // Beállítások (csak az indítás előtti képernyőn)
                    if (st.phase == Phase.SETUP && def.options.isNotEmpty()) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (d in def.options) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        "${d.label}:", color = Z.MutedFg, fontSize = 12.sp, fontFamily = Z.Term,
                                        modifier = Modifier.width(64.dp).padding(top = 8.dp),
                                    )
                                    for (c in d.choices) {
                                        val chosen = st.opts[d.key] == c.value
                                        val bs = RoundedCornerShape(6.dp)
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 32.dp)
                                                .clip(bs)
                                                .background(Z.Background)
                                                .border(2.dp, if (chosen) Z.Primary else Z.Border, bs)
                                                .clickable {
                                                    prefs.setOpt(def.id, d.key, c.value)
                                                    st.opts = st.opts + (d.key to c.value)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                c.label, fontSize = 12.sp, fontFamily = Z.Term,
                                                color = if (c.color != null) Color(c.color) else Z.Foreground,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    ConsoleControls(
        onMenu = onExit,
        over = st.phase == Phase.OVER,
        onTap = { side -> controlTap(side) },
        onDrag = { dx -> controlDrag(dx) },
        onStick = { dx, dy ->
            val gm = st.game
            if (gm != null && st.phase == Phase.PLAY && !gm.over) gm.dir(dx, dy)
        },
        onPress = {
            val gm = st.game
            if (gm != null && st.phase == Phase.PLAY && !gm.over) gm.press()
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Frissítés (OTA) ablak
// ---------------------------------------------------------------------------------------------

@Composable
private fun UpdateOverlay(vm: UpdateViewModel, onInstall: (File) -> Unit) {
    val st by vm.state.collectAsState()
    val info = st.info ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC060410))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp)
                .clip(shape)
                .background(Z.Card)
                .border(3.dp, Z.Primary, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Új frissítés elérhető", color = Z.Foreground, fontSize = 22.sp, fontFamily = Z.Term,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
            Text(info.versionName, color = Z.MutedFg, fontSize = 13.sp, fontFamily = Z.Term, textAlign = TextAlign.Center)
            if (st.stage == 1) {
                Text("Letöltés… ${st.progress}%", color = Z.Foreground, fontSize = 18.sp, fontFamily = Z.Term)
                Box(Modifier.fillMaxWidth().height12().clip(RoundedCornerShape(6.dp)).background(Z.Secondary)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(st.progress / 100f).background(Z.Primary))
                }
            } else {
                if (st.stage == 3) {
                    Text("Nem sikerült letölteni. Próbáld újra!", color = Z.Foreground, fontSize = 14.sp, fontFamily = Z.Term, textAlign = TextAlign.Center)
                }
                if (st.stage == 2) {
                    Text("A telepítés elindult. Hagyd jóvá a megjelenő ablakban.", color = Z.Foreground, fontSize = 14.sp, fontFamily = Z.Term, textAlign = TextAlign.Center)
                }
                UpdateButton(if (st.stage == 2) "Telepítés újra" else "Letöltés", Z.Primary, Z.PrimaryFg) { vm.start(onInstall) }
                UpdateButton("Később", Z.Secondary, Z.Foreground) { vm.dismiss() }
            }
        }
    }
}

private fun Modifier.height12(): Modifier = this.heightIn(min = 12.dp, max = 12.dp)

@Composable
private fun UpdateButton(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 16.sp, fontFamily = Z.Term, fontWeight = FontWeight.Bold)
    }
}
