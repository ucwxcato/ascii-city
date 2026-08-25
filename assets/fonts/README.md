# ASCII City bitmap font

Runtime files `vt323-10x18.fnt`/`.png`, `vt323-8x15.fnt`/`.png`, and `vt323-6x12.fnt`/`.png` are generated from VT323 Regular with `tools/generate_bitmap_font.py`.

- Source: Google Fonts `ofl/vt323/VT323-Regular.ttf`
- Upstream commit recorded by Google Fonts: `9bd4b3f69887fd960d51d07602db60a28a789145`
- Source SHA-256: `cf4de751ada78ceac033dbe16a687742939995b77bc2a052ae17a4957958594d`
- License: SIL Open Font License 1.1; see `assets/licenses/vt323/OFL.txt`
- Runtime coverage: printable ASCII code points 32 through 126
- Standard runtime cell: 10×18 logical pixels with a 10-pixel advance
- Dense world runtime cell: 8×15 logical pixels with an 8-pixel advance
- Fine world runtime cell: 6×12 logical pixels with a 6-pixel advance

Rebuild either derived atlas from the repository root:

```bash
python3 tools/generate_bitmap_font.py
python3 tools/generate_bitmap_font.py --name vt323-8x15 --cell-width 8 --cell-height 15 --font-size 17 --draw-x-offset 0 --draw-y-offset -2
python3 tools/generate_bitmap_font.py --name vt323-6x12 --cell-width 6 --cell-height 12 --font-size 13 --draw-x-offset 0 --draw-y-offset -1
```

The source TTF remains in `assets/fonts/source/` so the derived atlas can be reproduced. Runtime code loads only the `.fnt` and `.png` files.
