# AI Fitness Coach design

The default Studio theme uses graphite, mineral white, and citron. Training photography and the angular monogram are original project assets. The two photos are AI-generated and shipped as local WebP files. Exercise rows use the existing offline, movement-specific vector illustrations. Explicit palette choices and system dynamic colors remain available.

## Navigation and hierarchy

- Home gives the exercise picker the primary action and keeps session-building, recent movements, saved activity, and the daily quote within reach.
- Exercise setup owns a route keyed by exercise ID. Back returns to the precise launcher, list, or Home surface that opened it. Media and camera guide routes return to the same setup entry.
- History shows summary first. Expanding a session exposes recorded sets and form notes. Equipment labels use app terminology and locale.
- Settings groups profile and appearance, training and coaching, and device and data. Profile, media storage, and Health Connect open their working pages directly. English and Persian use the same menu structure and plain labels.
- The widgets share the app mark and a prominent Quick Start action. Compact and motivation widgets retain their saved data sources and destinations.
- The React companion uses shareable fragment links for Preview, Analyzer Lab, and Android Code. The browser's Back and Forward buttons traverse the sections on both mobile and desktop.

## Benchmark

| Reference | Pattern applied |
|---|---|
| Hevy, Strong, FitNotes and JEFIT | Keep setup close to the log. Lead with the exercise name and surface set details on demand. |
| Nike Training Club, LoadMuscle and Fitbod | Use purposeful training photography, immediate exercise selection and clear equipment metadata. |
| Boostcamp, Caliber, PUSH and Gymverse | Distinguish planning, coaching, and logging tools by what they let the user do. |
| Strava, Runna, Runkeeper, Google Health and Apple Fitness | Keep the next activity prominent, and put trends or longer summaries below the immediate action. |

The [LoadMuscle roundup](https://loadmuscle.com/blog/best-free-workout-apps-2026), [Zapier roundup](https://zapier.com/blog/best-fitness-tracking-apps/), and public Google Play screenshots informed the comparison. The brand and artwork in this repository are independent of the reference apps.

## Android implementation references

`tools/install-android-skills.sh` installs the relevant Android guidance from a pinned Git revision into the project's `.agents/skills` folder. Compose adaptive and performance guidance informs the width, insets, and stable list state. Navigation and intent guidance informs route ownership and widget destinations. Glance canonical layouts and update guidance inform widget hierarchy and refresh behavior, applied to the existing RemoteViews implementation. CameraX, LiteRT-LM, permissions, and ANR guidance were reviewed against the existing runtime boundaries. Testing guidance informs the Compose screenshot and emulator checks. No Navigation, Glance, camera, or AI dependency migration is required for these layout changes.
