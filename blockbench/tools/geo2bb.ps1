param([string]$Geo, [string]$Tex, [string]$Out, [string[]]$Hide = @(), [string]$Glow = '')
$ErrorActionPreference = 'Stop'
$inv = [Globalization.CultureInfo]::InvariantCulture
$gd = Get-Content $Geo -Raw | ConvertFrom-Json

function F($x) { ([double]$x).ToString('0.####', $inv) }
function Arr($a) { '[' + (($a | ForEach-Object { F $_ }) -join ',') + ']' }
function Esc($s) { $s.Replace('\', '\\').Replace('"', '\"') }

$abs = @{}; $groups = @{}; $order = @()
$elements = New-Object System.Collections.Generic.List[string]

foreach ($p in $gd.parts) {
    $pp = @([double]$p.pivot[0], [double]$p.pivot[1], [double]$p.pivot[2])
    if ($p.parent) { $par = $abs[$p.parent]; $A = @(($par[0] + $pp[0]), ($par[1] + $pp[1]), ($par[2] + $pp[2])) } else { $A = $pp }
    $abs[$p.name] = $A
    $origin = @((-$A[0]), (24 - $A[1]), $A[2])
    $rot = @((-[double]$p.rotation[0]), (-[double]$p.rotation[1]), ([double]$p.rotation[2]))
    $g = [ordered]@{ name = $p.name; origin = $origin; rot = $rot; children = New-Object System.Collections.Generic.List[object]; uuid = [guid]::NewGuid().ToString() }
    $groups[$p.name] = $g
    if ($p.parent) { $groups[$p.parent].children.Add($g) } else { $order += $g }

    $i = 0
    foreach ($c in $p.cubes) {
        $o = $c.origin; $s = $c.size
        $from = @((-($A[0] + $o[0] + $s[0])), (24 - ($A[1] + $o[1] + $s[1])), ($A[2] + $o[2]))
        $to   = @((-($A[0] + $o[0])),         (24 - ($A[1] + $o[1])),         ($A[2] + $o[2] + $s[2]))
        $w = [math]::Floor([double]$s[0] + 1e-7); $h = [math]::Floor([double]$s[1] + 1e-7); $d = [math]::Floor([double]$s[2] + 1e-7)
        $u = [double]$c.uv[0]; $v = [double]$c.uv[1]
        $faces = [ordered]@{
            east  = @(0, $d, $d, $h); west = @(($d + $w), $d, $d, $h)
            up    = @(($d + $w), $d, (-$w), (-$d)); down = @(($d + 2 * $w), 0, (-$w), $d)
            south = @((2 * $d + $w), $d, $w, $h); north = @($d, $d, $w, $h)
        }
        $mir = [bool]$c.mirror
        if ($mir) {
            foreach ($k in @($faces.Keys)) { $f = $faces[$k]; $faces[$k] = @(($f[0] + $f[2]), $f[1], (-$f[2]), $f[3]) }
            $e = $faces['east']; $faces['east'] = $faces['west']; $faces['west'] = $e
        }
        $fj = ($faces.Keys | ForEach-Object { $f = $faces[$_]
            "`"$_`":{`"uv`":" + (Arr @(($u + $f[0]), ($v + $f[1]), ($u + $f[0] + $f[2]), ($v + $f[1] + $f[3]))) + ",`"texture`":0}" }) -join ','
        $uuid = [guid]::NewGuid().ToString()
        $name = if ($p.cubes.Count -gt 1) { "$($p.name)_$i" } else { $p.name }
        $inf = if ($c.inflate) { F $c.inflate } else { '0' }
        $elements.Add("{`"name`":`"$(Esc $name)`",`"box_uv`":true,`"rescale`":false,`"locked`":false,`"render_order`":`"default`",`"allow_mirror_modeling`":true,`"from`":$(Arr $from),`"to`":$(Arr $to),`"autouv`":0,`"color`":$($i % 8),`"origin`":$(Arr $origin),`"uv_offset`":[$u,$v],`"mirror_uv`":$($mir.ToString().ToLower()),`"inflate`":$inf,`"faces`":{$fj},`"type`":`"cube`",`"visibility`":$(($Hide -notcontains $p.name).ToString().ToLower()),`"uuid`":`"$uuid`"}")
        $g.children.Add($uuid)
        $i++
    }
}

function GroupJson($g) {
    $kids = ($g.children | ForEach-Object { if ($_ -is [string]) { "`"$_`"" } else { GroupJson $_ } }) -join ','
    "{`"name`":`"$(Esc $g.name)`",`"origin`":$(Arr $g.origin),`"rotation`":$(Arr $g.rot),`"color`":0,`"uuid`":`"$($g.uuid)`",`"export`":true,`"mirror_uv`":false,`"isOpen`":false,`"locked`":false,`"visibility`":$(($Hide -notcontains $g.name).ToString().ToLower()),`"autouv`":0,`"children`":[$kids]}"
}

$tw = $gd.texture_size[0]; $th = $gd.texture_size[1]
function TexJson($path, $id, $mode) {
    $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($path))
    "{`"path`":`"`",`"name`":`"$([IO.Path]::GetFileName($path))`",`"folder`":`"entity`",`"namespace`":`"skywyrms`",`"id`":`"$id`",`"width`":$tw,`"height`":$th,`"uv_width`":$tw,`"uv_height`":$th,`"particle`":false,`"render_mode`":`"$mode`",`"render_sides`":`"auto`",`"visible`":true,`"mode`":`"bitmap`",`"saved`":true,`"uuid`":`"$([guid]::NewGuid())`",`"source`":`"data:image/png;base64,$b64`"}"
}
$texJson = TexJson $Tex 0 'default'
if ($Glow) {
    # glow overlay: Blockbench draws "layered" textures on top of the base one
    $texJson += ',' + (TexJson $Glow 1 'layered')
}

$modelName = [IO.Path]::GetFileNameWithoutExtension($Out)
$json = "{`"meta`":{`"format_version`":`"5.0`",`"model_format`":`"modded_entity`",`"box_uv`":true},`"name`":`"$modelName`",`"model_identifier`":`"$modelName`",`"modded_entity_entity_class`":`"`",`"modded_entity_version`":`"1.17`",`"modded_entity_flip_y`":true,`"visible_box`":[1,1,0],`"variable_placeholders`":`"`",`"variable_placeholder_buttons`":[],`"timeline_setups`":[],`"unhandled_root_fields`":{},`"resolution`":{`"width`":$tw,`"height`":$th},`"elements`":[$($elements -join ',')],`"outliner`":[$(($order | ForEach-Object { GroupJson $_ }) -join ',')],`"textures`":[$texJson]}"
[IO.File]::WriteAllText($Out, $json, (New-Object Text.UTF8Encoding $false))
"parts: $($gd.parts.Count)  cubes: $($elements.Count)  -> $Out"
