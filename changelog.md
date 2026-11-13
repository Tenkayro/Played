# Changes

## Added

- A statistics hub with today's playtime, current/previous week totals, a seven-day activity chart, and searchable world/server rankings.
- History for the last 500 sessions and a longest-session record.
- A draggable HUD editor with live preview, scaling, and Save/Cancel controls.
- Configurable shortcuts for statistics, HUD visibility, and the HUD editor; unbound by default.
- Optional playtime milestone notifications for individual worlds and servers.
- JSON backup import/export and CSV exports for totals, daily activity, and sessions. Imports preserve higher counters and avoid duplicating sessions.
- `/played stats`, `/played config`, `/played hud`, `/played today`, and `/played week`.

## Improved

- Click the main-menu playtime text to open statistics. A clock button is available in the pause menu.
- Settings are accessible without Mod Menu, including through `/played config`.
- AFK detection now uses keyboard and mouse input, including inventory activity. Passive movement no longer counts as activity.
- Added a safe fallback for invalid date formats.

## Updating

Existing world, server, and total playtime are preserved. Daily statistics and session history start with this update; previous totals cannot be reconstructed into past dates or sessions. All statistics remain stored locally.
