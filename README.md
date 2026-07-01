# Hidden Gems Deluxe

A match-3 style puzzle game built with [libGDX](https://libgdx.com/), featuring neon visuals, 3D gem effects (desktop), and dynamic animated backgrounds. Play in the browser or on desktop.

**Play online:** [hgd-game.com](https://hgd-game.com)

![Hidden Gems Deluxe](https://i.postimg.cc/5tjSHYRB/hgd.png)

## About the Game

**Hidden Gems Deluxe** is a gem-matching puzzle where you clear the board by matching three or more gems of the same color. The game combines classic match-3 mechanics with a distinctive neon aesthetic:

- **3D gems (desktop)** — On desktop, gems are rendered in 3D with neon-style materials and lighting. The web build uses a 2D fallback because WebGL in the browser does not support the full 3D pipeline.
- **Animated background** — A living gradient with drifting orbs and subtle motion keeps the playfield visually rich.
- **Semi-transparent board** — The game board sits over the background with a clear but non-distracting look.
- **Modern UI** — Menus and overlays use neon-styled text with glow effects; the main/pause menu has pill-shaped lime buttons with 3D press and hover feedback.
- **Progression** — Level up as you play; at level 12, pink gems are unlocked for more variety.

### Controls

- **Arrow keys** — Move the cursor and drop gems.
- **Down arrow** — Soft drop (faster fall) or hard drop when pressed during fall.
- **Pause** — Access the main/pause menu from in-game.

## Platforms

- **core** — Shared game logic, rendering, and assets for all platforms.
- **lwjgl3** — Desktop (Windows, macOS, Linux) using LWJGL3.
- **html** — Web (browser) using [gdx-teavm](https://github.com/xpenatan/gdx-teavm) / TeaVM. Scores are stored in the browser via `localStorage`.

## Getting Started

This project uses [Gradle](https://gradle.org/) (with wrapper). Useful commands:

### Desktop

- **Run the game:** `./gradlew lwjgl3:run` (or `gradlew.bat lwjgl3:run` on Windows)
- **Build runnable JAR:** `./gradlew lwjgl3:jar` — output in `lwjgl3/build/libs`

### Web

- **Run in the browser (build + local server):** `./gradlew html:runWeb` (or `gradlew.bat html:runWeb` on Windows)
- **Build static web output only:** `./gradlew html:buildWeb` — output in `html/build/dist/webapp/`

Open the served URL in your browser after `runWeb`, or host the contents of `html/build/dist/webapp/` on any static file server.

### General

- **Build everything:** `./gradlew build`
- **Clean:** `./gradlew clean`

Other Gradle options: `--continue`, `--daemon`, `--offline`, `--refresh-dependencies`. Project-specific tasks include `lwjgl3:run`, `html:runWeb`, `html:buildWeb`, and `core:clean`.

## Donate

If you enjoy Hidden Gems Deluxe and want to support development, you can donate to:

| Network | Address |
|---------|---------|
| **BTC** | `bc1qzsuu2zygp8j9qlftjq2fdu73932zxftax0ggvl` |
| **ETH** | `0xF868B80958AC6BDce638E90bA722E73AC0f919C5` |
| **BNB** | `0xF868B80958AC6BDce638E90bA722E73AC0f919C5` |
| **SOL** | `3tYQcU5rSEWJBoDrocyqkjvjM1zji6f6ZY4ygetP9CAj` |
| **TRX** | `TXosSwbddYbJ2PwJ9DsR9xvVhfKyX82pev` |

## Project Structure

Generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff). Application entry and shared code live in `core`; `lwjgl3` launches the desktop version and `html` builds the browser version.
