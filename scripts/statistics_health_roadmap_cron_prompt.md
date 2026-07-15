# AppOrganizer statistics/health roadmap automation prompt

You are running inside `C:\Users\hekizoglu\Documents\AppOrganizer` from a scheduled automation task.

Goal for this run:
- Open `ISTATISTIK_TELEMETRI_VE_SAGLIK_ROADMAP.md`.
- Treat it as the single active source of truth.
- Find the first roadmap item whose status line contains `**Durum:** ⏳ Bekliyor` or `**Durum:** Bekliyor`.
- Complete only that one item end to end according to the roadmap rules.
- When, and only when, the item satisfies the completion standard, update that item's status line to `**Durum:** ✅ Tamamlandı` with a short evidence note.

Mandatory project rules:
- Before implementation, do online research using official or primary sources where relevant.
- Use MemPalace first when previous decisions, project history, or changing context matters.
- Preserve unrelated worktree changes. Do not revert user or previous-agent changes.
- Use `apply_patch` for manual edits.
- Add or update focused tests when the item changes behavior.
- Update `HISTORY.md`.
- Bump `versionCode` and `versionName` for app-code tasks.
- Send a Telegram progress report after each meaningful step if `scripts/telegram_notify.ps1` and `.env` are available.
- Follow the roadmap's own status rules: code alone is not completion.

Build policy:
- The user prefers avoiding excessive build time. Batch build work when safe, but do not mark an item `✅ Tamamlandı` unless the required verification for that item has passed.
- If the item changes app code, run at least:
  - `.\gradlew.bat compileDebugKotlin -PskipGoogleServices --console=plain`
  - focused unit tests relevant to the change, or `testDebugUnitTest` if no focused target is clear
- Run `assembleDebug` when the item requires an APK, release/version validation, or when a batch reaches 4-5 completed app-code tasks.
- If Gradle hits the known Windows build lock, use `scripts/clear_build_lock.ps1` and retry.
- If any required quality gate cannot be run or fails, do not mark the item completed. Leave a concise note in `HISTORY.md` and Telegram.

Important automation boundaries:
- Do not work on more than one `Bekliyor` item in this run.
- Do not mark partially implemented or unverified work as completed.
- If the first `Bekliyor` item is already implemented in code, verify it against the acceptance criteria before changing the roadmap status.
- If the worktree contains unrelated dirty files, work around them carefully. Commit only if the scope is safe and the repo state is clean enough.
- Do not delete or disable this cron automation unless no `Bekliyor` items remain or the user explicitly asks.

Final response format:
- State the roadmap item handled.
- State whether it was marked `✅ Tamamlandı`.
- List the quality gate result.
- Mention Telegram status.
