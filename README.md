# Tic Tac Toe — Neon Pro Dashboard

## Abstract
Tic Tac Toe (Neon Pro Dashboard edition) is a Java Swing desktop game that reimagines classic Tic Tac Toe as a full control-center application. Beyond the 3×3 grid, it ships a live sidebar dashboard, an unbeatable-to-easy AI opponent, undo/hint/analyze tooling, five switchable color themes, and custom-painted UI components (glow buttons, gradient panels, pill toggles) built entirely with Java2D — no external UI library.

## Features
- Two modes: **2 Player** (local human vs human) and **vs Computer**
- **Difficulty levels**: Easy, Medium, Hard, Impossible (Pro) — tunes how often the AI plays randomly vs optimally
- **Minimax AI** with perfect play at max difficulty
- **Undo** — reverts the last move (and the AI's reply move automatically, in vs-Computer mode)
- **Hint** (3 uses/game) — flashes the best move for the current player
- **AI Suggest** (2 uses/game) — same engine, styled as an assist rather than a hint
- **Analyze** — reports the perfect-play outcome (win/draw) from the current board state
- Live **Game Status** card, running **Stats** (games played, wins per player, draws), **Win Conditions** legend, and a **Last 3 Moves** log
- **5 color themes** (Aurora, Blossom, Ember, Lagoon, Prism), swapped live without restarting
- **Sound toggle** synced between the top bar icon and footer switch
- Animated wins: glowing win-line pulse + confetti burst
- Hover ghost-preview of the current player's mark on empty cells

## Gameplay
- Board is a 3×3 grid (9 cells), players alternate as **O** (goes first) and **X**.
- Three in a row — row, column, or diagonal — wins.
- In vs-Computer mode, the human always plays O; the computer plays X and moves automatically after the human's turn.
- Difficulty controls the AI's random-move probability:

| Difficulty       | Random-move chance |
|------------------|---------------------|
| Easy             | 85%                 |
| Medium           | 50%                 |
| Hard             | 15%                 |
| Impossible (Pro) | 0% (pure minimax)   |

## Technical Concepts Demonstrated
- **Minimax algorithm**: `bestMoveFor()` / `minimax()` — generalized for either player, powers the AI, Hint, AI Suggest, and Analyze features
- **State tracking**: `MoveRecord` history list drives Undo and the Last Moves log
- **Custom Swing components**: `GlowButton`, `GradientPanel`, `GradientTitleLabel`, `PillToggle`, `FlatButton`, `RoundedLineBorder`, `ScoreCard` — all hand-painted with Java2D (`Graphics2D`, `RoundRectangle2D`, `Ellipse2D`)
- **Animation**: `javax.swing.Timer`-driven glow pulsing and a particle-based confetti overlay installed as the frame's glass pane
- **Theming**: mutable static `Color` palette re-applied at runtime via `applyTheme()`, backed by an immutable `ThemeDef` array
- **Enums as typed config**: `Player` (with `other()`) and `Difficulty` (label + random-weight pair)
- **Easing functions**: `easeOutCubic()` for smoother animation curves

## Project Structure
```
├── Tic_Tac_Toe.java                     # Main class — game logic, UI, AI
├── Tic_Tac_Toe_Player.class
├── Tic_Tac_Toe_ScoreCard.class
├── Tic_Tac_Toe_FlatButton.class / _1.class
├── Tic_Tac_Toe_GlowButton.class / _1.class
├── Tic_Tac_Toe_GradientPanel.class
├── Tic_Tac_Toe_GradientTitleLabel.class
├── Tic_Tac_Toe_PillToggle.class
├── Tic_Tac_Toe_RoundedLineBorder.class
├── Tic_Tac_Toe_AffineTransformSafe.class
└── README.md
```

## Dependencies
- Java SE 8+ (uses `javax.swing`, `java.awt.geom` — standard library only, no third-party packages)
- Nimbus Look and Feel (bundled with the JDK; applied at startup if available, falls back silently otherwise)

## Build
```bash
javac -d out Tic_Tac_Toe.java
```

## Run
```bash
java -cp out com.mycompany.tic_tac.Tic_Tac_Toe
```
Or run directly from your IDE (NetBeans/IntelliJ/Eclipse) by executing `Tic_Tac_Toe.main()`.
