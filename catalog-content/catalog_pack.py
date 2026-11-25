"""Build or validate the independently published Android exercise catalog."""

import argparse
import gzip
import io
import json
from pathlib import Path
import re
import struct
from urllib.parse import urlsplit
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

LIMITS = {"manifest.json": 1024, "catalog.json": 8 * 1024 * 1024, "thumbnails.webp": 3 * 1024 * 1024}
PACK_LIMIT = 8 * 1024 * 1024
ID = re.compile(r"[A-Za-z0-9_-]{1,120}")
REVISION = re.compile(r"[0-9a-f]{40}")
SHA256 = re.compile(r"[0-9a-f]{64}")
OEB_PHOTO = re.compile(r"images/[A-Za-z0-9_-]+/[A-Za-z0-9_.-]+\.png")
TYPES = {"PNG_SINGLE", "JPEG_SINGLE", "WEBP_SINGLE", "GIF", "MP4"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_atlas_size(data):
    require(len(data) >= 30 and data[:4] == b"RIFF" and data[8:12] == b"WEBP", "Invalid WebP")
    kind = data[12:16]
    if kind == b"VP8X":
        return tuple(1 + int.from_bytes(data[i:i + 3], "little") for i in (24, 27))
    if kind == b"VP8L" and data[20:21] == b"/":
        bits = int.from_bytes(data[21:25], "little")
        return 1 + (bits & 0x3fff), 1 + ((bits >> 14) & 0x3fff)
    if kind == b"VP8 " and data[23:26] == b"\x9d\x01\x2a":
        return tuple(value & 0x3fff for value in struct.unpack_from("<HH", data, 26))
    raise ValueError("Unknown WebP dimensions")


def validate(pack, built_in, previous=None):
    require(len(pack) <= PACK_LIMIT, "Catalog archive exceeds its size limit")
    with ZipFile(io.BytesIO(pack)) as archive:
        entries = archive.infolist()
        require(len(entries) == len(LIMITS) and {it.filename for it in entries} == LIMITS.keys(),
                "Unexpected or duplicate catalog entries")
        require(all(it.file_size <= LIMITS[it.filename] for it in entries), "Catalog entry exceeds its size limit")
        manifest = json.loads(archive.read("manifest.json"))
        catalog = json.loads(archive.read("catalog.json"))
        width, height = read_atlas_size(archive.read("thumbnails.webp"))
    require(manifest["schema"] == 1 and type(manifest["version"]) is int and 1 <= manifest["version"] < 2**31,
            "Invalid schema or version")
    require(width == 1536 and 96 <= height <= 3072 and height % 96 == 0, "Invalid thumbnail atlas dimensions")
    require(REVISION.fullmatch(catalog["openRevision"]), "Invalid OpenExerciseBase revision")
    rows = catalog["exercises"]
    require(isinstance(rows, list) and 1 <= len(rows) <= 25_000, "Invalid exercise count")
    ids = set()
    for item in rows:
        ident = item["id"]
        require(ID.fullmatch(ident) and ident not in ids and ident not in built_in, f"Invalid or duplicate ID {ident}")
        ids.add(ident)
        require(0 < len(item["name"].strip()) <= 200 and type(item["timed"]) is bool, f"Invalid exercise {ident}")
        for field in ("equipment", "muscles", "body", "steps"):
            require(isinstance(item[field], list) and all(isinstance(v, str) for v in item[field]),
                    f"Invalid {field} for {ident}")
        if "nameFa" in item:
            require(isinstance(item["nameFa"], str) and 0 < len(item["nameFa"].strip()) <= 200,
                    f"Invalid translated name for {ident}")
        for field in ("musclesFa", "stepsFa"):
            if field in item:
                require(isinstance(item[field], list) and all(isinstance(v, str) for v in item[field]),
                        f"Invalid {field} for {ident}")
        if "tile" in item:
            require(type(item["tile"]) is int and 0 <= item["tile"] < height // 96 * 16, f"Invalid tile for {ident}")
            if item.get("path"):
                require(ident.startswith("EX-") and OEB_PHOTO.fullmatch(item["path"]) and 0 < item["size"] <= 2_000_000,
                        f"Invalid OpenExerciseBase photo for {ident}")
            else:
                require(item["tileAttribution"].strip(), f"Missing thumbnail attribution for {ident}")
    media_ids = set()
    media = catalog.get("media", [])
    require(isinstance(media, list) and len(media) <= 25_000, "Invalid media list")
    for item in media:
        ident = item["exerciseId"]
        require(ident in ids | built_in and ident not in media_ids, f"Invalid media exercise ID {ident}")
        media_ids.add(ident)
        kind = item["type"]
        size_limit = 30_000_000 if kind == "MP4" else 5_000_000
        require(kind in TYPES and type(item["bytes"]) is int and 0 < item["bytes"] <= size_limit and
                SHA256.fullmatch(item["sha256"]), f"Invalid media format, size or checksum for {ident}")
        uri = urlsplit(item["url"])
        segments = uri.path.split("/")
        require(uri.scheme == "https" and uri.netloc == "raw.githubusercontent.com" and
                not uri.query and not uri.fragment and len(segments) >= 5 and
                REVISION.fullmatch(segments[3]), f"Media must use a commit-pinned public URL for {ident}")
        license_url = urlsplit(item["licenseUrl"])
        require(item["license"].strip() and item["attribution"].strip() and license_url.scheme == "https" and
                license_url.hostname and not license_url.username and not license_url.password,
                f"Missing or invalid licensing for {ident}")
    if previous is not None:
        prior_version, prior_ids = validate(previous, built_in)
        require(prior_ids <= ids, "A catalog update removed exercise IDs used by history or plans")
        require(pack == previous or manifest["version"] > prior_version, "Bump the version when the package changes")
    return manifest["version"], ids


def main():
    root = Path(__file__).resolve().parents[1]
    assets = root / "android-app/app/src/main/assets/exercise_sources"
    definitions = root / "android-app/app/src/main/java/com/aifitnesscoach/core/exercise/definitions/ExerciseCatalog.kt"
    built_in = set(re.findall(r'id = "([a-z0-9_]+)"', definitions.read_text(encoding="utf-8")))
    bundled = gzip.decompress((assets / "catalog.bin").read_bytes())
    baseline = {item["id"] for item in json.loads(bundled)["exercises"]}
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path)
    parser.add_argument("--previous", type=Path)
    parser.add_argument("--version", type=int, help="Build a pack with this version, then validate it")
    parser.add_argument("--data", type=Path, help="Read normalized catalog JSON instead of the APK baseline")
    parser.add_argument("--atlas", type=Path, help="Use a WebP thumbnail atlas instead of the APK baseline")
    args = parser.parse_args()
    previous = args.previous.read_bytes() if args.previous else None
    if args.version is not None:
        output = io.BytesIO()
        with ZipFile(output, "w", ZIP_DEFLATED, compresslevel=9) as archive:
            for name, data in {
                "manifest.json": json.dumps({"schema": 1, "version": args.version}, separators=(",", ":")).encode(),
                "catalog.json": args.data.read_bytes() if args.data else bundled,
                "thumbnails.webp": (args.atlas or assets / "open_thumbnails.webp").read_bytes(),
            }.items():
                archive.writestr(ZipInfo(name), data, compress_type=ZIP_DEFLATED, compresslevel=9)
        content = output.getvalue()
    else:
        content = args.pack.read_bytes()
    version, ids = validate(content, built_in, previous)
    require(baseline <= ids, "The feed must include all exercises shipped with the app")
    if args.version is not None:
        args.pack.write_bytes(content)
    print(f"Catalog {version}: {len(ids)} external exercises, {len(content):,} bytes")


if __name__ == "__main__":
    main()
