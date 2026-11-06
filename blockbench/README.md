# Blockbench models

Blockbench (Modded Entity) versions of the storm wyrm, for viewing and designing. The game does not read these
files: it loads `assets/skywyrms/dragon_geo/*.json`. The solar wyrm ships in game: its geo
`dragon_geo/solar_wyrm.json` is the one `tools/solar.ps1` writes (`build.ps1` puts it in `$env:TEMP\skywyrms-blockbench`).

| File | What it is |
| --- | --- |
| `storm_wyrm.bbmodel` | The storm wyrm exactly as the mod defines it (115 parts, 220 cubes). |
| `crimson_wyrm.bbmodel` + `.png` | Design variant: neck and tail twice as long and thinner, slimmer body, no front legs (wyvern), red texture. |
| `thunder_wyrm.bbmodel` + `.png` | Crimson shape with a storm look: dark storm-cloud body, bright yellow veins with a blue glow, zig-zag lightning spines, glowing kinked horn tips and tail prongs, forked lightning on the wing membranes. |
| `solar_wyrm.bbmodel` + `.png` | Light/sun element, storm wyrm body at the same size: ivory scales with gold veins and an orange glow, a sunburst crown of eleven rays behind the head, tall golden sun-ray spines, golden horns, a fan of rays on the tail tip, sunset wing membranes with light rays. |

Textures are embedded in each `.bbmodel`. The saddle is hidden in the variants. Wings are shown in the rest pose
(spread); in game `DragonAnimator` folds them.

## Regenerating

```powershell
powershell -ExecutionPolicy Bypass -File blockbench\tools\build.ps1
```

- `tools/geo2bb.ps1` converts a mod geo file to `.bbmodel` (Y-up, X-mirrored, absolute pivots, box UV).
- `tools/crimson.ps1` builds the crimson shape and texture from the storm wyrm.
- `tools/thunder.ps1` builds the thunder look on top of the crimson shape.
- `tools/solar.ps1` builds the solar wyrm from the storm wyrm.

## Using a variant in game

The variants keep every part name, so `DragonAnimator` would still drive them. Front leg groups (`leg_fl`,
`leg_fr`) are present but empty. The thunder variant adds `deco_dorsal_*_z1` and `_z2` parts and paints the free
area at UV 0,208 to 64,240. Shipping one needs a new geo file, a `DragonKind`, and a check of the mouth, seat and
breath positions, because the longer neck moves the head. The solar wyrm went through these steps.
