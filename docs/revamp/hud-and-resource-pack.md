# HUD and Resource Pack

## Top-of-screen text

A line of text sits at the top of the screen at all times, with no background behind it:

```
Attempt #42 · 00:13:07 · Best 00:41:55
```

It is a bossbar. The resource pack makes one bossbar color's texture fully transparent, so only the title text shows. The dragon's health bar uses a different color and still renders normally, just below the text.

- The timer shows real elapsed time, not in-game time. At higher tick rates it still matches a stopwatch.
- The best time shown is the record for the current category (tick rate plus modifiers).
- While splits are active, the timer turns green when ahead of the best run's pace and red when behind.
- A second transparent bossbar line appears under the first when there is something to show: the boss checklist in the `allbosses` goal, then any active modifiers.
- In the lobby, the line reads `Lobby · /start to begin`, or `Generating...` while the next worlds are still being built.

## Death sidebar

The right side of the screen shows a vanilla sidebar scoreboard titled `Deaths`. It lists how many times each online player has died since the last win.

- Only online players are listed. A player's row disappears when they leave and returns with the same count when they rejoin.
- Counts reset to zero when a run is won.
- Counts are saved in the stats file, so they survive restarts.

## Face rendering

The death room shows a flat image of the dead player's face, built from text:

1. The resource pack defines a custom font containing one glyph, a solid square pixel with spacing set so neighboring glyphs touch.
2. `FaceCache` reads each player's skin, takes the 8x8 face region, and draws the 8x8 hat region over it wherever the hat pixel is not transparent.
3. The face becomes a text component of 8 lines of 8 glyphs, each glyph colored with one pixel's RGB value.
4. A text display entity renders it with no background, no shadow, and a fixed facing toward the camera.

Players whose skin cannot be fetched get the default Steve or Alex face, matching their model.

## Resource pack

The pack is served by Polymer's autohost module over the Minecraft port. No extra port is forwarded. It is required, so clients receive it on join.

Contents:

| Asset | Purpose |
|---|---|
| Transparent bossbar texture | Background-free top-of-screen text |
| Pixel font | The 2D face |
| Black loading screen | Hides the brief dimension-change screen |
| Death sound | Plays when a run fails |
| Corner hit sound | Plays when the face lands in a corner |
| New record sound | Plays when a run sets a category record |

Pack sources live in `speedrun-core/src/main/resources/pack/`. The build zips them, and the SHA-1 hash is computed at build time so clients re-download only when the pack changes.

Retexturing the dimension-change loading screen is checked in Phase 0. If 1.21.1 draws it from a texture the pack cannot override, the brief flash stays as is.
