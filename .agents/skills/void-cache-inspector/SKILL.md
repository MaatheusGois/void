---
name: void-cache-inspector
description: Inspect Void RuneScape cache definitions from the command line and return AI-friendly JSON or concise text.
---

# Void cache inspector

Use this skill when a task requires checking IDs, names, models, animations, transforms, or other decoded cache definitions. Prefer the CLI over opening the Compose viewer when the result will be consumed by an agent or another command.

## Command

Run from the repository root:

```bash
./gradlew --quiet tools:app:run --args='--cli --cache=../../data/cache --type=npcs --id=1158 --fields=id,name,varbit,transforms'
```

The Gradle run task uses `tools/app` as its working directory. For a different cache, use an absolute path whenever possible:

```bash
./gradlew --quiet tools:app:run --args='--cli --cache=/absolute/path/to/cache --type=npcs --query=Kalphite --format=json'
```

The default output is JSON:

```json
{"cache":"...","types":["NPCs"],"count":1,"results":[{"type":"NPCs","fields":{"id":1158,"name":"Kalphite Queen"}}]}
```

Use `--format=text` for a compact human-readable result. Use `--help` for the complete option list.

## Options

- `--cache=<path>`: required cache directory or cache path.
- `--type=<name>`: `items`, `npcs`, `objs`, `anims`, `emotes`, `gfx`, `sounds`, `ifaces`, `components`, `enums`, `vars`, `invs`, `structs`, or `all`.
- `--id=<id>`: exact numeric cache ID or exact `stringId`.
- `--query=<text>`: case-insensitive substring search across decoded fields.
- `--fields=a,b`: restrict returned fields; omit it to return all fields.
- `--limit=<n>`: cap the total number of returned definitions; default is `20`.
- `--format=json|text`: JSON is the default and is preferred for agent responses.

## Agent procedure

1. Identify the definition type and exact ID if known.
2. Query only the fields needed for the answer with `--fields`.
3. Prefer `--id` for exact checks; broad `--query` searches every decoded field.
4. Treat an empty `results` array as “not found in the selected cache,” not as proof that the game never references the definition.
5. Report the command and cache path used when the result matters to a code change.
6. Do not modify cache files during inspection.

## Animations and game configuration

`--type=anims` checks whether numeric animation definitions exist in the binary cache. In this repository, the source of truth for the human-readable animation name-to-ID mapping is the repository's `*.anims.toml` catalog loaded through `definitions.animations=anims.toml`.

- `data/**/**.anims.toml` maps names to animation IDs.
- `data/area/**/**.combat.toml` connects animations to attacks, defence, and death states.
- Kotlin content scripts may call `anim("...")` directly.

Therefore, when answering “which animations does this NPC use,” inspect both sources:

```bash
./gradlew --quiet tools:app:run --args='--cli --cache=../../data/cache --type=anims --id=6232 --fields=id'
```

Then read the relevant area `.anims.toml`, `.combat.toml`, and Kotlin script. Treat the `.anims.toml` entry as the authoritative name-to-ID mapping; use the CLI to validate that the numeric ID exists in the selected cache revision. The CLI does not infer all cross-file gameplay references.

## Failure handling

- `No cache found in dir`: verify the path relative to `tools/app`, or use an absolute path.
- Non-zero exit: preserve the error and retry only after correcting the path or option.
- Do not silently substitute another cache revision; cache IDs are revision-specific.
