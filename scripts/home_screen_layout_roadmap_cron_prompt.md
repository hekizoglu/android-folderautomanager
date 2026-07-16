# AppOrganizer home screen layout editor roadmap automation prompt

You are running inside `C:\Users\hekizoglu\Documents\AppOrganizer` from a scheduled automation task.

Goal for this run:
- Open `ROADMAP_HOME_SCREEN_LAYOUT_EDITOR.md`.
- Treat section `23.0 Cron Görev Panosu` as the active task queue.
- Find the first roadmap item whose status line contains `Bekliyor`.
- Complete only that one item end to end according to the roadmap rules.
- Mark it completed only after the required verification passes.

Speed rules:
- Keep the run focused. Do not inspect unrelated files.
- Prefer focused unit tests over the full test suite when the item allows it.
- Do not run `assembleDebug` unless the item needs APK or final release validation.
- Keep Telegram messages short and ASCII-only.

Mandatory project rules:
- Before implementation, do online research using official or primary sources where relevant.
- Use MemPalace first when previous decisions, project history, or changing context matters.
- Preserve unrelated worktree changes.
- Use `apply_patch` for manual edits.
- Add or update focused tests when the item changes behavior.
- Update `HISTORY.md`.
- Bump `versionCode` and `versionName` for app-code tasks.
- Follow the roadmap status rules: code alone is not completion.

Build policy:
- If the item changes app code, run at least:
  - `.\gradlew.bat compileDebugKotlin -PskipGoogleServices --console=plain`
  - focused unit tests relevant to the change, or `testDebugUnitTest` if no focused target is clear
- If Gradle hits the known Windows build lock, use `scripts/clear_build_lock.ps1` and retry once.
- If a quality gate cannot be run or fails, do not mark the item completed.

Boundaries:
- Do not work on more than one `Bekliyor` item in this run.
- Do not delete or disable the cron automation unless no `Bekliyor` items remain or the user explicitly asks.

Final response format:
- Item handled.
- Completed or not completed.
- Verification result.
- Telegram status.
