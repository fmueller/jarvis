# Guidelines for AI Agents

## Build and Test

- Use `gradle build` to build the whole project.
- Use `gradle check` to run all tests.
- Ensure tests pass before committing code.

## Code Style

- Kotlin sources are in `src/main/kotlin` and tests in `src/test/kotlin`.
- Use 4 spaces for indentation and keep lines under 120 characters.
- End every file with a single newline.
- Do not add dependencies. Only use the available libraries.
- Keep public functions and classes documented with KDoc comments when adding new code.
- Try to add unit tests for changes as you see fit.

## Documentation

- Update `README.md` if user-facing behaviour changes.
- Update `CHANGELOG.md` when adding or changing features.

## Commit Messages and PRs

- Use concise commit messages describing what changed and why.
- Use conventional commits, e.g.:
    - `feat: add feature xyz`
    - `refactor: move files to domain folder xyz`
    - `docs: improve docs about data from report xyz`
    - `chore: cleanup files`
- Summarize important modifications in the PR description.
