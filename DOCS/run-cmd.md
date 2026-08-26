# Run commands

Use the checked-in Gradle wrapper from the repository root. On Windows PowerShell, replace `./gradlew` with `./gradlew.bat`.

## Build and validation

```bash
./gradlew clean check
./gradlew :core:validateContent
./gradlew :core:test
./gradlew :core:test --tests com.asciicity.render.WorldAsciiRendererTest
./gradlew --stop
```

`--stop` is useful after closing a desktop run if Windows has held a build artifact open.

## Launch the game

```bash
./gradlew gameRun
./gradlew desktop:run
```

The normal desktop launch uses the Fine 6×12 world surface by default. UI text stays on the 10×18 Standard font.

### World-density profiles

```bash
./gradlew desktop:run --args="--ascii-density=fine"
./gradlew desktop:run --args="--ascii-density=dense"
./gradlew desktop:run --args="--ascii-density=standard"
```

`fine` is the default; the other two commands are useful for performance comparison or fallback testing.

### Map, window, and camera setup

```bash
./gradlew desktop:run --args="--map=city-alpha"
./gradlew desktop:run --args="--map=water-park"
./gradlew desktop:run --args="--map=nature-reserve"
./gradlew desktop:run --args="--map=casino"
./gradlew desktop:run --args="--window-size=1280x720"
./gradlew desktop:run --args="--window-size=3440x1440"
./gradlew desktop:run --args="--view-yaw=90 --view-pitch=0"
./gradlew desktop:run --args="--view-x=28.5 --view-z=13.5"
```

`--view-x` and `--view-z` must be supplied together and should name a valid authored location.

### Smoke checks and screenshots

```bash
./gradlew desktop:run --args="--smoke-test"
./gradlew desktop:run --args="--smoke-test --no-audio --no-head-bob"
./gradlew desktop:run --args="--smoke-test --ascii-density=fine --window-size=1280x720 --screenshot=/absolute/path/city.png"
./gradlew desktop:run --args="--smoke-test --map=casino --no-audio --screenshot=/absolute/path/casino.png"
```

Use an absolute screenshot path. `--no-audio` disables the audio backend; `--no-head-bob` removes camera bob for repeatable captures.

### Scripted runtime verification

```bash
./gradlew desktop:run --args="--verify-walk --no-audio --no-head-bob"
./gradlew desktop:run --args="--verify-interaction --no-audio --no-head-bob"
./gradlew desktop:run --args="--verify-casino-entry --no-audio --no-head-bob"
./gradlew desktop:run --args="--verify-casino-return --no-audio --no-head-bob"
./gradlew desktop:run --args="--verify-presentation --no-audio --no-head-bob"
```

Choose at most one `--verify-*` route per launch. Add `--screenshot=/absolute/path/output.png` to retain the route's configured capture frame.

### Development-only asset overrides

```bash
./gradlew desktop:run --args="--font-path=fonts/vt323-10x18.fnt"
./gradlew desktop:run --args="--palette-path=data/palette.json"
```

These are useful for testing controlled asset-load failures or alternate local assets. Normal runs should use bundled assets.

## In-game keys

- `W` / `S`: forward / back
- `A` / `D`: strafe left / right
- Arrow keys: look
- `E` or Enter: interact
- `Escape`: cancel
- `F3`: diagnostics and debug footprint; `Tab` cycles map and `C` toggles CRT world presentation while diagnostics are open
- `M`: toggle city/park music only
