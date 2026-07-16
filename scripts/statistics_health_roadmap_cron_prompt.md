# AppOrganizer statistics/health roadmap automation prompt

You are running inside `C:\Users\hekizoglu\Documents\AppOrganizer` from a scheduled automation task.

Goal for this run:
- Open `ISTATISTIK_TELEMETRI_VE_SAGLIK_ROADMAP.md`.
- Treat it as the single active source of truth.
- Find the first roadmap item whose status line contains `Bekliyor`.
- Complete only that one item end to end according to the roadmap rules.
- Mark it completed only after the required verification passes.

Speed rules:
- Keep the run focused. Do not inspect unrelated files.
- If an item was already partially implemented, first repair the blocker and rerun the smallest relevant verification.
- Prefer focused unit tests over the full test suite when the roadmap item allows it.
- Do not run `assembleDebug` unless the item needs an APK, release validation, or the final roadmap completion step.
- Keep Telegram messages short and ASCII-only.

Mandatory project rules:
- Before implementation, do online research using official or primary sources where relevant.
- Use MemPalace first when previous decisions, project history, or changing context matters.
- Preserve unrelated worktree changes. Do not revert user or previous-agent changes.
- Use `apply_patch` for manual edits.
- Add or update focused tests when the item changes behavior.
- Update `HISTORY.md`.
- Bump `versionCode` and `versionName` for app-code tasks.
- Follow the roadmap's own status rules: code alone is not completion.

Build policy:
- If the item changes app code, run at least:
  - `.\gradlew.bat compileDebugKotlin -PskipGoogleServices --console=plain`
  - focused unit tests relevant to the change, or `testDebugUnitTest` if no focused target is clear
- If Gradle hits the known Windows build lock, use `scripts/clear_build_lock.ps1` and retry once.
- If a quality gate cannot be run or fails, do not mark the item completed. Leave a concise note in `HISTORY.md` and Telegram.

Boundaries:
- Do not work on more than one `Bekliyor` item in this run.
- Do not delete or disable this cron automation unless no `Bekliyor` items remain or the user explicitly asks.

Final response format:
- Item handled.
- Completed or not completed.
- Verification result.
- Telegram status.
