# Jarvis Changelog

## [Unreleased]

## [1.5.0] - 2024-09-12

### Added

- Plain chat command to send a message without code context

### Changed

- Always add selected code to the prompt, even if no flag is provided

## [1.4.0] - 2024-09-11

### Added

- Moving token window to respect the context limit

### Changed

- Use LangChain4j as the default LLM framework

## [1.3.0] - 2024-08-20

### Fixed

- Compatibility with the latest Jetbrains Platform version

## [1.2.0] - 2024-08-14

### Added

- Streaming updates from the LLM
- Sensible timeouts for the Ollama client

### Changed

- Use LLama 3.1 model

## [1.1.0] - 2024-07-12

### Added

- Support for selected code in prompts

## [1.0.0] - 2024-06-28

### Added

- Ollama integration to query LLMs
- Chat interface to list messages and enter a new message
- Support for rendering Markdown
- Support for rendering code blocks with syntax highlighting
- Support for slash commands
- Command to start a new conversation
- Help command to list all available commands
- Support for different themes
