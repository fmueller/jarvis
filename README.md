# Jarvis

![Build](https://github.com/fmueller/jarvis/workflows/Build/badge.svg?branch=main)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## About

<!-- Plugin description -->
Jarvis is an LLM-powered developer plugin for the JetBrains IDE platform. It aims to support developers by leveraging
local LLMs only. To achieve this, it is integrating with Ollama.
<!-- Plugin description end -->

## Installation

1. Install and run [Ollama](https://ollama.com)
2. Download LLama3 8B model in Ollama: ```ollama run llama3```
3. Install Jarvis plugin in your Jetbrains IDE:
   - Using the IDE built-in plugin system: <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "jarvis"</kbd> >
     <kbd>Install</kbd>
   - Manually: Download the [latest release](https://github.com/fmueller/jarvis/releases/latest) and install it manually using
     <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This project is licensed under the [MIT](LICENSE)
