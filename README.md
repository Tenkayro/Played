# Played

Played is a lightweight Fabric mod that tracks local playtime for singleplayer worlds and multiplayer servers, including session, current-world or server, and total playtime displays.

The clickable playtime text in the title menu and clock button in the pause menu opens a statistics hub with daily and weekly activity, world/server rankings, session history, HUD editing, and JSON/CSV exports. In-game, use `/played stats`, `/played config`, `/played hud`, or chat commands like `/played share`. Four configurable keyboard shortcuts can be assigned under Controls. Full usage and backup/import semantics are documented in [docs/USAGE.md](docs/USAGE.md).

## Key Features

- **Customizable In-Game HUD**: Corner overlay showing custom placeholders (`{time}`, `{session}`, `{current}`, `{total}`, `{today}`, `{week}`, `{afk}`) with a live draggable in-game editor and hold-to-peek keybind.
- **Statistics Hub**: Interactive weekly activity chart with week navigation, all-time best day record, longest session record, searchable world/server rankings, and the last 500 sessions with active session status.
- **AFK & Session Management**: Keyboard/mouse activity-based AFK detection, manual pause/resume, session reset, and optional session break reminders.
- **Milestones & Audio Feedback**: Toast notifications and optional sound effects when reaching playtime milestones (10h, 100h, 500h, 1000h+) or receiving break reminders.
- **Chat Sharing**: Easily share your session, world, or total playtime with other players in chat via `/played share`.
- **Local Data & Backups**: All data is stored locally. Export portable JSON backups and CSV spreadsheets anytime with automatic folder access in your file manager.

## Official downloads

- [Modrinth](https://modrinth.com/mod/played/versions)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/played/files)

Only downloads published by Tenkayro through official channels are authorized distributions of Played.

## Source availability and contributions

The source code is publicly available for transparency, private modification, and contributions. Played is source available, not open source: redistribution, reuploads, unofficial builds, and independent releases are not permitted.

Contributions are welcome through issues and pull requests. See [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes.

## License

Played is licensed under the custom [Played Source-Available License 1.0](LICENSE). You may view the source and modify it privately, but you may not publish or distribute the original mod, modified versions, or compiled builds without prior written permission from Tenkayro.
