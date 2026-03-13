# Contributing to Megumi

We appreciate your help improving Megumi.

## Ways to Contribute

- **Report Bugs**: Let us know if something breaks.
- **Suggest Features**: Help us improve the plugin.
- **Docs**: Fix typos or clarify steps.
- **Code**: Fix bugs or add features.

## Development Setup

### Prerequisites

- **Java 21 JDK**
- **IntelliJ IDEA** (Recommended with Kotlin plugin)
- **Gradle** (Uses the included wrapper)

### Local Setup

1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/your-username/megumi.git
   cd megumi
   ```
3. Run the tests to ensure everything is working:
   ```bash
   ./gradlew test
   ```

## Branching Convention

Please use descriptive branch names:
- `feat/feature-name`
- `fix/bug-fix-name`
- `docs/documentation-changes`
- `chore/tooling-updates`

## Commit Message Convention

We follow a simple commit message format:
`type: short description in present tense`

Examples:
- `fix: correct balance formatting in join message`
- `feat: add configurable limit to supporter command`
- `docs: update installation steps in README`

## Pull Request Process

1. Ensure your code follows the existing style.
2. Verify that all tests pass (`./gradlew test`).
3. Update the `CHANGELOG.md` for any user-facing changes.
4. Open a Pull Request with a clear description of the change.
5. A maintainer will review your PR as soon as possible.

## What NOT to Contribute

- Breaking changes without prior discussion.
- Refactorings that only change style without improving performance or readability.
- Features that are outside the scope of Core Essentials or Trakteer.id integration.
