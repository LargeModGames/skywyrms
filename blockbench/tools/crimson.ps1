param([string]$SrcGeo, [string]$SrcTex, [string]$OutGeo, [string]$OutTex)
$ErrorActionPreference = 'Stop'
$gd = Get-Content $SrcGeo -Raw | ConvertFrom-Json
$byName = @{}; foreach ($p in $gd.parts) { $byName[$p.name] = $p }

function Copy-Cube($c) { $c | ConvertTo-Json -Depth 5 | ConvertFrom-Json }
function Thin($c, [double]$t) {
    # shrink width/height around the centre; strips (h<=1) only move towards the axis
    if ([double]$c.size[1] -le 1) {
        $nw = [math]::Max(1, [double]$c.size[0] - [math]::Floor($t / 2))
        $c.origin[0] = [double]$c.origin[0] + ([double]$c.size[0] - $nw) / 2; $c.size[0] = $nw
        $c.origin[1] = [double]$c.origin[1] + $(if ([double]$c.origin[1] -gt 0) { -$t / 2 } else { $t / 2 })
    } else {
        foreach ($i in 0, 1) {
            $n = [math]::Max(2, [double]$c.size[$i] - $t)
            $c.origin[$i] = [double]$c.origin[$i] + ([double]$c.size[$i] - $n) / 2; $c.size[$i] = $n
        }
    }
}

# --- long, slender neck and tail: each segment gets a second cube further along, child pivots move out ---
$chains = @(
    @{ parts = 'neck1', 'neck2', 'neck3', 'neck4'; dir = -1; ext = 8; seg = 11; thin = 2 },
    @{ parts = 'tail1', 'tail2', 'tail3'; dir = 1; ext = 10; seg = 12; thin = 2 },
    @{ parts = 'tail4', 'tail5', 'tail6'; dir = 1; ext = 10; seg = 12; thin = 1 }
)
$chainNames = 'neck1', 'neck2', 'neck3', 'neck4', 'head', 'tail1', 'tail2', 'tail3', 'tail4', 'tail5', 'tail6'
foreach ($ch in $chains) {
    foreach ($name in $ch.parts) {
        $p = $byName[$name]
        $new = @()
        foreach ($c in $p.cubes) {
            Thin $c $ch.thin
            $new += $c
            if ([double]$c.size[2] -ge 8) { $d = Copy-Cube $c; $d.origin[2] = [double]$d.origin[2] + $ch.dir * $ch.ext; $new += $d }
        }
        $p.cubes = $new
        foreach ($k in $gd.parts | Where-Object { $_.parent -eq $name }) {
            if ($chainNames -contains $k.name) { $k.pivot[2] = [double]$k.pivot[2] + $ch.dir * $ch.ext }
            else { $k.pivot[2] = [double]$k.pivot[2] * ($ch.seg + $ch.ext) / $ch.seg }
        }
    }
}
# gentler S-curve so the long neck rises instead of folding back
$byName['neck1'].rotation[0] = -34; $byName['neck2'].rotation[0] = -18; $byName['neck3'].rotation[0] = 10
$byName['neck4'].rotation[0] = 26; $byName['head'].rotation[0] = 16

# --- slimmer body ---
$b = $byName['body'].cubes
for ($i = 0; $i -lt 8; $i++) { $t = $(if ($i -lt 4) { 4 } else { 2 }); $n = [double]$b[$i].size[0] - $t; $b[$i].origin[0] = [double]$b[$i].origin[0] + $t / 2; $b[$i].size[0] = $n }
$b[8].origin[0] = [double]$b[8].origin[0] - 2; $b[9].origin[0] = [double]$b[9].origin[0] + 2
foreach ($n in 'leg_bl', 'wing_l') { $byName[$n].pivot[0] = [double]$byName[$n].pivot[0] - 1.5 }
foreach ($n in 'leg_br', 'wing_r') { $byName[$n].pivot[0] = [double]$byName[$n].pivot[0] + 1.5 }

# --- wyvern build: no front legs (groups stay so part names still match the animator) ---
$drop = New-Object System.Collections.Generic.HashSet[string]
foreach ($p in $gd.parts) { if ($p.name -in 'leg_fl', 'leg_fr' -or ($p.parent -and $drop.Contains($p.parent))) { [void]$drop.Add($p.name) } }
foreach ($p in $gd.parts) { if ($drop.Contains($p.name)) { $p.cubes = @() } }

$gd.kind = 'crimson'
[IO.File]::WriteAllText($OutGeo, ($gd | ConvertTo-Json -Depth 10 -Compress), (New-Object Text.UTF8Encoding $false))

# --- crimson texture: blue scales -> deep red, yellow stripes -> dark orange ---
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System; using System.Drawing; using System.Drawing.Imaging; using System.Runtime.InteropServices;
public static class Recolor {
  static void ToHsv(int r,int g,int b,out double h,out double s,out double v){double R=r/255.0,G=g/255.0,B=b/255.0;double mx=Math.Max(R,Math.Max(G,B)),mn=Math.Min(R,Math.Min(G,B)),d=mx-mn;v=mx;s=mx==0?0:d/mx;if(d==0)h=0;else if(mx==R)h=60*(((G-B)/d)%6);else if(mx==G)h=60*((B-R)/d+2);else h=60*((R-G)/d+4);if(h<0)h+=360;}
  static void ToRgb(double h,double s,double v,out int r,out int g,out int b){double c=v*s,x=c*(1-Math.Abs((h/60)%2-1)),m=v-c,R=0,G=0,B=0;if(h<60){R=c;G=x;}else if(h<120){R=x;G=c;}else if(h<180){G=c;B=x;}else if(h<240){G=x;B=c;}else if(h<300){R=x;B=c;}else{R=c;B=x;}r=(int)Math.Round((R+m)*255);g=(int)Math.Round((G+m)*255);b=(int)Math.Round((B+m)*255);}
  public static void Run(string src,string dst){
    using(var bmp=new Bitmap(src)){var img=new Bitmap(bmp.Width,bmp.Height,PixelFormat.Format32bppArgb);using(var gr=Graphics.FromImage(img))gr.DrawImage(bmp,0,0,bmp.Width,bmp.Height);
    var rect=new Rectangle(0,0,img.Width,img.Height);var data=img.LockBits(rect,ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);int n=data.Stride*img.Height;var px=new byte[n];Marshal.Copy(data.Scan0,px,0,n);
    for(int i=0;i<n;i+=4){if(px[i+3]==0)continue;double h,s,v;ToHsv(px[i+2],px[i+1],px[i],out h,out s,out v);
      if(h>=170&&h<=290&&s>0.08){h=(h-220+358)%360;s=Math.Min(1,s*1.35+0.15);v=Math.Min(1,v*1.05);}
      else if(h>=35&&h<=75&&s>0.3){h=22;s=Math.Min(1,s*1.05);v=v*0.85;}
      int r,g,b;ToRgb(h,s,v,out r,out g,out b);px[i+2]=(byte)r;px[i+1]=(byte)g;px[i]=(byte)b;}
    Marshal.Copy(px,0,data.Scan0,n);img.UnlockBits(data);img.Save(dst,ImageFormat.Png);img.Dispose();}}
}
'@
[Recolor]::Run($SrcTex, $OutTex)
"geo + texture written"
