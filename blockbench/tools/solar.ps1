#Requires -PSEdition Desktop
param([string]$SrcGeo, [string]$SrcTex, [string]$OutGeo, [string]$OutTex, [string]$SrcGlow, [string]$OutGlow)
# Solar wyrm: storm wyrm body (same size) with a sun look. A corona of eleven rays behind the head, tall
# sun-ray spines with fiery tips, golden horns, a sun disc on the chest, a fan of rays on the tail tip,
# warm gold scales with white-hot veins, and wings that glow from white at the arm to sunset orange.
$ErrorActionPreference = 'Stop'
$gd = Get-Content $SrcGeo -Raw | ConvertFrom-Json
# painted blocks in the free bottom of the sheet
$GOLD = @(0, 208)     # speckled gold          UV 0,208   .. 128,240
$RAY  = @(128, 208)   # fiery tip -> white-gold UV 128,208 .. 192,240 (top rows are the ray tips)
$SUN  = @(200, 208)   # chest plate with sun   UV 200,208 .. 224,219

function Cube($o, $s, $uv = $GOLD) { [pscustomobject]@{ origin = $o; size = $s; uv = $uv; inflate = 0.0; mirror = $false } }
function Part($name, $parent, $pivot, $rot, $cubes) { [pscustomobject]@{ name = $name; parent = $parent; pivot = $pivot; rotation = $rot; cubes = @($cubes) } }

$out = New-Object System.Collections.Generic.List[object]
foreach ($p in $gd.parts) {
    $out.Add($p)

    # tall, thin sun-ray spines with fiery tips instead of the dorsal plates
    if ($p.name -match '^deco_dorsal_\d+$') {
        $h = [math]::Round([double]$p.cubes[0].size[1] * 1.8)
        $p.rotation = @(18.0, 0.0, 0.0)
        $p.cubes = @(Cube @(-0.5, (-$h), -0.5) @(1, $h, 1) $RAY)
    }

    # corona: a gold disc on the head with eleven rays, alternating long and short
    if ($p.name -eq 'head') {
        # turned a quarter so the rider sees it edge-on (a crest of rays seen from the side)
        $out.Add((Part 'deco_crown' 'head' @(0.0, -5.5, 2.0) @(-10.0, 90.0, 0.0) (Cube @(-2.0, -2.0, -0.5) @(4, 4, 1))))
        $i = 0
        for ($a = -100; $a -le 100; $a += 20) {
            $len = $(if ($i % 2 -eq 0) { 8 } else { 5 })
            $out.Add((Part "deco_crown_ray_$i" 'deco_crown' @(0.0, 0.0, 0.0) @(0.0, 0.0, [double]$a) (Cube @(-0.5, (-2.5 - $len), -0.5) @(1, $len, 1) $RAY)))
            $i++
        }
    }

    # tail tip: the fork becomes a fan of five rays
    if ($p.name -eq 'deco_tailfork') {
        $p.cubes = @(Cube @(-1.5, -1.5, -1.0) @(3, 3, 3))
        $i = 0
        foreach ($a in -60, -30, 0, 30, 60) {
            $len = @(6, 4, 8, 4, 6)[$i]
            $out.Add((Part "deco_tailsun_$i" 'deco_tailfork' @(0.0, 0.0, 1.5) @([double]$a, 0.0, 0.0) (Cube @(-0.5, -0.5, 0.0) @(1, 1, $len))))
            $i++
        }
    }
    if ($p.name -match '^deco_prong_') { $p.cubes = @() }
}
$gd.parts = $out.ToArray()

foreach ($p in $gd.parts) {
    if ($p.name -match '^deco_hornb?_[LR]_\d$') { foreach ($c in $p.cubes) { $c.uv = $GOLD } }   # golden horns
    if ($p.name -eq 'body') { $p.cubes[7].uv = $SUN }                                              # chest plate (10x9x2, front)
}
$gd.kind = 'solar'
[IO.File]::WriteAllText($OutGeo, ($gd | ConvertTo-Json -Depth 10 -Compress), (New-Object Text.UTF8Encoding $false))


Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System; using System.Drawing; using System.Drawing.Imaging; using System.Runtime.InteropServices;
public static class Solar {
  static void ToHsv(int r,int g,int b,out double h,out double s,out double v){double R=r/255.0,G=g/255.0,B=b/255.0;double mx=Math.Max(R,Math.Max(G,B)),mn=Math.Min(R,Math.Min(G,B)),d=mx-mn;v=mx;s=mx==0?0:d/mx;if(d==0)h=0;else if(mx==R)h=60*(((G-B)/d)%6);else if(mx==G)h=60*((B-R)/d+2);else h=60*((R-G)/d+4);if(h<0)h+=360;}
  static void ToRgb(double h,double s,double v,out int r,out int g,out int b){h=((h%360)+360)%360;double c=v*s,x=c*(1-Math.Abs((h/60)%2-1)),m=v-c,R=0,G=0,B=0;if(h<60){R=c;G=x;}else if(h<120){R=x;G=c;}else if(h<180){G=c;B=x;}else if(h<240){G=x;B=c;}else if(h<300){R=x;B=c;}else{R=c;B=x;}r=(int)Math.Round((R+m)*255);g=(int)Math.Round((G+m)*255);b=(int)Math.Round((B+m)*255);}
  static int W,H; static byte[] px; static double[] glow;
  static void Blend(int p,int r,int g,int b,double a){int i=p*4;px[i+2]=(byte)(px[i+2]*(1-a)+r*a);px[i+1]=(byte)(px[i+1]*(1-a)+g*a);px[i]=(byte)(px[i]*(1-a)+b*a);}
  static void Put(int x,int y,int r,int g,int b,double gl){if(x<0||y<0||x>=W||y>=H)return;int p=y*W+x,i=p*4;px[i]=(byte)b;px[i+1]=(byte)g;px[i+2]=(byte)r;px[i+3]=255;if(gl>glow[p])glow[p]=gl;}
  static void Glow(int p,double a){if(a>glow[p])glow[p]=Math.Min(1,a);}
  public static void Run(string src,string dst,string glowSrc,string glowDst,int[] rects){
    using(var bmp=new Bitmap(src)){var img=new Bitmap(bmp.Width,bmp.Height,PixelFormat.Format32bppArgb);using(var gr=Graphics.FromImage(img))gr.DrawImage(bmp,0,0,bmp.Width,bmp.Height);
    W=img.Width;H=img.Height;var data=img.LockBits(new Rectangle(0,0,W,H),ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);int n=W*H*4;px=new byte[n];Marshal.Copy(data.Scan0,px,0,n);
    int sc=W/256;var vein=new bool[W*H];glow=new double[W*H];
    // scales: bright yellow-gold, shading into deeper gold; stripes become white-hot veins
    for(int p=0;p<W*H;p++){int i=p*4;if(px[i+3]==0)continue;int qx=p%W/sc,qy=p/W/sc;if(qx>=98&&qx<132&&qy>=150&&qy<168)continue; /* saddle */ double h,s,v;ToHsv(px[i+2],px[i+1],px[i],out h,out s,out v);
      if(h>=35&&h<=75&&s>0.3){vein[p]=true;h=52;s=0.05;v=1.0;Glow(p,1);}
      else if(h>=170&&h<=290&&s>0.08){double dark=1-v;h=50-dark*9;s=Math.Min(1,0.42+dark*0.38);v=Math.Min(1,0.76+v*0.4);}
      int r,g,b;ToRgb(h,s,v,out r,out g,out b);px[i+2]=(byte)r;px[i+1]=(byte)g;px[i]=(byte)b;}
    // soft sunlight glow around the veins
    var halo=new double[W*H];
    for(int y=0;y<H;y++)for(int x=0;x<W;x++){if(!vein[y*W+x])continue;for(int dy=-4;dy<=4;dy++)for(int dx=-4;dx<=4;dx++){int X=x+dx,Y=y+dy;if(X<0||Y<0||X>=W||Y>=H)continue;double d=Math.Sqrt(dx*dx+dy*dy);if(d==0||d>4)continue;double a=0.65*(1-d/4.5);if(a>halo[Y*W+X])halo[Y*W+X]=a;}}
    for(int p=0;p<W*H;p++)if(!vein[p]&&halo[p]>0&&px[p*4+3]!=0){Blend(p,255,238,150,halo[p]);Glow(p,halo[p]*1.1);}
    // wing membranes: white at the arm warming to soft gold at the edge, with glowing golden rays
    for(int r=0;r+3<rects.Length;r+=4){int x0=rects[r],y0=rects[r+1],x1=rects[r+2],y1=rects[r+3];double diag=Math.Sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0));
      for(int y=y0;y<y1&&y<H;y++)for(int x=x0;x<x1&&x<W;x++){int p=y*W+x;if(px[p*4+3]==0||vein[p])continue;
        double dx=x-x0,dy=y-y0,dist=Math.Min(1,Math.Sqrt(dx*dx+dy*dy)/diag*1.15),ang=Math.Atan2(dy,dx);
        Blend(p,255,(int)(252-40*dist),(int)(240-120*dist),0.85);
        double ray=Math.Pow(Math.Max(0,Math.Cos(ang*18)),24)*(1-dist*0.5);
        if(ray>0.05){Blend(p,252,200,50,Math.Min(0.85,ray*1.2));Glow(p,ray*1.3);}}}
    // a glowing sun on every membrane panel (panel box x0,y0,x1,y1 and sun centre cx,cy in 512px texture space)
    int[] suns={122,2,222,108,165,58, 226,2,330,120,268,60, 330,18,410,120,372,62, 410,18,492,120,452,62, 102,125,175,226,148,195, 175,125,290,226,235,190};
    for(int q=0;q+5<suns.Length;q+=6){int x0=suns[q],y0=suns[q+1],x1=suns[q+2],y1=suns[q+3];double scx=suns[q+4],scy=suns[q+5],R=Math.Min(x1-x0,y1-y0)*0.17;
      for(int y=y0;y<y1&&y<H;y++)for(int x=x0;x<x1&&x<W;x++){int p=y*W+x;if(px[p*4+3]==0)continue;
        double dx=x+0.5-scx,dy=y+0.5-scy,d=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx);
        double ray=Math.Pow(Math.Max(0,Math.Cos(a*6)),4),ray2=Math.Pow(Math.Max(0,Math.Cos(a*6+Math.PI/2)),6);
        if(d<R*0.62){Blend(p,255,252,232,1.0);Glow(p,1.0);}
        else if(d<R*0.85){Blend(p,255,226,110,1.0);Glow(p,1.0);}
        else if(d<R){Blend(p,255,170,45,0.95);Glow(p,0.9);}
        else if(d<R*2.6&&ray>0.55){double fade=1-(d-R)/(R*1.6);Blend(p,255,200,60,0.85*fade+0.15);Glow(p,0.85*fade+0.15);}
        else if(d<R*1.8&&ray2>0.55){double fade=1-(d-R)/(R*0.8);Blend(p,255,185,55,0.8*fade);Glow(p,0.7*fade);}
      }
    }
    var rnd=new Random(5);
    // GOLD block (horns, crown disc, tail rays): not glowing
    for(int ty=208;ty<240;ty++)for(int tx=0;tx<128;tx++){double t=rnd.NextDouble();int G=(int)(205+t*42),B=(int)(70+t*90);if(t>0.92){G=250;B=220;}
      for(int yy=0;yy<sc;yy++)for(int xx=0;xx<sc;xx++)Put(tx*sc+xx,ty*sc+yy,255,G,B,0);}
    // RAY block (crown rays, spines): tips orange-gold -> yellow -> white-gold, all glowing
    for(int ty=208;ty<240;ty++)for(int tx=128;tx<192;tx++){int row=ty-208;int G,B;
      if(row<=2){G=165;B=40;}else if(row<=4){G=200;B=60;}else if(row<=6){G=230;B=110;}else{G=246;B=205;}
      if(rnd.NextDouble()<0.12){G=Math.Min(255,G+20);B=Math.Min(255,B+25);}
      for(int yy=0;yy<sc;yy++)for(int xx=0;xx<sc;xx++)Put(tx*sc+xx,ty*sc+yy,255,G,B,1);}
    // SUN block: gold chest plate with a glowing sun disc on the front face (north face = u+2..u+12, v+2..v+11)
    for(int ty=208;ty<219;ty++)for(int tx=200;tx<224;tx++){double t=rnd.NextDouble();for(int yy=0;yy<sc;yy++)for(int xx=0;xx<sc;xx++)Put(tx*sc+xx,ty*sc+yy,255,(int)(200+t*40),(int)(60+t*50),0);}
    double cx=(202+5)*sc,cy=(210+4.5)*sc;
    for(int y=210*sc;y<219*sc;y++)for(int x=202*sc;x<212*sc;x++){double dx=x+0.5-cx,dy=y+0.5-cy,d=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx);
      double rays=Math.Pow(Math.Max(0,Math.Cos(a*6)),6);
      if(d<1.8*sc)Put(x,y,255,253,235,1);else if(d<2.8*sc)Put(x,y,255,232,110,1);else if(d<3.4*sc)Put(x,y,255,195,60,1);else if(d<4.4*sc&&rays>0.5)Put(x,y,255,215,80,1);}
    // eyes: whatever the storm glow texture lights up that is not a lightning stripe
    using(var gb=new Bitmap(glowSrc)){for(int y=0;y<H&&y<gb.Height;y++)for(int x=0;x<W&&x<gb.Width;x++){var c=gb.GetPixel(x,y);if(c.A<40)continue;double h,s,v;ToHsv(c.R,c.G,c.B,out h,out s,out v);if(h>=35&&h<=75&&s>0.3)continue;int p=y*W+x;if(px[p*4+3]==0)continue;Put(x,y,255,240,190,1);}}
    Marshal.Copy(px,0,data.Scan0,n);img.UnlockBits(data);img.Save(dst,ImageFormat.Png);
    // glow texture: lit colour where it glows, transparent elsewhere (drawn full-bright by DragonGlowLayer)
    var gimg=new Bitmap(W,H,PixelFormat.Format32bppArgb);var gd=gimg.LockBits(new Rectangle(0,0,W,H),ImageLockMode.WriteOnly,PixelFormat.Format32bppArgb);var gp=new byte[n];
    for(int p=0;p<W*H;p++){if(glow[p]<0.08)continue;int i=p*4;gp[i]=px[i];gp[i+1]=px[i+1];gp[i+2]=px[i+2];gp[i+3]=(byte)(255*glow[p]);}
    Marshal.Copy(gp,0,gd.Scan0,n);gimg.UnlockBits(gd);gimg.Save(glowDst,ImageFormat.Png);gimg.Dispose();img.Dispose();}}
}
'@
[Solar]::Run($SrcTex, $OutTex, $SrcGlow, $OutGlow, [int[]]@(122, 2, 222, 108, 226, 2, 330, 120, 102, 125, 290, 226, 296, 125, 340, 226, 330, 18, 492, 120))
"solar geo, texture and glow written"
