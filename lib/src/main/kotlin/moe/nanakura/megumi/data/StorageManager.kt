package moe.nanakura.megumi.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import moe.nanakura.megumi.MegumiPlugin

class StorageManager(private val plugin: MegumiPlugin) {

    private val dataSource: HikariDataSource
    private val executor = Executors.newFixedThreadPool(4)

    init {
        val databaseFile = File(plugin.dataFolder, "database.db")
        if (!databaseFile.exists()) {
            databaseFile.parentFile.mkdirs()
        }

        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"
        config.driverClassName = "org.sqlite.JDBC"
        config.poolName = "MegumiPool"
        config.maximumPoolSize = 10
        config.connectionTimeout = 10000
        config.idleTimeout = 600000
        config.maxLifetime = 1800000

        dataSource = HikariDataSource(config)
        createTables()
    }

    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                        """
                    CREATE TABLE IF NOT EXISTS save_points (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner_uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        world_name TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        UNIQUE(owner_uuid, name)
                    )
                """.trimIndent()
                )

                stmt.execute(
                        """
                    CREATE TABLE IF NOT EXISTS teleport_attempts (
                        player_uuid TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                stmt.execute(
                        """
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid TEXT PRIMARY KEY,
                        group_override TEXT
                    )
                """.trimIndent()
                )

                stmt.execute(
                        "CREATE INDEX IF NOT EXISTS idx_teleport_attempts_uuid ON teleport_attempts(player_uuid)"
                )
                stmt.execute(
                        "CREATE INDEX IF NOT EXISTS idx_teleport_attempts_time ON teleport_attempts(timestamp)"
                )
            }
        }
    }

    private fun getConnection(): Connection {
        return dataSource.connection
    }

    fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
        executor.shutdown()
    }

    // --- Save Point Operations ---

    fun saveSavePoint(savePoint: SavePoint): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        """
                    INSERT OR REPLACE INTO save_points (owner_uuid, name, world_name, x, y, z, yaw, pitch)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                                )
                                .use { ps ->
                                    ps.setString(1, savePoint.owner.toString())
                                    ps.setString(2, savePoint.name)
                                    ps.setString(3, savePoint.worldName)
                                    ps.setDouble(4, savePoint.x)
                                    ps.setDouble(5, savePoint.y)
                                    ps.setDouble(6, savePoint.z)
                                    ps.setFloat(7, savePoint.yaw)
                                    ps.setFloat(8, savePoint.pitch)
                                    ps.executeUpdate()
                                }
                    }
                },
                executor
        )
    }

    fun deleteSavePoint(ownerUuid: UUID, name: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        "DELETE FROM save_points WHERE owner_uuid = ? AND name = ?"
                                )
                                .use { ps ->
                                    ps.setString(1, ownerUuid.toString())
                                    ps.setString(2, name)
                                    ps.executeUpdate()
                                }
                    }
                },
                executor
        )
    }

    fun getSavePoints(ownerUuid: UUID): CompletableFuture<List<SavePoint>> {
        return CompletableFuture.supplyAsync(
                {
                    val savePoints = mutableListOf<SavePoint>()
                    getConnection().use { conn ->
                        conn.prepareStatement("SELECT * FROM save_points WHERE owner_uuid = ?")
                                .use { ps ->
                                    ps.setString(1, ownerUuid.toString())
                                    ps.executeQuery().use { rs ->
                                        while (rs.next()) {
                                            savePoints.add(
                                                    SavePoint(
                                                            rs.getInt("id"),
                                                            UUID.fromString(
                                                                    rs.getString("owner_uuid")
                                                            ),
                                                            rs.getString("name"),
                                                            rs.getString("world_name"),
                                                            rs.getDouble("x"),
                                                            rs.getDouble("y"),
                                                            rs.getDouble("z"),
                                                            rs.getFloat("yaw"),
                                                            rs.getFloat("pitch")
                                                    )
                                            )
                                        }
                                    }
                                }
                    }
                    savePoints
                },
                executor
        )
    }

    // --- Teleport Attempt Operations ---

    fun addTeleportAttempt(uuid: UUID, timestamp: Long): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        "INSERT INTO teleport_attempts (player_uuid, timestamp) VALUES (?, ?)"
                                )
                                .use { ps ->
                                    ps.setString(1, uuid.toString())
                                    ps.setLong(2, timestamp)
                                    ps.executeUpdate()
                                }
                    }
                },
                executor
        )
    }

    fun getTeleportAttempts(uuid: UUID, since: Long): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        "SELECT COUNT(*) FROM teleport_attempts WHERE player_uuid = ? AND timestamp > ?"
                                )
                                .use { ps ->
                                    ps.setString(1, uuid.toString())
                                    ps.setLong(2, since)
                                    ps.executeQuery().use { rs ->
                                        if (rs.next()) {
                                            rs.getInt(1)
                                        } else 0
                                    }
                                }
                    }
                },
                executor
        )
    }

    fun getOldestAttemptWithinWindow(uuid: UUID, since: Long): CompletableFuture<Long?> {
        return CompletableFuture.supplyAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        "SELECT MIN(timestamp) FROM teleport_attempts WHERE player_uuid = ? AND timestamp > ?"
                                )
                                .use { ps ->
                                    ps.setString(1, uuid.toString())
                                    ps.setLong(2, since)
                                    ps.executeQuery().use { rs ->
                                        if (rs.next()) {
                                            val time = rs.getLong(1)
                                            if (time == 0L) null else time
                                        } else null
                                    }
                                }
                    }
                },
                executor
        )
    }

    fun clearTeleportAttempts(uuid: UUID): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement("DELETE FROM teleport_attempts WHERE player_uuid = ?")
                                .use { ps ->
                                    ps.setString(1, uuid.toString())
                                    ps.executeUpdate()
                                }
                    }
                },
                executor
        )
    }

    fun cleanOldAttempts(before: Long): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement("DELETE FROM teleport_attempts WHERE timestamp < ?")
                                .use { ps ->
                                    ps.setLong(1, before)
                                    ps.executeUpdate()
                                }
                    }
                },
                executor
        )
    }

    // --- Player Data Operations ---

    fun getPlayerData(uuid: UUID): CompletableFuture<PlayerData> {
        return CompletableFuture.supplyAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        "SELECT group_override FROM player_data WHERE uuid = ?"
                                )
                                .use { ps ->
                                    ps.setString(1, uuid.toString())
                                    ps.executeQuery().use { rs ->
                                        if (rs.next()) {
                                            PlayerData(uuid, rs.getString("group_override"))
                                        } else {
                                            PlayerData(uuid)
                                        }
                                    }
                                }
                    }
                },
                executor
        )
    }

    fun savePlayerData(playerData: PlayerData): CompletableFuture<Void> {
        return CompletableFuture.runAsync(
                {
                    getConnection().use { conn ->
                        conn.prepareStatement(
                                        "INSERT OR REPLACE INTO player_data (uuid, group_override) VALUES (?, ?)"
                                )
                                .use { ps ->
                                    ps.setString(1, playerData.uuid.toString())
                                    ps.setString(2, playerData.groupOverride)
                                    ps.executeUpdate()
                                }
                    }
                },
                executor
        )
    }
}
