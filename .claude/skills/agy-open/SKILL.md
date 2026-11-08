---
name: agy-open
description: Use when the user pastes an agy (Antigravity) session/conversation ID (a UUID like 7aec4c75-585c-4402-aaea-7797d9e5f893), with or without words like "agy", "id", "director". Returns the command that opens agy on that conversation with permissions skipped.
---

# agy-open

When the user gives an agy session ID, reply with the command to open agy on that conversation, with permissions skipped.

## Steps

1. Take the UUID from the message (format `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`). Join it back together if the user's paste split it across lines or spaces.
2. Reply with this command, putting the ID in place of `<id>`:

```powershell
agy --conversation <id> --dangerously-skip-permissions
```

3. If the user names the session's role (for example "agy director"), and the ID is different from the one listed in `AGENTS.md` under "Agy session ID", ask whether to update `AGENTS.md`. Only the director edits `AGENTS.md`.

## Rules

- Do not run the command yourself. The user opens the agy window.
- Output the command in a code block so the user can copy it.
- If the ID is not a full UUID, say so and ask for the full ID.
