# Played statistics and tools

Click the playtime text in the title menu, use the clock button in the pause menu, or enter `/played stats`.
The hub includes an overview, world/server rankings with search, and session history.
English is the default language. All Played interface text is configured in `assets/played/lang/en_us.json`.

## Commands and keyboard shortcuts

| Command | Action |
| --- | --- |
| `/played` | Session, current world/server, and all-time counters |
| `/played session`, `/played current`, `/played total` | Individual counters |
| `/played today`, `/played week` | Local calendar day/week counters |
| `/played stats` | Statistics hub |
| `/played config` | Settings, also available without Mod Menu |
| `/played hud` | HUD editor |

Three shortcuts are available under Controls → Key Binds → Played: open statistics,
toggle the corner HUD, and open the HUD editor. They are unbound by default.

## Calendar and sessions

The overview shows today, the current calendar week, the previous week, a seven-day
activity chart with tooltips, and the longest recorded session. Weeks start on Monday.
Days follow the computer's local timezone, including daylight saving changes.
Past day values retain the dates they were recorded under if the timezone changes.

Existing world, server and all-time totals remain intact. Daily/session history starts
with this update; older totals cannot be reconstructed into dates or sessions.
History retains the last 500 sessions, including an active session. Daily totals and
the longest-session record remain after old sessions are trimmed.

A session covers one connection to a world/server. AFK and pause gaps are excluded
from its active duration, while its start/end timestamps describe the whole session.
Active sessions are checkpointed along with the counters every 15 seconds and when
tracking pauses or stops. After a crash, the last checkpoint remains in history.
Resetting a world, server, or all-time counter does not delete calendar/session history.

## AFK and milestones

Enable Ignore AFK Time in Settings → Tracking and choose the timeout in seconds.
Keyboard input, mouse movement, clicks and scrolling count, including in inventories.
Held movement, attack and use controls also count. Passive movement from water,
vehicles or server corrections does not reset inactivity. Normal singleplayer pause
still suspends tracking, regardless of input.

Milestone notifications are optional and disabled by default. When enabled, a toast
appears when a world/server passes 10, 100, 500, 1,000, and every further 1,000 hours.
Joining a server or importing existing time does not replay historical milestones.
Resetting a counter allows its milestones to be reached again.

## HUD editor

The editor previews the configured corner HUD even when it is disabled. Drag its
outline, adjust its size, and choose whether the overlay should be enabled.
Done saves; Cancel or Escape discards changes. The placement anchors to the nearest
corner so it adapts to window resizing. The other overlays have separate settings.
Apply settings before opening the editor from YACL to preview those applied values.

## Backup, export and import

Export creates a portable JSON backup plus CSV files for world/server totals, daily
values and sessions in `Played/exports`. The JSON path is copied to the clipboard.
CSV durations are milliseconds; session timestamps are Unix epoch milliseconds.
CSV files are for analysis; use JSON for import.

Drop a JSON backup onto the import screen or paste its absolute file path. Review the
counts and merge explanation, then confirm. Import keeps the higher value for each
total, world/server counter and calendar day, and deduplicates sessions by ID.
Repeated imports are safe: they do not add the same time again. This is a backup
restore/merge operation; it does not sum independent installations' playtime.

World counters are matched by existing save-folder names. Transfer the world saves
separately first. Import never creates world folders. Server counters are matched by
normalized address. Backups do not contain configuration or absolute world paths.

History is stored in `Played/history.json`. Existing counter files keep their previous
locations. History and configuration use atomic replacement with `.bak` recovery.
All data stays local; no account or server-side mod is required.
