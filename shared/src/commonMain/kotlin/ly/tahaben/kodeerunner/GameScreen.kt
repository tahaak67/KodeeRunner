package ly.tahaben.kodeerunner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kodeerunner.shared.generated.resources.Res
import kodeerunner.shared.generated.resources.kodee_loving
import kodeerunner.shared.generated.resources.obstacle
import org.jetbrains.compose.resources.imageResource

/** Placeholder colors for things that still have no art. */
private val SkyColor = Color(0xFFF7F7F7)
private val GroundColor = Color(0xFF535353)
private val LandmarkColor = Color(0xFFCFCFCF)

private const val Disclaimer =
    "Kodee by JetBrains s.r.o. is licensed under CC BY 4.0. All rights reserved.\n" +
        "This game is for learning and exploring purposes only."

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel { GameViewModel() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    // Decoded once and cached by the resource loader, then reused every frame.
    val player = imageResource(Res.drawable.kodee_loving)
    val obstacle = imageResource(Res.drawable.obstacle)

    // Grab keyboard focus once so the space bar works without clicking first.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyColor)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.Spacebar -> viewModel.onAction()
                    Key.P -> viewModel.togglePause()
                    else -> return@onKeyEvent false
                }
                true
            }
            .pointerInput(Unit) { detectTapGestures { viewModel.onAction() } },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawWorld(state, player, obstacle) }

        Text(
            text = "Score: ${state.score}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        )

        // Transparent pause button for touch screens; the P key does the same on a keyboard.
        if (state.phase == Phase.Running || state.phase == Phase.Paused) {
            TextButton(
                onClick = viewModel::togglePause,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).alpha(0.5f),
            ) { Text(if (state.phase == Phase.Paused) "Resume" else "Pause") }
        }

        when (state.phase) {
            Phase.Welcome -> Overlay("Welcome to Kodee Runner!", "Press SPACE (or tap) to start", Disclaimer)
            Phase.Paused -> Overlay("Paused", "Press P (or tap) to resume", Disclaimer)
            Phase.GameOver -> Overlay("Game over! Score: ${state.score}", "Press SPACE (or tap) to try again")
            Phase.Running -> Unit
        }
    }
}

@Composable
private fun Overlay(title: String, hint: String, disclaimer: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(hint, style = MaterialTheme.typography.bodyLarge)
        if (disclaimer != null) {
            Text(
                text = disclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

/** Draws the world in logical units and lets [scale] stretch it to the real canvas size. */
private fun DrawScope.drawWorld(state: GameState, player: ImageBitmap, obstacle: ImageBitmap) {
    scale(scaleX = size.width / World.WIDTH, scaleY = size.height / World.HEIGHT, pivot = Offset.Zero) {
        drawLine(GroundColor, Offset(0f, World.GROUND_Y), Offset(World.WIDTH, World.GROUND_Y), strokeWidth = 2f)

        state.landmarks.forEach { drawRoundRect(LandmarkColor, Offset(it.x, it.y), Size(70f, 24f)) }

        state.obstacles.forEach {
            drawImage(
                image = obstacle,
                dstOffset = IntOffset(it.x.toInt(), (World.GROUND_Y - it.height).toInt()),
                dstSize = IntSize(it.width.toInt(), it.height.toInt())
            )
        }

        // One still image of the character.
        val playerTop = World.GROUND_Y - World.PLAYER_HEIGHT - state.playerY
        drawImage(
            image = player,
            dstOffset = IntOffset(World.PLAYER_X.toInt(), playerTop.toInt()),
            dstSize = IntSize(World.PLAYER_WIDTH.toInt(), World.PLAYER_HEIGHT.toInt()),
        )
    }
}
