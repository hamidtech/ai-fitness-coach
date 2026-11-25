# Exercise catalog delivery

The Android app ships with an offline catalog and accepts a versioned pack from
[main](https://raw.githubusercontent.com/hamidtech/ai-fitness-coach/main/catalog-content/pack.zip).
Check for updates in Exercises or Settings → Exercise catalog. A failed request
keeps the last valid on-device pack. A newly installed app can use the bundled
version offline. The public feed becomes available when this pack lands on main.

## Publish content

1. Prepare normalized `catalog.json` and a WebP atlas. Use the existing archive
   as a starting point. Keep exercise IDs stable and retain existing IDs so
   saved routines and workout history continue resolving them. Mark new source
   records with their own ID prefix. The app groups additional providers under
   Other sources.
2. Bump the integer version and generate the pack. The atlas is 16 columns of
   96-pixel square tiles. OpenExerciseBase entries use their pinned source and PNG
   path. An independent thumbnail uses a tile number and `tileAttribution`.
3. Commit the refreshed pack and its source notices under `catalog-content/`.
   The catalog workflow validates data, dimensions, media references, IDs and the
   version. The Android APK workflow watches `android-app/` and its own workflow
   only. Catalog-only updates publish with a merge to main.
4. Open the app and check for updates. Catalog records and thumbnails arrive
   together. Exercise media files download on demand using the existing media
   library and its Wi-Fi-only preference.

```bash
unzip -p catalog-content/pack.zip catalog.json > /tmp/catalog.json
unzip -p catalog-content/pack.zip thumbnails.webp > /tmp/thumbnails.webp
# Edit the normalized metadata and update the atlas before packaging.
python3 catalog-content/catalog_pack.py catalog-content/pack.zip \
  --version 2 --data /tmp/catalog.json --atlas /tmp/thumbnails.webp
```

## Media entries

The optional `media` array supports one downloadable asset per exercise
with those fields:

| Field | Format |
| --- | --- |
| exerciseId | Stable catalog or built-in exercise ID |
| type | PNG_SINGLE, JPEG_SINGLE, WEBP_SINGLE, GIF or MP4 |
| url | Public `raw.githubusercontent.com` link pinned to a 40-character commit SHA |
| bytes, sha256 | Exact byte length and lowercase SHA-256 |
| attribution, license, licenseUrl | Publisher attribution and applicable HTTPS license link |

Publish only media for which the project has redistribution rights. The initial
pack includes the same MIT exercise data and CC BY-NC 4.0 OpenExerciseBase material
as the APK. The [bundled source notices](../android-app/app/src/main/assets/exercise_sources)
record the original copyrights and terms. The feed installer accepts data and
images and keeps downloaded media under app storage.

## Version 2 media pilot

Eight H.264 MP4 clips reference the upstream [Workout Free Videos CC0 collection](https://github.com/namnq/workout-free-videos/tree/346c9709eaecfe160a0500650a5f7a43b8425762).
Each clip was matched to the movement and equipment, downloaded from the pinned
commit, and checked against the upstream byte count and SHA-256.

| Exercise ID | Clip |
| --- | --- |
| `hasan-0289` | Dumbbell bench press |
| `hasan-0251` | Chest dip |
| `hasan-0334` | Dumbbell lateral raise |
| `hasan-0095` | Barbell shrug |
| `hasan-0406` | Dumbbell shrug |
| `hasan-0120` | Barbell upright row |
| `hasan-0861` | Seated cable row |
| `hasan-1429` | Wide-grip pull-up |

Check for catalog updates from the Exercises toolbar, then use the Video or
GIF filter to find these movements. The package contains the media references.
Android fetches individual clips when requested.


## Version 3 movement additions

Four independently authored movements are added to the existing 1,515 external
records: high plank, child's pose, seated forward fold on the floor, and wide
push-up. Each includes original schematic artwork and bilingual instructions.
The optional `nameFa`, `musclesFa` and `stepsFa` fields are backward compatible.
Zing's public exercise guides are linked as references from the app's guide
filter; their text, images and videos are not included in the pack.
All v2 media and prior exercise IDs are retained.
