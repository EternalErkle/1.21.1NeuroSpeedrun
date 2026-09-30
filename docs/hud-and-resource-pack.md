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

`ServerPack` registers the mod's `assets/` folder with Polymer. Polymer builds the pack at server start, writes `polymer/resource_pack.zip`, and computes its SHA-1 hash. Clients re-download only when the pack changes.

Contents, all under `speedrun-core/src/main/resources/assets/`:

| Asset | Purpose |
|---|---|
| `minecraft/textures/gui/sprites/boss_bar/white_background.png`, `white_progress.png` | Fully transparent 182x5 sprites. White bossbars show only their title text |
| `speedrun/font/face.json`, `speedrun/textures/font/pixel.png` | The pixel font for the 2D face |
| `minecraft/textures/gui/title/background/panorama_0.png` to `panorama_5.png` | Black panorama for the dimension-change screen |

Sounds are vanilla sound events, so the pack ships no sound files.

### Autohost config

Autohost is disabled by default on a production server. On first start, `ServerPack` writes `config/polymer/auto-host.json` with `enabled` and `required` set to true, and Polymer fills in the other fields. If that file already exists, it is left alone. In that case set `"enabled": true` by hand. The pack URL is derived from the address each client connected with. Behind a proxy or a different public hostname, set `settings.forced_address` to something like `http://play.example.com:25565`.

### Pixel font metrics

The face is 8 rows. Each row is 8 `` pixel glyphs with a `` between neighbors, and rows are separated by newlines.

- `pixel.png` is an 8x8 solid white square. The bitmap provider sets `height` 10, so it scales by 10/8 and draws 10x10 font units.
- A bitmap glyph advances by its drawn width plus 1, so the pixel advances 11. The space glyph advances -1. Each pixel therefore starts exactly 10 units after the previous one and touches it.
- Text displays step each line down by a fixed 10 units, the same as the pixel height, so rows touch too.
- `ascent` 7 puts the top of each pixel at the top of its line, so the face fills the display's 80-unit box exactly.

### Dimension-change screen

In 1.21.1, `ReceivingLevelScreen` draws the nether portal texture for vanilla nether travel and the end portal effect for vanilla end travel. Every other dimension change uses the title panorama, blurred, under the in-world menu background. Run worlds are Fantasy dimensions with their own keys, so every run teleport and the death room use the panorama. The pack makes all six panorama faces black. The title screen still shows the normal panorama, because the client drops server packs on disconnect.
