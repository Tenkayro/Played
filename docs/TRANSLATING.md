# Translating Played

All mod-owned interface text is defined in:

`src/main/resources/assets/played/lang/en_us.json`

This includes settings categories, option names and descriptions, enum choices,
buttons, tooltips, statistics screens, command responses, reset confirmations,
milestone messages, weekdays, time units, and default MiniMessage displays.
Minecraft and YACL supply translations for their own shared controls.

## Add a language

1. Copy `en_us.json` in the same directory and name the copy after a Minecraft
   locale, for example `de_de.json` or `fr_fr.json`.
2. Translate the values, keeping every key unchanged.
3. Select that language in Minecraft. No Java changes or new registration are needed.

English remains the fallback for keys omitted from other language files. Resource
packs can supply the same `assets/played/lang/<locale>.json` files without changing
the mod. Save JSON files as UTF-8, without comments or trailing commas.

## Preserve formatting

- Keep `%s` placeholders and their argument counts. You can reorder arguments with
  indexed placeholders such as `%2$s` and `%1$s`.
- `%%` renders a literal percent sign, for example in `played.hud.scale`.
- Keep `{time}`, `{session}`, `{current}`, and `{total}` in MiniMessage templates.
  These are separate from Minecraft's `%s` placeholders.
- Preserve MiniMessage tags, for example `<gradient:#a855f7:#ec4899>…</gradient>`.
- `played.date.session_pattern` is a Java DateTimeFormatter pattern. Translate the
  date order only using valid pattern syntax; it is not an ordinary sentence.
- Time-unit values are suffixes: `d`, `h`, `m`, and `s`. Spaces between verbose
  units, compact time formatting, and clock punctuation are handled by the mod.

## Key groups

| Prefix | Text |
| --- | --- |
| `played.config.category.*` | Settings categories and descriptions |
| `played.config.option.*` | Option names and `.description` help text |
| `played.enum.*` | HUD position, sort direction, and time-format choices |
| `played.command.*` | Client command output |
| `played.reset.*`, `played.sorting.*` | Reset dialogs and server-list tooltips |
| `played.stats.*`, `played.tab.*`, `played.metric.*` | Statistics interface |
| `played.import.*`, `played.export.*` | Backup screens and feedback |
| `played.hud.*`, `played.milestone.*` | HUD editor and milestones |
| `played.template.*` | Default MiniMessage display templates |
| `played.weekday.*`, `played.time.*`, `played.date.*` | Calendar and duration text |
| `key.played.*`, `key.category.played.*` | Keyboard shortcut labels |

Custom world/server names and user-edited display templates are user data and stay
as entered. Unmodified default templates resolve through the language file when
rendered, including defaults from existing configuration files. Stored enum names,
command syntax, file names, and configuration keys remain language-independent.
Developer log messages and CSV machine-readable column names are not UI translations.

## Tolgee synchronization

The repository is configured for Tolgee project **36408**. Install the project tools
with `npm ci`, then authenticate locally:

```sh
npm run translations:login
```

The CLI opens browser login. Select the Played project and authorize access. For
CI, supply `TOLGEE_API_KEY` as a secret environment variable. Never put a key in
`.tolgeerc.json`, scripts, commits, or chat messages. Revoke and replace exposed keys.

```sh
npm run translations:push
npm run translations:pull
```

Push uploads only the local `en_us.json` to the `en_us` language in Tolgee. Local
English is authoritative: changed English values replace their Tolgee counterparts.
Other languages and obsolete server keys are not deleted. ICU placeholder conversion
is disabled to preserve Minecraft `%s` and MiniMessage `{time}` placeholders.

Pull downloads translated/reviewed strings as flat JSON into a fresh staging folder
under `build/tolgee`. It validates the complete export before installing target
languages into `src/main/resources/assets/played/lang`:

- English is maintained locally and never replaced by pull.
- Locale tags such as `de-DE` become `de_de.json`; common regionless tags are mapped
  in `scripts/translations.mjs`. For other languages, use their exact Minecraft
  locale in Tolgee, or add an explicit alias in `minecraftLocale()`.
- Placeholders and MiniMessage tags must match the English originals. Invalid
  translations abort installation and report the affected key.
- Missing/empty translations are omitted so Minecraft falls back to English.
- Stale keys absent from the English source are omitted from installed files.
- Previous versions of replaced files are backed up in the staging folder.
- Existing language files are not deleted if Tolgee returns no translations for them.

Review the downloaded file changes before committing and include them in the next
normal mod build. Synchronization is explicit; it does not run during Gradle builds
or while Minecraft is playing. No Tolgee credential is shipped in the mod JAR.

Run `npm run translations:test` for the focused offline synchronization checks.

## Empty files for every Minecraft language

`scripts/minecraft-languages.json` records the 143 languages listed by the Minecraft
26.3 vanilla language metadata (asset index `26.3-34`, with the metadata hash recorded
in the manifest). It includes regional variants, dialects, and novelty languages.

```sh
npm run translations:prepare-languages
```

This preserves `en_us.json` and all existing translations. Missing resource files
are created as `{}` so they fall back to English in Minecraft. Do not ship resource
files containing empty string values: those would hide the corresponding text.

The command also creates 142 target-language upload templates in
`build/tolgee-language-templates`, each with all current English keys and empty
translation values. These templates are for Tolgee, not for packaging in the mod.
Regenerate them after adding new source keys or after `gradlew clean`.

To import the target-language templates separately from the normal English push:

```sh
npm run translations:push-languages
```

This command creates missing languages directly through Tolgee's language API.
It checks the existing project languages first, preserves their names and existing
translations, creates only missing Minecraft language tags, then verifies that all
143 languages exist. Empty translation files alone do not register project languages.
The existing 150 project keys are automatically available in each added language.

It reuses the local CLI login or `TOLGEE_API_KEY`. The credential needs
`languages.edit` permission. The API adapter uses the pinned Tolgee CLI 2.21.0
credential handling, so login secrets are not duplicated in repository files.
Special Minecraft languages are registered with their exact tags and native names.
The usual `translations:push` command still uploads only English.
