# Megumi

An essential Minecraft Paper plugin for the **Axuint SMP** server that integrates with [Trakteer.id](https://trakteer.id) to announce donation goals and list supporters.

## Features

- Announces the current donation goal progress when players join the server.
- Lists recent supporters via the `/supporter` command.
- Asynchronous API requests to ensure server performance remains unaffected.
- Configurable Trakteer API key.

## Requirements

- **Minecraft Server**: Paper, Spigot, or compatible (1.21.1+)
- **Java**: Version 21 or higher

## Installation

1. Clone the repository and build the plugin using Gradle (see [Development](#development)).
2. Place the generated jar file (from `lib/build/libs/lib-all.jar`) into your server's `plugins` directory.
3. Restart the server to generate the default configuration.
4. Edit `plugins/megumi/config.yml` and add your Trakteer API key.
5. Restart or reload the plugin to apply changes.

## Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `trakteer.api-key` | Yes | — | Your Trakteer.id Public API Key |

## Usage

### Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/supporter` | None | Shows the most recent Trakteer supporters |

## Development

Build the project using Gradle:

```bash
./gradlew build        # Standard build
./gradlew shadowJar    # Build a fat-jar with dependencies included
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
