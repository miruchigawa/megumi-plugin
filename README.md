# Megumi

Paper plugin for the **Axuint SMP** server. Integrates with [Trakteer.id](https://trakteer.id) for supporter listing and donation goal announcements.

## Features

- Announces donation goal progress on join.
- Lists recent supporters via `/supporter`.
- **Save Points**: Set personal marks to teleport back to later.
- **Player Teleportation**: Request to teleport to other players.
- **Warmups & Limits**: Group-based daily limits and movement-sensitive warmups.
- Async performance for database and API requests.

## Requirements

- **Server**: Paper/Spigot (1.21.1+)
- **Java**: Version 21+ (as of build config)
- **Dependency**: LuckPerms (highly recommended for limits)

## Installation

1. Build the plugin: `./gradlew shadowJar`.
2. Find the jar in `lib/build/libs/lib-all.jar`.
3. Drop it into your server's `plugins/` folder.
4. Restart the server.
5. Configure your Trakteer API key and limits in `plugins/megumi/config.yml`.

## Configuration

### trakteer
- `api-key`: Your Trakteer.id public API key.

### settings
- `cooldown_hours`: Reset window for attempt limits (default: 24).
- `teleport_warmup_seconds`: Delay before teleporting (default: 3).
- `request_expire_seconds`: How long `/tpx` requests last (default: 30).

### groups
Configure daily limits for save points and teleports.
- `max_savepoints`: Max locations a player can save.
- `max_teleports`: Max teleport attempts per cooldown window.

## Usage

### Commands

| Command | Alias | Description |
|---------|-------|-------------|
| `/supporter` | — | Shows the most recent Trakteer supporters. |
| `/sp <subcommand>` | `/savepoint` | Manage save points (save, go, list, delete, gui). |
| `/tpx <player>` | `/tpa` | Request to teleport to a player. |
| `/tpx accept/deny` | — | Respond to a pending teleport request. |
| `/tpx status` | — | Check your remaining daily teleport attempts. |
| `/tpxadmin <sub>` | — | Admin tools: reset attempts, set group, info. |

### Permissions

- `teleportx.use`: Basic access to `/sp` and `/tpx`.
- `teleportx.admin`: Access to `/tpxadmin`.
- `teleportx.bypass`: Unlimited save points and teleports.
- `teleportx.group.<name>`: Assign a specific limit group (supporter, admin, developer).

## Development

Build the project using Gradle:

```bash
./gradlew build        # Standard build
./gradlew shadowJar    # Build a fat-jar with dependencies (recommended for servers)
./gradlew test         # Run unit tests
```

## Project Structure

```
lib/
  src/main/kotlin/   Source code
  src/main/resources/ Default configuration and plugin metadata
gradle/              Gradle wrapper and version catalogs
```

## Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute to this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
