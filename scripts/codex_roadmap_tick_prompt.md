AppOrganizer roadmap automation run.

Read and obey:
- `AGENTS.md`
- `CODEX_DEVIR_2026-07-15.md`
- `ROADMAP_AI_AUDIT_2026-07-14.md`
- `HISTORY.md`

Primary goal:
- Continue from the next unfinished roadmap item.
- Prefer the recommended order from `CODEX_DEVIR_2026-07-15.md`.
- Work safely in the existing dirty worktree.
- Never revert unrelated user changes.

Execution rules:
- Do online research before each requested task and prefer official sources.
- Use MemPalace first if prior decisions or project memory are needed.
- After each meaningful step, send a Telegram status message using `.env` and the existing repo scripts or direct Telegram API.
- If you finish a roadmap item, run the required quality gates:
  - `./gradlew compileDebugKotlin -PskipGoogleServices --console=plain`
  - `./gradlew testDebugUnitTest -PskipGoogleServices --console=plain`
  - `./gradlew assembleDebug -PskipGoogleServices --console=plain`
- If app code changes, update `HISTORY.md` and apply the required version bump.
- If you are blocked by risk, ambiguity, merge state, or a failing guard, do not force changes. Send Telegram with the blocker and stop cleanly.

Output:
- End with a short status summary describing the step attempted, the result, and the next best follow-up.
