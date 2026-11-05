# Regenerates every .bbmodel and variant texture in blockbench/ from the mod's own geo and textures.
$ErrorActionPreference = 'Stop'
$tools = $PSScriptRoot
$out = Split-Path $tools
$assets = Join-Path (Split-Path $out) 'src\main\resources\assets\skywyrms'
$geo = Join-Path $assets 'dragon_geo\storm_wyrm.json'
$tex = Join-Path $assets 'textures\entity\storm_wyrm.png'
$tmp = Join-Path ([IO.Path]::GetTempPath()) 'skywyrms-blockbench'
New-Item -ItemType Directory -Force $tmp | Out-Null

& "$tools\geo2bb.ps1" -Geo $geo -Tex $tex -Out "$out\storm_wyrm.bbmodel"

& "$tools\crimson.ps1" -SrcGeo $geo -SrcTex $tex -OutGeo "$tmp\crimson_wyrm.json" -OutTex "$out\crimson_wyrm.png"
& "$tools\geo2bb.ps1" -Geo "$tmp\crimson_wyrm.json" -Tex "$out\crimson_wyrm.png" -Out "$out\crimson_wyrm.bbmodel" -Hide saddle

& "$tools\thunder.ps1" -SrcGeo "$tmp\crimson_wyrm.json" -SrcTex $tex -OutGeo "$tmp\thunder_wyrm.json" -OutTex "$out\thunder_wyrm.png"
& "$tools\geo2bb.ps1" -Geo "$tmp\thunder_wyrm.json" -Tex "$out\thunder_wyrm.png" -Out "$out\thunder_wyrm.bbmodel" -Hide saddle
