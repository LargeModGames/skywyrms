# Contributing to Skywyrms (our fork)

This repository is our own buildable copy of the Fabric mod Skywyrms ("Dragons" by Pokato). It was decompiled
from `skywyrms-1.2.3-fabric-26.3.jar` (CurseForge project 1711514, file 9336921), because there is no public
source. We change it for the "Large's dungeon" server and its players.

## License

The mod is MIT licensed, "Copyright (c) 2026 pokato".

- Never edit or remove `LICENSE`.
- `build.gradle` copies it into every jar as `LICENSE_skywyrms`. Keep that rule when you touch the build.
- Your contributions are released under the same MIT license.

## Setup

You need:

- JDK 25 (Temurin 25 is what we use). Gradle refuses to run on an older JDK.
- git, and the GitHub CLI `gh` for pull requests.
- A Minecraft 26.3 client with Fabric Loader if you want to test in your own game.

If your `JAVA_HOME` points to another JDK, set it for the shell before building:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
.\gradlew build
```

The jar lands in `build/libs/skywyrms-<mod_version>-fabric-26.3.jar`. The `-sources.jar` next to it is not
for the server.

Versions of Loom, Fabric Loader and Fabric API are in `gradle.properties`. They come from the `26.3` branch of
[fabric-example-mod](https://github.com/FabricMC/fabric-example-mod). Change them only on purpose, in their own
pull request.

## Project layout

| Path | What it is |
| --- | --- |
| `src/main/java/com/pokato/skywyrms` | All Java code. Client and server code share one source set. |
| `.../fabric` | Fabric entrypoints (`SkywyrmsFabric`, `SkywyrmsFabricClient`). |
| `.../entity` | Dragon and egg entities, `DragonKind` (stats per dragon), `GeoData` (model loader), `DragonRig`. |
| `.../anim` | Procedural animation: `DragonAnimator`, `Skeleton`, `Bone`. |
| `.../client/render` | Renderers and models (`DragonModel`, `EggModel`). |
| `.../ability` | Breath and special abilities. |
| `.../config` | `ModConfig`, written to `config/skywyrms.json` on the server. |
| `src/main/resources/assets/skywyrms` | Models, textures, sounds, particles, language file. |
| `src/main/resources/data/skywyrms` | Loot tables. |
| `src/main/resources/fabric.mod.json` | Mod metadata. Its version is filled in from `mod_version`. |

## Changing dragon models

Each dragon has one geometry file in `assets/skywyrms/dragon_geo/`: `ember_wyrm`, `frost_wyrm`, `storm_wyrm`,
`thorn_wyrm`, `tide_wyrm`, `void_wyrm`. This is the mod's own JSON format, not a Blockbench file.

Top-level fields read by `entity/GeoData.java`:

| Field | Meaning |
| --- | --- |
| `texture_size` | Texture width and height in pixels (256 x 256). |
| `scale` | Overall size multiplier. Also scales hitbox, seat and breath position. |
| `seat` | Rider position in model pixels. Y and Z set where the rider sits. |
| `bbox` | Hitbox width and height in blocks, before `scale`. |
| `mouth_bone` | Part the breath comes from (normally `head`). |
| `mouth_local` | Breath origin relative to `mouth_bone`, in model pixels. |
| `parts` | The bone tree. |

`kind`, `mouth` and `mouth_breath` are in the files but are not read by the code. `sit_drop` is read but not
used anywhere.

Each part has `name`, `parent` (`null` for `root`), `pivot` (pixels), `rotation` (degrees) and `cubes`. A
parent must appear before its children, or the game fails to load the model. Each cube has `origin`, `size`,
`uv` (top-left texture offset), `inflate` and `mirror`. A cube with one size of 0 is drawn as a two-sided flat
sheet, which is how wing membranes work.

Things to know before you edit:

- `DragonAnimator` moves parts by name (`neck1` to `neck4`, `tail1` to `tail6`, `wing_l_fore`, `leg_fl_shin`,
  and so on). Moving or reshaping cubes is a data-only change. Renaming, removing or re-parenting a named part
  needs a matching code change.
- The server reads the same file: `scale`, `seat` and `bbox` set the hitbox and rider position when entities
  register, and `DragonRig` uses the bone tree to find where breath starts. Every player and the server must
  run the same jar after a model change.
- New cubes need texture space. Keep `uv` inside the 256 x 256 sheet and update the matching textures in
  `textures/entity/<kind>.png`, its `_glow` texture, and the `hybrid/<body>_<blood>` textures that use this
  body.
- The egg model is code, not data: `client/render/EggModel.java`.

## Changing code

The sources are decompiler output. Expect odd local names and the occasional strange control flow. That is
fine to clean up, but do it in its own pull request with no behaviour change, so reviews stay readable.

The only hand fix to the raw decompiler output so far is a block around `case 3` in
`DragonAnimator.idleAction`, needed to compile.

### Checking that you did not change behaviour

For refactors and cleanups, compare the bytecode of your build with the previous build. Constant pool numbers
and whitespace differ between builds, so strip them first (bash):

```bash
JAVAP="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot/bin/javap"
norm() { "$JAVAP" -c -p -constants "$1" | sed -E 's/#[0-9]+(:#[0-9]+)?//g; s/^Compiled from.*//; s/ +\/\//  \/\//'; }
diff <(norm old/com/pokato/skywyrms/anim/Bone.class) <(norm new/com/pokato/skywyrms/anim/Bone.class)
```

Extract both jars into `old/` and `new/` first. Remaining differences in branch layout or local slot numbers
can still be harmless. Explain them in the pull request.

## Testing

### Automated tests

| Command | What it runs | Where it lives |
| --- | --- | --- |
| `.\gradlew build` | Compile, JUnit tests and server gametests | `src/test`, `src/gametest` |
| `.\gradlew test` | JUnit only: every JSON parses, every `dragon_geo` model loads, keeps the bones the animator moves and fits its texture, all textures, sounds, particles, items, loot tables and translations exist, genes logic | `src/test/java` |
| `.\gradlew runGameTest` | Server gametests: every dragon and hybrid spawns and ticks, eggs hatch, breath and cast abilities work, mouth position, riding | `src/gametest/.../DragonGameTests.java` |
| `.\gradlew runProductionServerGameTest` | The same gametests on a real Fabric server with the built jar | |
| `.\gradlew runProductionServerSmoke` | A real dedicated server: generates a world, ticks every dragon for 5 seconds, stops. Look for `SKYWYRMS SMOKE PASSED` in `build/run/productionSmoke/logs/latest.log` | `ServerSmoke.java` |
| `.\gradlew runClientGameTest` | A game window: renders every dragon, a hybrid and an egg, rides a dragon, uses an ability on a dedicated server. Screenshots in `build/run/clientGameTest/screenshots` | `DragonClientGameTest.java` |

`runProductionClientGameTest` is what CI runs (under xvfb on Linux). On Windows it can crash in native code
while loading; use `runClientGameTest` locally.

When you add a feature, add a test next to the closest existing one. When you rename or remove a bone that
`DragonAnimator` uses, update `ANIMATED_BONES` in `GeoDataTest` together with the animator.

### CI

`.github/workflows/ci.yml` runs on every pull request and every push to `main`:

- **Build and tests:** `gradlew build`, then checks that every jar has `LICENSE_skywyrms` and the right
  `fabric.mod.json`. The jar is uploaded as the `skywyrms-jar` artifact.
- **Production server tests:** the gametests and the smoke test on a real server. Logs are uploaded.
- **Client gametest:** the client test under xvfb. Screenshots are uploaded as `client-screenshots`. Look at them
  for model and texture changes.
- **Bytecode report** (pull requests only, never fails): compares every class with the base branch. The summary
  is on the run page, the diffs in the `bytecode-report` artifact. Use it to show that a cleanup changes nothing.

The first three must pass before a pull request can merge.

### By hand

1. For anything a player can see or do, test it in game. Loom can start a dev client and server:
   `.\gradlew runClient` and `.\gradlew runServer` (game files go into `run/`, which git ignores).
2. For model changes, check every dragon you touched: idle, walking, flying, sitting, with a rider, and the
   breath ability (it must still come out of the mouth).
3. For server-side changes, test on a dedicated server (`runServer`), not only in single player.

Write in the pull request what you tested and what you could not test.

## Branches, commits and pull requests

- `main` is always buildable and is what goes on the server. Do not push to it directly.
- Branch from `main` with a short, descriptive name: `model/frost-longer-tail`, `fix/egg-hatch-timer`,
  `build/fabric-api-0.166`.
- Commit small: one commit per finished step that builds on its own (for example "Lengthen frost wyrm tail",
  then "Adjust frost wyrm tail texture"). Write messages in the imperative, short first line.
- No generated attribution lines in commits or pull requests.

Opening a pull request:

```powershell
git switch -c model/frost-longer-tail
# ... change, build, test, commit ...
git push -u origin model/frost-longer-tail
gh pr create --fill
```

`gh pr create` fills in the pull request template. Complete every section, link the issue it closes
(`Closes #12`), and add screenshots for anything visual.

Review:

- At least one other person reviews before merge.
- Reviewers check the build, the license rule, and that unrelated files were not touched.
- Fix review comments with new commits on the same branch. Squash only if the history is noise.
- Merge only when the build passes and review is done. Delete the branch after merge.

## Issues

Use the issue templates: **Bug report** for something broken, **Change request** for new features, balance
changes and model changes. Search existing issues first. For bugs, attach `logs/latest.log` (and the crash
report, if any) and say whether it happens in single player, on the server, or both.

## Releasing to the server

1. Raise `mod_version` in `gradle.properties` (for example `1.2.3` to `1.2.3-ld.1`), in its own commit on `main`.
2. Build on `main`: `.\gradlew build`.
3. Tag the commit: `git tag v1.2.3-ld.1` and push the tag.
4. On the Apex panel (server 3425467): stop the server, download the current `mods/skywyrms-*.jar` as a backup,
   delete it, upload the new jar (not the `-sources` one), start the server, and check the log for the new
   version.
5. Send the same jar to every player. A client with a different version may see wrong models or wrong breath
   positions.
6. If something breaks, put the backup jar back and restart.

## Upstream updates

Pokato publishes new versions on CurseForge (1.2.4 was released on 3 Nov 2026) without source. To pick up an
upstream change, decompile the new jar with Vineflower the same way, diff that against a fresh decompile of
1.2.3, and port the differences by hand in their own pull request. Do not overwrite our sources with a new
decompile.
