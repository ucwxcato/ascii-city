"""Generate ASCII City's fixed-cell VT323 bitmap font atlases."""

from pathlib import Path
import argparse

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
SOURCE_FONT = ROOT / "assets" / "fonts" / "source" / "VT323-Regular.ttf"
ATLAS_COLUMNS = 16
FIRST_CODE_POINT = 32
LAST_CODE_POINT = 126


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cell-width", type=int, default=10)
    parser.add_argument("--cell-height", type=int, default=18)
    parser.add_argument("--font-size", type=int, default=20)
    parser.add_argument("--draw-x-offset", type=int, default=1)
    parser.add_argument("--draw-y-offset", type=int, default=-2)
    parser.add_argument("--name", default="vt323-10x18")
    return parser.parse_args()


def main() -> None:
    args = arguments()
    if args.cell_width <= 0 or args.cell_height <= 0 or args.font_size <= 0:
        raise ValueError("cell dimensions and font size must be positive")
    output_image = ROOT / "assets" / "fonts" / f"{args.name}.png"
    output_definition = ROOT / "assets" / "fonts" / f"{args.name}.fnt"
    font = ImageFont.truetype(SOURCE_FONT, size=args.font_size)
    character_count = LAST_CODE_POINT - FIRST_CODE_POINT + 1
    atlas_rows = (character_count + ATLAS_COLUMNS - 1) // ATLAS_COLUMNS
    atlas_width = ATLAS_COLUMNS * args.cell_width
    atlas_height = atlas_rows * args.cell_height
    atlas = Image.new("RGBA", (atlas_width, atlas_height), (255, 255, 255, 0))
    draw = ImageDraw.Draw(atlas)

    character_lines: list[str] = []
    for offset, code_point in enumerate(range(FIRST_CODE_POINT, LAST_CODE_POINT + 1)):
        column = offset % ATLAS_COLUMNS
        row = offset // ATLAS_COLUMNS
        x = column * args.cell_width
        y = row * args.cell_height
        character = chr(code_point)

        if character != " ":
            draw.text(
                (x + args.draw_x_offset, y + args.draw_y_offset),
                character,
                font=font,
                fill=(255, 255, 255, 255),
            )

        character_lines.append(
            "char id={id:<3} x={x:<3} y={y:<3} width={width} height={height} "
            "xoffset=0 yoffset=0 xadvance={width} page=0 chnl=15".format(
                id=code_point,
                x=x,
                y=y,
                width=args.cell_width,
                height=args.cell_height,
            )
        )

    atlas.save(output_image, optimize=True)
    definition = "\n".join(
        [
            f'info face="VT323" size={args.font_size} bold=0 italic=0 charset="" unicode=0 '
            "stretchH=100 smooth=0 aa=1 padding=0,0,0,0 spacing=0,0",
            f"common lineHeight={args.cell_height} base={args.cell_height} scaleW={atlas_width} scaleH={atlas_height} pages=1 packed=0",
            f'page id=0 file="{args.name}.png"',
            f"chars count={character_count}",
            *character_lines,
            "kernings count=0",
            "",
        ]
    )
    output_definition.write_text(definition, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
