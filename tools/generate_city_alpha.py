"""Generate the fixed, authored 96x96 city-alpha footprint."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "assets" / "maps" / "city-alpha.txt"
SIZE = 96
VERTICAL_ROADS = (16, 48, 80)
HORIZONTAL_ROADS = (8, 32, 56, 80)


def building_token(x: int, z: int) -> str:
    pattern = (x // 12 + z // 12) % 4
    return ("l", "m", "t", "n")[pattern]


def main() -> None:
    grid = [[building_token(x, z) for x in range(SIZE)] for z in range(SIZE)]

    for z in range(SIZE):
        for x in range(SIZE):
            road_distance = min(
                min(abs(x - road_x) for road_x in VERTICAL_ROADS),
                min(abs(z - road_z) for road_z in HORIZONTAL_ROADS),
            )
            if road_distance <= 2:
                grid[z][x] = "."
            elif road_distance <= 4:
                grid[z][x] = ","

    block_x_ranges = ((0, 11), (21, 43), (53, 75), (85, 95))
    block_z_ranges = ((13, 27), (37, 51), (61, 75), (85, 95))
    for x_start, x_end in block_x_ranges:
        alley_x = (x_start + x_end) // 2
        for z_start, z_end in block_z_ranges:
            for z in range(z_start, z_end + 1):
                if grid[z][alley_x] not in (".", ","):
                    grid[z][alley_x] = ":"

    plazas = ((30, 18), (62, 42), (6, 66), (88, 90))
    for center_x, center_z in plazas:
        for z in range(center_z - 2, center_z + 3):
            for x in range(center_x - 2, center_x + 3):
                if 0 <= x < SIZE and 0 <= z < SIZE and grid[z][x] not in (".", ","):
                    grid[z][x] = "_"

    for z in range(18, 23):
        for x in range(53, 61):
            if grid[z][x] not in (".", ",", ":", "_"):
                grid[z][x] = "n"

    rows_north_first = ["".join(grid[z]) for z in range(SIZE - 1, -1, -1)]
    assert len(rows_north_first) == SIZE
    assert all(len(row) == SIZE for row in rows_north_first)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(rows_north_first) + "\n", encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
