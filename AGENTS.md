# Skywyrms (our fork)

Buildable source of the Fabric mod Skywyrms ("Dragons" by Pokato), decompiled from
`skywyrms-1.2.3-fabric-26.3.jar` (CurseForge project 1711514, file 9336921) with Vineflower 1.12.0.
There is no upstream source repository. We change it ourselves and run it on the "Large's dungeon" server.

## License

MIT, "Copyright (c) 2026 pokato". Keep `LICENSE` unchanged. `build.gradle` adds it as
`LICENSE_skywyrms` to every jar; keep that rule.

## Build

Gradle needs JDK 25. The machine's `JAVA_HOME` points to JDK 17, so set it per shell:

```
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'; .\gradlew build
```

Output: `build/libs/skywyrms-<mod_version>-fabric-26.3.jar`. There are no tests; the build is the gate.
Versions (Loom, loader, Fabric API) come from fabric-example-mod's `26.3` branch and live in `gradle.properties`.
`fabric.mod.json` takes its version from `mod_version`. Bump `mod_version` with every change we deploy.

## Layout

- `src/main/java/com/pokato/skywyrms`: decompiled sources. Client and common code share one source set.
- `src/main/resources`: assets, data, `fabric.mod.json`, icon, all copied from the original jar.
- Dragon models: `assets/skywyrms/dragon_geo/<kind>_wyrm.json` (ember, frost, storm, thorn, tide, void), a
  custom format (parts with parent, pivot, rotation; cubes with origin, size, uv, mirror; 256x256 texture; plus
  seat, mouth and bbox). Loaded by `entity/GeoData.java` and used on both client (`DragonModel`) and server
  (`DragonRig`), so geometry changes can affect gameplay positions.
- Animation is procedural in `anim/DragonAnimator.java` and addresses parts by name. Renaming or removing a part
  needs a matching code change.
- Egg model is code: `client/render/EggModel.java`. Textures: `assets/skywyrms/textures/entity`.

## Decompiled code

The only hand fix to the decompiler output is a block around `case 3` in `DragonAnimator.idleAction`.
Recompiled bytecode was checked against the original with normalized `javap -c -p -constants` dumps: all 63
classes behave identically. Repeat that comparison when touching decompiled code you did not mean to change.

