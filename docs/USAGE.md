# Played statistics and tools

Click the playtime text in the title menu, use the clock button in the pause menu, or enter `/played stats`.
The hub includes an overview, world/server rankings with search, and session history.
English is the default language. All Played interface text is configured in `assets/played/lang/en_us.json`.

## Commands and keyboard shortcuts

| Command | Action |
| --- | --- |
| `/played` | Session, current world/server, and all-time counters |
| `/played session`, `/played current`, `/played total` | Individual counters |
| `/played session pause`, `/played session resume` | Pause or resume playtime tracking for the current session |
| `/played session reset` | Reset current session playtime without leaving the game |
| `/played share [session\|current\|total]` | Generate a clickable chat message to share playtime |
| `/played today`, `/played week` | Local calendar day/week counters |
| `/played stats` | Statistics hub |
| `/played config` | Settings, also available without Mod Menu |
| `/played hud` | HUD editor |
| `/played help` | Overview of all available commands |

Four shortcuts are available under Controls → Key Binds → Played:
- Open playtime statistics
- Toggle playtime overlay
- Edit playtime overlay
- Peek playtime overlay (hold to reveal the HUD while pressed)

They are unbound by default.

## Calendar and sessions

The overview shows today's playtime, the current calendar week, the previous week, an interactive
seven-day activity chart with week navigation buttons (`<` and `>`), your all-time best day record,
and the longest recorded session. Weeks start on Monday. Days follow the computer's local timezone,
including daylight saving changes. Past day values retain the dates they were recorded under if the
timezone changes.

Existing world, server and all-time totals remain intact. Daily/session history starts with this update;
older totals cannot be reconstructed into dates or sessions. History retains the last 500 sessions.
The currently running session is clearly marked as active with a green indicator (`● Name (Active)`).
Singleplayer worlds show their friendly world title from `level.dat` rather than raw directory names.
Daily totals, best day, and the longest-session record remain even as older individual sessions are trimmed.

A session covers one connection to a world/server. AFK and pause gaps are excluded from its active duration,
while its start/end timestamps describe the whole session. Active sessions are checkpointed along with the
counters every 15 seconds and when tracking pauses or stops. After a crash, the last checkpoint remains in history.
Resetting a world, server, or all-time counter does not delete calendar/session history.

## AFK, break reminders, and milestones

- **AFK Tracking**: Enable Ignore AFK Time in Settings → Tracking and choose the timeout in seconds.
  Keyboard input, mouse movement, clicks, and scrolling count, including in inventories. Held movement,
  attack, and use controls also count. Passive movement from water, vehicles, or server corrections does
  not reset inactivity. Normal singleplayer pause still suspends tracking, regardless of input.
- **Break Reminders**: Set Break Reminder in Settings → Tracking (in minutes, 0 to disable). When active,
  Played will send a toast notification reminding you to take a break after long continuous play.
- **Milestones**: Optional milestone toasts appear when a world/server passes 10, 100, 500, 1,000, and
  every further 1,000 hours. Joining a server or importing existing time does not replay historical milestones.
  Resetting a counter allows its milestones to be reached again.
- **Audio Feedback**: Toggle Notification Sounds in Settings → Notifications to play chime sound effects
  when reaching a milestone or receiving a break reminder.

## HUD editor and formatting

The in-game overlay displays configurable text on screen and automatically hides during F1 screenshot mode.
Supported placeholders in `Display Text` include:
- `{time}`: Default shorthand for current world/server playtime
- `{session}`: Active session duration
- `{current}`: Current world/server playtime
- `{total}`: All-time total playtime across all worlds/servers
- `{today}`: Playtime logged today (local calendar day)
- `{week}`: Playtime logged during the current calendar week
- `{afk}`: Displays `[PAUSED]` when manually paused, or `[AFK]` when inactive

The editor previews the configured corner HUD even when it is disabled. Drag its outline, adjust its size,
and choose whether the overlay should be enabled. Done saves; Cancel or Escape discards changes. The placement
anchors to the nearest corner so it adapts to window resizing. Apply settings before opening the editor from
YACL to preview those applied values.

## Backup, export and import

Export creates a portable JSON backup plus CSV files for world/server totals, daily values, and sessions in
`Played/exports`. The JSON path is copied to the clipboard, and the export folder is automatically opened in
your operating system's file manager. CSV durations are milliseconds; session timestamps are Unix epoch
milliseconds. CSV files are for analysis; use JSON for import.

Drop a JSON backup onto the import screen or paste its absolute file path. Review the counts and merge
explanation, then confirm. Import keeps the higher value for each total, world/server counter and calendar day,
and deduplicates sessions by ID. Repeated imports are safe: they do not add the same time again. This is a backup
restore/merge operation; it does not sum independent installations' playtime.

World counters are matched by existing save-folder names. Transfer the world saves separately first. Import never
creates world folders. Server counters are matched by normalized address. Backups do not contain configuration
or absolute world paths.

History is stored in `Played/history.json`. Existing counter files keep their previous locations. History and
configuration use atomic replacement with `.bak` recovery. All data stays local; no account or server-side mod
is required.
