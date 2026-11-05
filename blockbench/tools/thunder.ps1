param([string]$SrcGeo, [string]$SrcTex, [string]$OutGeo, [string]$OutTex)
$ErrorActionPreference = 'Stop'
$gd = Get-Content $SrcGeo -Raw | ConvertFrom-Json
$parts = New-Object System.Collections.Generic.List[object]
foreach ($p in $gd.parts) { $parts.Add($p) }

# bolt texture block lives in the free bottom area of the sheet (UV 0,208 .. 64,240)
function BoltCube([double]$h, [double]$w = 1) {
    [pscustomobject]@{ origin = @((-$w / 2), (-$h), (-$w / 2)); size = @($w, $h, $w); uv = @(0, 208); inflate = 0.0; mirror = $false }
}
function NewPart($name, $parent, $pivot, $rot, $cube) {
    [pscustomobject]@{ name = $name; parent = $parent; pivot = $pivot; rotation = $rot; cubes = @($cube) }
}

# --- dorsal spines -> zig-zag lightning bolts (three kinked segments) ---
$out = New-Object System.Collections.Generic.List[object]
foreach ($p in $parts) {
    $out.Add($p)
    if ($p.name -match '^deco_dorsal_\d+$') {
        $h = [math]::Max(2, [double]$p.cubes[0].size[1]) + 1
        $p.rotation = @(28.0, 0.0, 0.0)
        $p.cubes = @(BoltCube $h)
        $out.Add((NewPart "$($p.name)_z1" $p.name @(0.0, (-$h + 0.5), 0.0) @(-62.0, 0.0, 0.0) (BoltCube ($h * 0.8))))
        $out.Add((NewPart "$($p.name)_z2" "$($p.name)_z1" @(0.0, (-$h * 0.8 + 0.5), 0.0) @(62.0, 0.0, 0.0) (BoltCube ($h * 0.7))))
    }
}
$gd.parts = $out.ToArray()
$byName = @{}; foreach ($p in $gd.parts) { $byName[$p.name] = $p }

# --- glowing, kinked horn tips and tail prongs ---
foreach ($n in 'deco_horn_L_3', 'deco_horn_R_3', 'deco_hornb_L_2', 'deco_hornb_R_2', 'deco_prong_up_2', 'deco_prong_dn_2') {
    $byName[$n].cubes[0].uv = @(0, 220)
}
foreach ($s in 'L', 'R') {
    $sign = $(if ($s -eq 'L') { 1 } else { -1 })
    $byName["deco_horn_${s}_2"].rotation = @(-16.0, (14.0 * $sign), 0.0)
    $byName["deco_horn_${s}_3"].rotation = @(30.0, (-20.0 * $sign), 0.0)
}
$byName['deco_prong_up_2'].rotation = @(-48.0, 0.0, 0.0)
$byName['deco_prong_dn_2'].rotation = @(48.0, 0.0, 0.0)

$gd.kind = 'thunder'
[IO.File]::WriteAllText($OutGeo, ($gd | ConvertTo-Json -Depth 10 -Compress), (New-Object Text.UTF8Encoding $false))

# --- storm-cloud body, electric veins with halo, forked lightning on the wing membranes ---
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System; using System.Drawing; using System.Drawing.Imaging; using System.Runtime.InteropServices;
public static class Thunder {
  static void ToHsv(int r,int g,int b,out double h,out double s,out double v){double R=r/255.0,G=g/255.0,B=b/255.0;double mx=Math.Max(R,Math.Max(G,B)),mn=Math.Min(R,Math.Min(G,B)),d=mx-mn;v=mx;s=mx==0?0:d/mx;if(d==0)h=0;else if(mx==R)h=60*(((G-B)/d)%6);else if(mx==G)h=60*((B-R)/d+2);else h=60*((R-G)/d+4);if(h<0)h+=360;}
  static void ToRgb(double h,double s,double v,out int r,out int g,out int b){h=((h%360)+360)%360;double c=v*s,x=c*(1-Math.Abs((h/60)%2-1)),m=v-c,R=0,G=0,B=0;if(h<60){R=c;G=x;}else if(h<120){R=x;G=c;}else if(h<180){G=c;B=x;}else if(h<240){G=x;B=c;}else if(h<300){R=x;B=c;}else{R=c;B=x;}r=(int)Math.Round((R+m)*255);g=(int)Math.Round((G+m)*255);b=(int)Math.Round((B+m)*255);}
  static int W,H; static byte[] px; static bool[] vein; static bool[] memb;
  static void Set(int x,int y,int r,int g,int b,double a){if(x<0||y<0||x>=W||y>=H)return;int i=(y*W+x)*4;if(px[i+3]==0)return;px[i+2]=(byte)(px[i+2]*(1-a)+r*a);px[i+1]=(byte)(px[i+1]*(1-a)+g*a);px[i]=(byte)(px[i]*(1-a)+b*a);}
  static void Bolt(Random rnd,double x,double y,double ang,int len,int depth){
    double baseAng=ang; int segLeft=0;
    for(int k=0;k<len;k++){
      if(segLeft--<=0){ang=baseAng+(rnd.NextDouble()-0.5)*1.6;segLeft=4+rnd.Next(6);}
      x+=Math.Cos(ang)*1.5; y+=Math.Sin(ang)*1.5;
      int ix=(int)x,iy=(int)y; if(ix<0||iy<0||ix>=W||iy>=H||!memb[iy*W+ix])return;
      for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){int d=Math.Abs(dx)+Math.Abs(dy);if(d==0)continue;if(ix+dx>=0&&iy+dy>=0&&ix+dx<W&&iy+dy<H&&memb[(iy+dy)*W+ix+dx])Set(ix+dx,iy+dy,150,220,255,d<=1?0.55:0.22);}
      Set(ix,iy,255,252,215,1);Set(ix+1,iy,255,240,140,0.8);
      if(depth<2&&rnd.NextDouble()<0.05)Bolt(rnd,x,y,baseAng+(rnd.NextDouble()<0.5?-0.7:0.7),len/2,depth+1);
    }
  }
  public static void Run(string src,string dst,int[] rects){
    using(var bmp=new Bitmap(src)){var img=new Bitmap(bmp.Width,bmp.Height,PixelFormat.Format32bppArgb);using(var gr=Graphics.FromImage(img))gr.DrawImage(bmp,0,0,bmp.Width,bmp.Height);
    W=img.Width;H=img.Height;var rect=new Rectangle(0,0,W,H);var data=img.LockBits(rect,ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);int n=W*H*4;px=new byte[n];Marshal.Copy(data.Scan0,px,0,n);
    vein=new bool[W*H];memb=new bool[W*H];
    for(int p=0;p<W*H;p++){int i=p*4;if(px[i+3]==0)continue;double h,s,v;ToHsv(px[i+2],px[i+1],px[i],out h,out s,out v);
      if(h>=35&&h<=75&&s>0.3){vein[p]=true;h=52;s=0.7;v=1.0;}
      else if(h>=170&&h<=290&&s>0.08){h=248;s=Math.Min(1,s*0.75+0.05);v=v*0.62;}
      int r,g,b;ToRgb(h,s,v,out r,out g,out b);px[i+2]=(byte)r;px[i+1]=(byte)g;px[i]=(byte)b;}
    // halo around the veins
    var halo=new double[W*H];
    for(int y=0;y<H;y++)for(int x=0;x<W;x++){if(!vein[y*W+x])continue;for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++){int X=x+dx,Y=y+dy;if(X<0||Y<0||X>=W||Y>=H)continue;double d=Math.Sqrt(dx*dx+dy*dy);if(d==0||d>3)continue;double a=0.5*(1-d/3.5);if(a>halo[Y*W+X])halo[Y*W+X]=a;}}
    for(int p=0;p<W*H;p++)if(!vein[p]&&halo[p]>0)Set(p%W,p/W,120,200,255,halo[p]);
    // wing membranes: forked bolts
    for(int r=0;r+3<rects.Length;r+=4){for(int y=rects[r+1];y<rects[r+3];y++)for(int x=rects[r];x<rects[r+2];x++)if(x<W&&y<H&&px[(y*W+x)*4+3]!=0)memb[y*W+x]=true;}
    var rnd=new Random(7);
    for(int r=0;r+3<rects.Length;r+=4){int area=(rects[r+2]-rects[r])*(rects[r+3]-rects[r+1]);int count=Math.Max(2,area/3500);
      for(int k=0;k<count*6&&count>0;k++){int x=rnd.Next(rects[r],rects[r+2]),y=rnd.Next(rects[r+1],rects[r+3]);if(!memb[y*W+x])continue;Bolt(rnd,x,y,rnd.NextDouble()*Math.PI*2,40+rnd.Next(40),0);count--;}}
    // bolt texture block (texels 0..64 x 208..240 -> pixels x2)
    int sc=W/256;var rr=new Random(3);
    for(int ty=208;ty<240;ty++)for(int tx=0;tx<64;tx++){double t=rr.NextDouble();int R=255,G=t<0.45?248:(t<0.85?226:252),B=t<0.45?200:(t<0.85?80:235);if(t>0.93){R=170;G=235;B=255;}
      for(int yy=0;yy<sc;yy++)for(int xx=0;xx<sc;xx++){int i=((ty*sc+yy)*W+tx*sc+xx)*4;px[i]=(byte)B;px[i+1]=(byte)G;px[i+2]=(byte)R;px[i+3]=255;}}
    Marshal.Copy(px,0,data.Scan0,n);img.UnlockBits(data);img.Save(dst,ImageFormat.Png);img.Dispose();}}
}
'@
# wing membrane areas in the 512px sheet (x0,y0,x1,y1)
[Thunder]::Run($SrcTex, $OutTex, [int[]]@(122, 2, 222, 108, 226, 2, 330, 120, 102, 125, 290, 226, 296, 125, 340, 226, 330, 18, 492, 120))
"thunder geo + texture written"
