# Roadmap Completion Audit - 2026-07-01

## Local work completed

- Latest local data committed and pushed in `ea218e2`:
  - `HISTORY.md`
  - `PermissionsBanner.kt`
  - `current_home.png`
  - `design_a.png`
  - `screen1.png`
  - `screen2.png`
  - `screen3.png`
  - `screen4.png`
- AppOrganizer scheduled repeat delivery task was disabled:
  - `AppOrganizer_DebugBuildDelivery_6h`
  - Existing AppOrganizer audit tasks are also disabled.
- Previous local cycle commits already completed the high-score docs loop items:
  - docs backlog scoring and ROADMAP sync
  - local audit resolve cycle
  - launcher search source enforcement
  - grouped launcher search results
  - permission fallback scored item closure
  - launcher warning cleanup
  - Play Store QA pack

## Roadmap items not locally completable

These items remain open because they require a user-owned external system, credentials, or real device access. They should not be marked complete from a local agent run.

| Area | Item | Blocker | Required next action |
|---|---|---|---|
| Play Store | QUERY_ALL_PACKAGES declaration | Play Console access and policy form | Fill the Play Console declaration before production upload |
| Play Store | Content rating questionnaire | Play Console access | Complete the questionnaire in Play Console |
| Privacy | GitHub Pages activation | Repository settings access | Enable GitHub Pages for `/docs` and verify `privacy_policy.html` public URL |
| Firebase | Crashlytics setup | `google-services.json` and Firebase project/service account | Provide Firebase config or connect project |
| Gemini | LLM fallback | Gemini API key | Provide key through secure local config |
| QA | Android 14 NotificationListener real-device test | Physical device required | Run on an Android 14 phone and record result |
| QA | BLUR-4 real-device test | Physical device/GPU behavior required | Run on representative API 26+ devices |
| QA | AppNotificationListener restart test | Real notification access lifecycle required | Verify on phone after permission toggle/reboot |
| QA | AllApps double-tap test | Emulator did not reproduce | Verify on physical device |
| QA | Manufacturer categories test | OEM app set required | Verify on Samsung/Xiaomi/Google-style devices |

## Product backlog still requiring implementation

The U1-U10 critical UX list in `ROADMAP.md` is still a product implementation backlog. It can be worked sequentially now that user approval has been given, but it is not a single local verification task:

- Settings hierarchy redesign
- Search defaults and search settings redesign
- File search hardening
- Header/banner height control
- Folder search placement
- Drag-and-drop folder ordering
- Home visual redesign using launcher references
- FAB behavior decision
- Open-source launcher reference pass

## Build/send policy

- Manual APK delivery should use `scripts/send_debug_build.ps1`.
- Automatic 6-hour delivery is stopped as of this audit.
- Future repeated delivery should only be re-enabled after explicit user request.
