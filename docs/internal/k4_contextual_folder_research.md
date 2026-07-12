# K4 Contextual Smart Folder Research

Date: 2026-07-13

## Sources

- Android UsageStats / UsageEvents official docs reviewed for local, permission-gated usage signals.
- Local code reviewed:
  - `UsageStatsHelper.getCurrentSlotTopApps(context, days = 28)`
  - `UsageStatsHelper.getWeightedScores(context, days = 28)`
  - `LauncherViewModel.suggestedApps`
  - `InsightEngine.generate`

## Finding

The app already has the core signal needed for contextual folders:

- `UsageStatsHelper.getCurrentSlotTopApps` ranks packages by absolute launch count in the current time slot.
- `UsageStatsHelper.getWeightedScores` combines recency, frequency, and time-slot score.
- `LauncherViewModel.suggestedApps` already consumes both and fills a 4-app recommendation list.

So K4 does not need a new persistent Room model first. The safer next step is a UX/product decision about where the contextual result should appear.

## Option A: Temporary "Now" Row

Add a temporary "Simdi" row/card above folders in `HomeScreen`.

Pros:
- Very visible.
- Easy for the user to understand.
- Does not disturb dock stability.

Cons:
- Adds another home-screen surface.
- More Compose UI and layout risk.
- Could feel noisy if ticker, search, widgets, and folders are already active.

## Option B: Time-Slot Boost In Existing Suggestions

Keep the current UI and add a controlled toggle that increases the weight of current-slot apps in `LauncherViewModel.suggestedApps`.

Pros:
- Smallest behavioral change.
- Uses existing `suggestedApps` and contextual dock pipeline.
- Lower visual risk.
- Fully local; no data leaves the device.

Cons:
- Less obvious to the user unless Settings explains it.
- Needs careful caps to avoid jumpy suggestions.

## Recommendation

Choose Option B first.

Implementation shape after approval:

- Add `AppPrefs.KEY_CONTEXTUAL_FOLDER_ENABLED`, default `true` or `false` depending on desired conservatism.
- Add toggle under Launcher or Apps settings.
- In `LauncherViewModel.suggestedApps`, only apply current slot picks / time-slot boost when the toggle is enabled.
- Keep existing fallback to recency/usage order.
- Do not create persistent folders yet.

## Approval Needed

K4 explicitly requires Huseyin approval before code changes. Suggested decision:

`K4-B`: enable contextual suggestions through the existing recommendation/dock path, no new visual row.
