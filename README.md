# Jarvis

[![Build](https://github.com/fmueller/jarvis/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/fmueller/jarvis/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/24755-jarvis.svg)](https://plugins.jetbrains.com/plugin/24755-jarvis)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24755-jarvis.svg)](https://plugins.jetbrains.com/plugin/24755-jarvis)

## About

<!-- Plugin description -->
Jarvis is an LLM-powered developer plugin for the JetBrains IDE platform. It aims to support developers by leveraging
local LLMs only. To achieve this, it is integrating with Ollama.
<!-- Plugin description end -->

## Installation

1. Install and run [Ollama](https://ollama.com)
2. Download LLama 3.1 8B model in Ollama: ```ollama run llama3.1```
3. Download LLama 3.2 8B model in Ollama: ```ollama run llama3.2```
4. Install Jarvis plugin in your Jetbrains IDE:
   - Using the IDE built-in plugin system: <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "jarvis"</kbd> >
     <kbd>Install</kbd>
   - Manually: Download the [latest release](https://github.com/fmueller/jarvis/releases/latest) and install it manually using
     <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This project is licensed under the [MIT](LICENSE)

## Help

Available commands:

- ```/help``` or ```/?``` - Shows this help message
- ```/new``` - Starts a new conversation
- ```/plain``` - Sends a chat message without code context
- ```/model <modelName>``` - Changes the model to use ('llama3.1' or 'llama3.2')
