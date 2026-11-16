package hu.zenith.gamebox.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** A két kerek érintőgomb + középen a GAME OVER felirat és a MENÜ gomb. */
@Composable
fun ConsoleControls(
    onMenu: (() -> Unit)? = null,
    over: Boolean = false,
    repeat: Boolean = false,
    onTap: ((Int) -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onStick: ((Int, Int) -> Unit)? = null,
    onPress: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TouchControl(-1, repeat, onTap, onDrag, onStick, onPress)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.heightIn(min = 16.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (over) "GAME OVER" else "",
                    color = Z.Foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Z.Term,
                    maxLines = 1, softWrap = false,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onMenu != null, onClick = { onMenu?.invoke() })
                    .alpha(if (onMenu != null) 1f else 0.5f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("MENÜ", color = Z.MutedFg, fontSize = 11.sp, fontFamily = Z.Term, fontWeight = FontWeight.Medium)
            }
        }
        TouchControl(1, repeat, onTap, onDrag, onStick, onPress)
    }
}

// A gomb körül 9 mp-es ciklusban váltakozó fény: lila → narancs (halvány) → zöld → sárga (halvány) → lila.
private val GLOW_KEYS: List<Pair<Color, Float>> = listOf(
    Z.Primary to 1f, Z.Pong to 0.33f, Z.Jump to 1f, Z.Bird to 0.33f, Z.Primary to 1f,
)

private fun glowAt(p: Float): Pair<Color, Float> {
    val s = p.coerceIn(0f, 0.9999f) * 4f
    val i = s.toInt()
    val t0 = s - i
    val t = t0 * t0 * (3f - 2f * t0)
    val a = GLOW_KEYS[i]
    val b = GLOW_KEYS[i + 1]
    return lerp(a.first, b.first, t) to (a.second + (b.second - a.second) * t)
}

@Composable
private fun TouchControl(
    side: Int,
    repeat: Boolean,
    onTap: ((Int) -> Unit)?,
    onDrag: ((Float) -> Unit)?,
    onStick: ((Int, Int) -> Unit)?,
    onPress: (() -> Unit)?,
) {
    val enabled = onTap != null || onStick != null || onPress != null
    val tapCb by rememberUpdatedState(onTap)
    val dragCb by rememberUpdatedState(onDrag)
    val stickCb by rememberUpdatedState(onStick)
    val pressCb by rememberUpdatedState(onPress)
    val scope = rememberCoroutineScope()
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }
    val holder = remember { arrayOfNulls<Job>(1) }

    val ax = animateFloatAsState(
        targetValue = knobX,
        animationSpec = if (active) tween<Float>(90) else spring<Float>(dampingRatio = 0.42f, stiffness = 260f),
        label = "knobX",
    )
    val ay = animateFloatAsState(
        targetValue = knobY,
        animationSpec = if (active) tween<Float>(90) else spring<Float>(dampingRatio = 0.42f, stiffness = 260f),
        label = "knobY",
    )
    val transition = rememberInfiniteTransition(label = "glow")
    val phase = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "phase",
    )

    fun stopRepeat() { holder[0]?.cancel(); holder[0] = null }
    fun startRepeat(dx: Int, dy: Int) {
        stopRepeat()
        if (!repeat) return
        holder[0] = scope.launch {
            delay(380)
            while (true) {
                stickCb?.invoke(dx, dy)
                delay(160)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(92.dp)
            .alpha(if (enabled) 1f else 0.65f)
            .semantics { contentDescription = if (side < 0) "Bal érintőgomb" else "Jobb érintőgomb"; role = Role.Button }
            .drawBehind {
                val (gc, gs) = glowAt(phase.value)
                val r = size.minDimension / 2f
                val extent = 16.dp.toPx() + 20.dp.toPx() * gs
                val total = r + extent
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to gc.copy(alpha = 0.5f * gs), (r / total) to gc.copy(alpha = 0.5f * gs), 1f to Color.Transparent,
                        center = center, radius = total,
                    ),
                    radius = total, center = center,
                )
            }
            .clip(CircleShape)
            .background(Z.Secondary)
            .drawBehind {
                val (gc, gs) = glowAt(phase.value)
                val r = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        0.55f to Color.Transparent, 1f to gc.copy(alpha = 0.32f * gs),
                        center = center, radius = r,
                    ),
                    radius = r, center = center,
                )
            }
            .border(3.dp, Z.Primary, CircleShape)
            .pointerInput(enabled, repeat) {
                if (!enabled) return@pointerInput
                val reach = 22.dp.toPx()
                val step = 12.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    active = true
                    knobX = 0f; knobY = 0f
                    tapCb?.invoke(side)
                    val sx = down.position.x
                    val sy = down.position.y
                    var lastX = sx
                    var dir = ""
                    var maxD = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull { it.id == down.id }
                        if (ch == null) { stopRepeat(); active = false; knobX = 0f; knobY = 0f; break }
                        if (ch.changedToUpIgnoreConsumed()) {
                            stopRepeat()
                            active = false; knobX = 0f; knobY = 0f
                            if (maxD < step) pressCb?.invoke()
                            break
                        }
                        val px = ch.position.x
                        val py = ch.position.y
                        if (px != lastX) { dragCb?.invoke(px - lastX); lastX = px }
                        val x = px - sx
                        val y = py - sy
                        val d = hypot(x, y)
                        maxD = maxOf(maxD, d)
                        val k = if (d > reach) reach / d else 1f
                        knobX = x * k; knobY = y * k
                        if (stickCb != null) {
                            val nd = if (d >= step) {
                                if (abs(x) > abs(y)) (if (x > 0) "R" else "L") else (if (y > 0) "D" else "U")
                            } else if (d < step / 2) "" else dir
                            if (nd != dir) {
                                if (nd.isNotEmpty()) {
                                    val dx = if (nd == "R") 1 else if (nd == "L") -1 else 0
                                    val dy = if (nd == "D") 1 else if (nd == "U") -1 else 0
                                    stickCb?.invoke(dx, dy)
                                    startRepeat(dx, dy)
                                } else stopRepeat()
                            }
                            dir = nd
                        }
                        ch.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val reach = 22.dp.toPx()
            val kx = ax.value
            val ky = ay.value
            val c = center + Offset(kx, ky)
            val dist = min(reach, hypot(kx, ky))
            val pull = dist / reach
            val angle = if (dist > 0.4f) Math.toDegrees(atan2(ky, kx).toDouble()).toFloat() else 0f
            val base = 44.dp.toPx()
            val w = base * (1f + 0.06f * pull)
            val h = base * (1f - 0.05f * pull)
            val (gc, gs) = glowAt(phase.value)
            val borderC = lerp(Z.Primary, gc, 0.6f)
            val glow = if (active) 15.dp.toPx() else 8.dp.toPx()
            rotate(degrees = angle, pivot = c) {
                drawOval(
                    brush = Brush.radialGradient(
                        0f to Z.Primary.copy(alpha = 0.0f), 0.6f to Z.Primary.copy(alpha = 0.0f), 0.68f to Z.Primary.copy(alpha = 0.55f), 1f to Color.Transparent,
                        center = c, radius = w / 2f + glow,
                    ),
                    topLeft = Offset(c.x - w / 2f - glow, c.y - h / 2f - glow),
                    size = Size(w + 2 * glow, h + 2 * glow),
                )
                drawOval(
                    brush = Brush.radialGradient(
                        0f to Z.Primary.copy(alpha = 0.55f), 0.7f to Z.Primary.copy(alpha = 0.22f), 1f to Z.Primary.copy(alpha = 0.22f),
                        center = Offset(c.x - w * 0.15f, c.y - h * 0.2f), radius = w * 0.7f,
                    ),
                    topLeft = Offset(c.x - w / 2f, c.y - h / 2f), size = Size(w, h),
                )
                drawOval(
                    color = borderC.copy(alpha = 0.55f + 0.45f * gs),
                    topLeft = Offset(c.x - w / 2f, c.y - h / 2f), size = Size(w, h),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}
