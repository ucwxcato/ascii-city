# ASCII City generated audio provenance

The project owner generated and supplied both instrumental source tracks specifically for ASCII City on 2026-08-19 using ACE-Step 1.5. The tracks contain no vocals and are bundled with the owner's authorization.

| Runtime asset | Source master | Intended use |
|---|---|---|
| `audio/neon-city-loop.ogg` | `audio/source/neon-city-master.mp3` | General city exploration ambience |
| `audio/signal-garden-loop.ogg` | `audio/source/signal-garden-master.mp3` | Spawn-park ambience |
| `audio/footstep-a.ogg` | Procedurally synthesized project asset | Superseded movement cue retained for provenance |
| `audio/footstep-b.ogg` | Procedurally synthesized project asset | Superseded movement cue retained for provenance |
| `audio/ambient-vent-hiss.ogg` | Procedurally synthesized filtered pink noise | Positional vent micro-event |
| `audio/ambient-electric-spark.ogg` | Procedurally synthesized filtered noise and sine transient | Positional electrical micro-event |
| `audio/ambient-distant-horn.ogg` | Procedurally synthesized dual-sine tone | Positional distant traffic micro-event |

The source masters were generated from original descriptive prompts for a low-intensity cyberpunk city and neon park. No reference audio is included in this repository.

The runtime music OGG files were derived locally with FFmpeg 9.0. Each source was normalized to a restrained common level, converted to stereo 48 kHz Vorbis, and rebuilt with a four-second tail-to-intro crossfade. The legacy mono 48 kHz footstep cues were synthesized locally from filtered noise and low-frequency tones without third-party samples; the current surface-specific footsteps are documented separately in `PIXABAY-FOOTSTEPS.md`. FFmpeg was used only as a temporary authoring tool and is not distributed with the game.

The three `ambient-*` cues were synthesized locally on 2026-08-25 with FFmpeg 9.0 from generated noise and sine sources, filtered and faded into short mono 48 kHz Vorbis files. They contain no recorded samples or third-party source audio.

ACE-Step 1.5's repository is published under the MIT License and asks users to verify originality and disclose AI involvement. That software license should not be confused with a separate copyright determination for generated output. Preserve this provenance record and review the tracks for unintended similarity before a public release.

- ACE-Step 1.5: <https://github.com/ace-step/ACE-Step-1.5>
- FFmpeg: <https://ffmpeg.org/>
