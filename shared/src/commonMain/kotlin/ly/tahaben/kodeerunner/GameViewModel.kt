package ly.tahaben.kodeerunner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds the [GameState] and runs the game loop in a coroutine.
 * The UI only observes [state] and calls [onAction] / [togglePause]; all rules live in [GameState].
 */
class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var loop: Job? = null

    /** Space bar (or a tap) means different things depending on the phase. */
    fun onAction() = when (state.value.phase) {
        Phase.Welcome, Phase.GameOver -> start()
        Phase.Running -> _state.update { it.jump() }
        Phase.Paused -> togglePause()
    }

    /** The P key or the on-screen button flips between Running and Paused; other phases ignore it. */
    fun togglePause() = _state.update { current ->
        when (current.phase) {
            Phase.Running -> current.copy(phase = Phase.Paused)
            Phase.Paused -> current.copy(phase = Phase.Running)
            else -> current
        }
    }

    private fun start() {
        _state.value = GameState(phase = Phase.Running)
        loop?.cancel()
        loop = viewModelScope.launch {
            while (isActive && state.value.phase != Phase.GameOver) {
                delay(FRAME_MILLIS.milliseconds)
                // While paused the loop keeps waiting but the world does not move.
                if (state.value.phase == Phase.Running) _state.update { it.tick() }
            }
        }
    }

    private companion object {
        const val FRAME_MILLIS = 16L   // roughly 60 frames per second
    }
}
