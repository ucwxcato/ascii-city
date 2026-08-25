# ASCII City assets

The desktop `run` task uses this directory as its working directory so LibGDX internal asset paths behave the same during development and packaging.

- `data/palette.json`: named runtime colors.
- `data/presentation.json`: validated fog, contrast, face-shading, pulse, and reduced-flicker tuning.
- `data/materials.json`: glyph ramps, palette references, and animation channels.
- `data/cells.json`: semantic map-token definitions.
- `data/dialogue.json`: validated dialogue records reserved for the narrative phase.
- `maps/`: fixed playable footprints, metadata (including optional whole-map ceilings), and objects.
- `audio/*-loop.ogg`: loop-ready music and rain ambience; `audio/footstep-*.ogg` contains quiet surface-specific movement cues derived from licensed recordings; `audio/ambient-*.ogg` contains short procedurally synthesized positional city cues; `audio/source/` retains supplied/generated source masters.
- `licenses/audio/`: generation provenance, source attribution, processing details, and redistribution notes for bundled audio.

All data files are loaded strictly: missing fields, unknown fields, invalid ranges, and cross-file references fail startup and `:core:validateContent` with a file-and-JSON-path diagnostic.
