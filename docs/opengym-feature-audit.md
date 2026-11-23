# OpenGym feature audit

Sources: [project README](https://github.com/DuarteSantos8/openGym), [product tour](https://opengym.ch/). OpenGym publishes different feature sets across its native companion apps and self-hosted service. This inventory tracks the AI Fitness Coach implementation independently. No OpenGym code or branded assets are used.

| Area | OpenGym capability | AI Fitness Coach baseline |
| --- | --- | --- |
| Library | Search, typo tolerance, exercise types, muscle/body map, owned-equipment filters, animated demos, figure variants | Bilingual search and four categories. 39 offline illustrations, downloadable images and user imports. |
| Library | Custom exercise records, photo/GIF/video, guide link, on-device location metadata stripping | Existing media import on catalog entries. |
| Plan | Editable routines, routines per weekday, Monday/Sunday weeks, per-date overrides, rotation | Editable queue for a single session. |
| Plan | Starter PPL, upper/lower, full body and 5×5 plans, muscle previews | Quick workout suggestions. |
| Plan | Supersets, warm-ups, drop sets, rest-pause, pyramids, deloads, substitutions | Straight sets and adjustable sets, reps, duration and rest. |
| Training | Guided workout, prior weights, record alerts, rest timer, next session/rest day | Guided manual workouts, countdown and supported camera analyzers. |
| Training | Exercise/set menus, card/list preference, optional quick buttons, swipe copy/delete and undo | Session editor with workout items. |
| Training | Bodyweight and add-on load, per-side reps, timed sets, cardio speed/duration, optional RIR/RPE | Bodyweight, timed holds, reps, dumbbell load, band metadata. |
| Training | Per-gym equipment inventory, plate math for barbell variants, machine stacks | Dumbbell and resistance-band setup. |
| Training | Screen awake, visual/notification rest alerts, live volume | In-app timer and notification preferences. |
| Progression | Linear, Greyskull, double, triple and timed rules with rationale and stall handling | Manual targets. |
| Progression | Estimated 1RM, Structural Balance protocols, lifting volume and lifts relative to body weight | Stored sets with reps, load and duration. |
| Progression | Year heatmap, streaks/badges, muscle volume/recovery/strength views | Workout history. |
| Body | Weigh-in chart, target/trend, maintenance range, progress photos and comparison | Optional profile body metrics. |
| History | Retroactive entries, editing and date moves, session photo/video | Saved history and CSV export. |
| Data | JSON backup/merge, CSV, FitNotes/Strong/Hevy import, Apple Health file import, plan file and PDF | CSV workout export and Android Health Connect. |
| Appearance | Light/dark, accents, bespoke icons, 26 locales, module visibility | Theme and palette options, English/Persian RTL. |
| Privacy/accounts | Offline mobile build, optional self-hosted sync, per-profile passkeys/password, QR pairing, merge and admin tools | Offline-first Android application, no required account. |
| Integrations | Two-way Health Connect/Apple Health, external device data, signed webhooks | Android Health Connect, permission scoped. |
| Notifications | Workout-day and optional gym/geofence reminders, lock-screen actions | Android notifications and home-screen widgets. |
| Wearables | Wear OS and Apple Watch, rest, set actions, effort, heart rate, lock and complications | Phone UI and launcher widgets. |
| AI coach | Optional BYOK Q&A, weekly plan and history review, explicit change approval | Opt-in local/external AI workout analysis. |
| MCP | Optional local training-history access and workout logging, depending on distribution | No MCP endpoint. |

Apple-platform parity, self-hosting/sync, and wearable support require their own application targets or service. Mobile catalog additions can run offline using bundled metadata and thumbnails.

## Implementation and verification

| Capability delivered | Implementation | Verification |
| --- | --- | --- |
| Library expansion, typo and Persian-tolerant search, equipment × muscle filters | Both pinned sources are bundled, with Hasan as the normal library default. The plan builder reuses the same picker. | JVM matching test and Android packaged-catalog instrumentation. |
| Media choices, attribution, offline fallback | Validated OpenExerciseBase PNGs and thumbnails, exact cross-source matches, and target-region diagrams. User media uses the existing import path. | Packaged atlas and URL mapping instrumentation. Download and player behavior also need an on-device check. |
| Weekly routines and quick start | Seven independent weekday slots, local persistence, editable upper/lower/full-body starters, a muscle preview, and Start centered in Android navigation. | JVM routine codec and Android DataStore persistence tests. |
| Guided workouts | Existing set entry, timers and camera analyzers, plus recent load in exercise setup and keep-awake while training. | Existing workout engine tests plus the on-device workflow above. |
| Progress | Rolling 365-day activity heatmap, streaks, recent muscle involvement, estimated 1RM and separate History tab. | JVM fixture covers exact day boundaries, streaks, load bases, and exclusion of sets over 12 reps. |
| Licenses and credits | Bundled source terms, notices for application libraries, and links from Settings. | Check copyright, revision, media rights and offline opening. |

Remaining parity includes named routine collections, one-off schedule moves, supersets and special-set types, per-side and effort fields, automatic progression, inventory and lifting analytics, custom exercise definitions, session editing and attachments, body charts and photos, JSON and third-party import, PDF/share, locale expansion, wearable controls, self-hosted sync/accounts, geofences, webhooks and MCP. The camera coach, Health Connect, Android widgets and local-first data remain part of AI Fitness Coach.

### Motion and stress cases

| Surface | Observed issue | Change or test |
| --- | --- | --- |
| Welcome step indicator | Progress value jumped when the step changed | Ease the indicator to the new value over 170 ms. The step and inputs respond immediately. |
| Web companion navigation | A section is replaced without a visible transition | Brief 150 ms opacity transition with immediate URL and tab feedback. Reduced-motion mode displays it instantly. |
| Web controls | Press was primarily represented by the final selection | Apply subtle scale on press without gating navigation. |
| Exercise results | Movement names up to 66 characters, missing media, combined filters covering zero to over 1,000 rows | Use wrapping rows, region fallback, and a keyed lazy list. |
| Workout builder | Expanded catalog would eagerly render a menu with more than 1,500 entries. Equipment controls squeezed into a single row at large type. | Use the searchable lazy picker and allow controls to wrap at each line. |
| RTL and large text | Bilingual mixed-script provider names and two independent filter controls | Bind layout direction to app language. CI capture samples the muscle filter at 165% type. Review on a physical phone with the camera off and on. |

Continuous welcome animation, list entrance effects, and moving exercise cards were omitted. They add movement while users read or repeatedly filter content.

## Follow-up

| Capability | Implementation |
| --- | --- |
| Optional interface modules | Simple and Advanced presets with separate saved choices for Plan, progress charts, AI, Health Connect UI and media management. Workout history and the normal exercise picker stay available in both modes. |
| Rotating workout routines | Run the seven existing saved routine slots in order. The rotation advances after saving a workout containing sets. Weekday schedules and rotation share the same saved workouts. |
| Workout starters | Added Push, Pull and a three-exercise 5 × 5 template using the equipment already in the Android library. |
| Independent content delivery | App-side refresh accepts a versioned catalog and atlas, caches the last valid pack for offline use, and supports licensed pinned media references. The content workflow validates catalog changes separately from the APK workflow. See [Exercise catalog delivery](../catalog-content/README.md). |
