# AGENT instructions for jarvis repository

## Build and Test
- Use `./gradlew test` to run the unit test suite.
- Ensure tests pass before committing code.
- Use `./gradlew buildPlugin` if you need to assemble the plugin artifact.

## Code Style
- Kotlin sources are in `src/main/kotlin` and tests in `src/test/kotlin`.
- Use 4 spaces for indentation and keep lines under 120 characters.
- End every file with a single newline.
- Keep public functions and classes documented with KDoc comments when adding new code.

## Documentation
- Update `CHANGELOG.md` when adding or changing features.
- Update `README.md` if user-facing behaviour changes.

## Commit Messages and PRs
- Use concise commit messages describing what changed and why.
- Summarize important modifications in the PR description.

