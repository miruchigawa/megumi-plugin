package moe.nanakura.megumi.data

import java.util.UUID

/** Represents a player's save point. */
data class SavePoint(
        val id: Int = 0,
        val owner: UUID,
        val name: String,
        val worldName: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float
)

/** Represents a teleport attempt. */
data class TeleportAttempt(val playerUuid: UUID, val timestamp: Long)

/** Represents player-specific settings and overrides. */
data class PlayerData(val uuid: UUID, var groupOverride: String? = null)
