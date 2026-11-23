#!/usr/bin/env python3
"""Build the offline catalog and thumbnail atlas from pinned upstream snapshots.

Requires Pillow. Pass Hasan's data/exercises.json, the OpenExerciseBase checkout,
and the output asset directory, in that order. Media paths from Hasan are omitted.
"""
import gzip
import json
import sys
from pathlib import Path

from PIL import Image, ImageOps

HASAN_REV = "7455efae41b330c265e7cd4b78dfa848e7ce5ebd"
OPEN_REV = "d2e9058330bc477be1b1e51b9f9df788f963ac04"
EDGE = 96
COLUMNS = 16


def main():
    hasan_file, open_directory, output = map(Path, sys.argv[1:])
    output.mkdir(parents=True, exist_ok=True)
    hasan = json.loads(hasan_file.read_text())
    index = json.loads((open_directory / "index.json").read_text())["exercises"]
    rows = []
    images = []
    for item in hasan:
        rows.append({
            "id": "hasan-" + item["id"],
            "name": item["name"],
            "equipment": [item["equipment"]],
            "muscles": list(dict.fromkeys([item["target"], *item["secondary_muscles"]])),
            "body": [item["body_part"]],
            "steps": item["instruction_steps"]["en"],
            "timed": item["body_part"] == "cardio",
        })
    for item in index:
        exercise = json.loads((open_directory / "exercises" / (item["id"] + ".json")).read_text())
        if exercise["metadata"]["reviewStatus"] not in {"accepted", "accepted_with_edits"}:
            continue
        row = {
            "id": item["id"],
            "name": item["name"],
            "equipment": item["equipment"] or ["body weight"],
            "muscles": item["bodyParts"],
            "body": item["bodyParts"],
            "steps": [step["description"] for step in exercise["instructions"]],
            "timed": not any(metric["type"] == "repetitions" for metric in exercise["performanceMetrics"]),
        }
        if (item["imageFile"] and item["imageFolder"] and
                exercise["metadata"].get("image_source") == "ai generated"):
            relative = Path("images", item["imageFolder"], item["imageFile"])
            file = open_directory / relative
            if file.is_file() and file.suffix == ".png":
                with Image.open(file) as image:
                    tile = ImageOps.pad(image.convert("RGB"), (EDGE, EDGE), color="white")
                    width, height = image.size
                row.update(tile=len(images), path=relative.as_posix(), size=file.stat().st_size,
                           resolution=f"{width} × {height}")
                images.append(tile)
        rows.append(row)
    assert len({row["id"] for row in rows}) == len(rows)
    catalog = json.dumps(
        {"hasanRevision": HASAN_REV, "openRevision": OPEN_REV, "exercises": rows},
        ensure_ascii=False, separators=(",", ":")
    ).encode("utf-8")
    # Android asset merging unwraps files with a .gz suffix.
    (output / "catalog.bin").write_bytes(gzip.compress(catalog, mtime=0))
    atlas = Image.new("RGB", (COLUMNS * EDGE, (len(images) + COLUMNS - 1) // COLUMNS * EDGE), "white")
    for i, tile in enumerate(images):
        atlas.paste(tile, (i % COLUMNS * EDGE, i // COLUMNS * EDGE))
    atlas.save(output / "open_thumbnails.webp", quality=78, method=6)


if __name__ == "__main__":
    main()
