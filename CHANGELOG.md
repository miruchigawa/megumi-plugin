# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project structure for the **Axuint SMP** essential plugin.
- `/supporter` command to view recent Trakteer supporters.
- **Teleportation System**: Request-based player-to-player teleportation (`/tpx`).
- **Save Points**: Personal location management (`/sp`) with GUI support.
- **Group Limits**: Configurable daily limits for teleports and save points via LuckPerms.
- **Teleport warmups**: Movement-sensitive delays before teleporting.
- Automatic announcement of donation goal progress on player join.
- Kotlin Coroutines for asynchronous API requests.
- Created `config.yml` to store Trakteer API key and plugin settings.
- Initial project documentation (README, CONTRIBUTING, etc.).

### Fixed
- Moved hardcoded API key from `MegumiPlugin.kt` to configuration file.
