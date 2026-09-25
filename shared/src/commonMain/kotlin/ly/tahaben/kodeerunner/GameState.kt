package ly.tahaben.kodeerunner

import kotlin.random.Random

/** Fixed logical world size; the screen scales this to fit whatever window it runs in. */
object World {
    const val WIDTH = 800f
    const val HEIGHT = 300f
    const val GROUND_Y = 250f
    const val PLAYER_X = 60f
    const val PLAYER_WIDTH = 60f
    const val PLAYER_HEIGHT = 50f
    const val HITBOX_INSET = 8f          // hitbox is smaller than the sprite so near misses feel fair
    const val GRAVITY = 0.8f
    const val JUMP_VELOCITY = 14f
}

/** The screens of the game. An enum keeps `when` expressions exhaustive. */
enum class Phase { Welcome, Running, Paused, GameOver }

/** A cactus-like block the player has to jump over. Placeholder until real art arrives. */
data class Obstacle(val x: Float, val width: Float = 20f, val height: Float = 40f)

/** A background landmark (cloud placeholder) that scrolls slower than the ground. */
data class Landmark(val x: Float, val y: Float)

/**
 * Immutable snapshot of the whole game. Every frame produces a new copy via [tick],
 * which shows off data classes, `copy`, default arguments and expression bodies.
 */
data class GameState(
    val phase: Phase = Phase.Welcome,
    val score: Int = 0,
    val playerY: Float = 0f,          // height above the ground
    val velocityY: Float = 0f,
    val speed: Float = 6f,
    val obstacles: List<Obstacle> = emptyList(),
    val landmarks: List<Landmark> = listOf(Landmark(200f, 60f), Landmark(550f, 40f)),
    val ticksUntilSpawn: Int = 60,
) {
    val isOnGround: Boolean get() = playerY <= 0f

    /** Only jump when standing; otherwise return `this` unchanged. */
    fun jump(): GameState = if (isOnGround) copy(velocityY = World.JUMP_VELOCITY) else this

    /** Advance the simulation by one frame. */
    fun tick(): GameState {
        val nextVelocity = velocityY - World.GRAVITY
        val nextY = (playerY + nextVelocity).coerceAtLeast(0f)

        val movedObstacles = obstacles
            .map { it.copy(x = it.x - speed) }
            .filter { it.x + it.width > 0f }
        val spawned = if (ticksUntilSpawn <= 0) movedObstacles + randomObstacle() else movedObstacles

        val movedLandmarks = landmarks.map { landmark ->
            val x = landmark.x - speed / 3
            if (x < -80f) Landmark(World.WIDTH + Random.nextInt(0, 200), Random.nextInt(30, 90).toFloat())
            else landmark.copy(x = x)
        }

        val crashed = spawned.any { it.hits(nextY) }
        return copy(
            phase = if (crashed) Phase.GameOver else Phase.Running,
            score = score + 1,
            playerY = nextY,
            velocityY = if (nextY == 0f) 0f else nextVelocity,
            speed = speed + 0.001f,               // gets a little harder over time
            obstacles = spawned,
            landmarks = movedLandmarks,
            ticksUntilSpawn = if (ticksUntilSpawn <= 0) Random.nextInt(50, 110) else ticksUntilSpawn - 1,
        )
    }

    private fun randomObstacle() = Obstacle(
        x = World.WIDTH,
        height = Random.nextInt(30, 60).toFloat(),
    )
}

/**
 * Extension function: box overlap between the player and this obstacle.
 * The sprite cell includes the arms and empty corners, so the box is inset on every side.
 */
fun Obstacle.hits(playerY: Float): Boolean {
    val inset = World.HITBOX_INSET
    val playerLeft = World.PLAYER_X + inset
    val playerRight = World.PLAYER_X + World.PLAYER_WIDTH - inset
    val playerBottom = World.GROUND_Y - playerY - inset
    val obstacleTop = World.GROUND_Y - height
    return playerLeft < x + width && playerRight > x && playerBottom > obstacleTop
}
