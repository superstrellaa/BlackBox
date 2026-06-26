# BlackBox

BlackBox is a lightweight, client-side telemetry and crash reporting mod built for testing and event environments. It silently collects performance data, client configuration, and error information, then delivers it in real time to a Discord webhook — no external server or API required.

## What it does

BlackBox runs entirely on the client and captures a snapshot of the session whenever you disconnect from a server, close the game, or hit an uncaught exception. Each report includes:

- **Performance metrics**: average, minimum, and maximum FPS over the session, along with JVM memory usage
- **Hardware information**: operating system, CPU core count, system RAM, GPU renderer, and Java version
- **Client settings**: render distance, graphics mode, max FPS, VSync, particles, entity shadows, fullscreen state, and GUI scale
- **Environment details**: Minecraft version, Fabric Loader version, and the full list of installed mods
- **Crash data**: full stack trace and context whenever an uncaught exception occurs

## Why it exists

BlackBox was built to support structured testing for modded events. Rather than relying on testers to manually describe issues or performance problems, it automatically reports objective data the moment something goes wrong or a session ends — making it easier to correlate crashes, lag, and configuration across many players without running any backend infrastructure.

## Configuration

On first launch, BlackBox generates a configuration file where you set your own Discord webhook URL. No telemetry is sent until this is configured. The mod is intended for controlled testing environments and is not meant to run unattended on public servers without the knowledge of the players being monitored.

## Requirements

- Minecraft 1.21.1
- Fabric Loader
- Fabric API
- Java 21+