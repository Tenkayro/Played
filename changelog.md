# Changes

## Added

- **Peek HUD Keybind**: Hold a key (`key.played.peek_hud`, unbound by default) to temporarily reveal the playtime overlay without keeping it on.
- **New HUD Placeholders**: `{today}`, `{week}`, and `{afk}` (shows `[PAUSED]` or `[AFK]`) can now be used in custom HUD formats.
- **Break Reminders**: Configurable session break reminder with toast notifications (Settings → Tracking).
- **Sound Effects**: Audio chimes for reaching milestones and receiving break reminders.
- **Activity Chart Navigation**: Browse previous weeks in the 7-day activity chart using the new `<` and `>` buttons.
- **All-Time Best Day Record**: Highlights your personal record for the most playtime logged in a single calendar day.
- **Active Session Indicator**: The active session is highlighted with a green indicator (`● Active`) in the Session History tab.
- **Friendly World Names**: Singleplayer worlds now display their actual world name instead of folder names where available.
- **Direct Export Access**: Exporting now automatically opens the `Played/exports` folder in your file explorer.
- **New Commands**:
  - `/played session pause` & `/played session resume`: Temporarily pause or resume tracking.
  - `/played session reset`: Restart the current session timer.
  - `/played share [session|current|total]`: Generate a clickable chat message to share your playtime.
  - `/played help`: Quick overview of all commands.

## Improved

- **F1 Screenshot Mode**: The playtime HUD overlay now automatically hides when pressing F1 (hide GUI).
- **AFK Detection**: Accurately tracks keyboard, mouse, and inventory interaction; passive movements (water, riding) no longer prevent AFK timeout.

## Updating

Existing world, server, and total playtime are preserved. All data and session history remain stored locally in your game directory (`Played/history.json`).
