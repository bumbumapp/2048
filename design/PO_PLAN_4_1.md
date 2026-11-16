# 2048 — Product plan for Huawei rule 4.1 (2026-11-16)

## What exists (10.5.0)
One screen: a 4x4 board, score/best/max/time tiles, pause, undo (1 step), restart, endless mode after 2048.
Single save slot. No modes, no stats, no themes. Reviewer sees "one simple thing".

## Competitor scan (2048 clones on AppGallery/Play)
Top clones offer: board sizes (3x3..8x8), time/move-limited variants, daily puzzle, undo, themes, stats, achievements.

## New features (all offline, no new permissions, no data collection)
1. **Mode hub (home screen)** - six playable modes, each with its own rules, goal and save slot:
   Classic (board 3x3 to 8x8), Time Attack (256 in 4:00), Limited Moves (128 in 100 moves),
   Daily Challenge (seeded 4x4, identical for everybody that UTC day, streak), Blocks (5x5 with stone obstacles, reach 1024), Zen (unlimited undo, no game over pressure).
2. **Statistics + 2048-style achievements** - per-mode games/wins/best score/best tile, daily streak,
   12 achievements with progress bars.
3. **Undo system + save/continue per mode** - limited undo per mode (unlimited in Zen), every mode keeps its own
   unfinished game across app restarts and process death; Continue card on home.
4. **Themes** - Bento Light, Night, Pastel, High Contrast tile skins + haptics toggle.

## Engine
Pure Java (repo is Java; no Kotlin plugin): `engine/` package with seeded PRNG, board with walls, move/merge-once,
spawn, win/lose, snapshots/undo, text serialization. JUnit tests (moves, merges once, walls, spawn, game over,
daily reproducibility, undo, save/restore).

## Not added (decision)
Rewarded ads: skipped (extra policy + Petal reward API risk); undo limits are fixed per mode.
