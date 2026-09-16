# Bakes this mod's dyeable saddle textures out of vanilla's.
#
# Vanilla's saddle is one texture of brown leather with grey metal hardware, and
# it is NOT dyeable: vanilla builds the saddle equipment asset with the plain
# single-argument Layer constructor, dyeable empty. To tint it we ship our own
# asset with one layer per zone, each layer dyeable, each with its own texture.
#
# THREE ZONES, and they are found two different ways. That distinction is the
# whole reason this script is not trivial:
#
#   METAL  - the buckles and rings. Found by COLOUR: vanilla's sheet holds six
#            saturated browns and three pure greys, so hardware is exactly
#            "R equals G equals B".
#   SEAT   - the seat cube.
#   BRIDLE - the crownpiece, noseband, bits and reins.
#
# Seat and bridle are BOTH saturated brown, so no colour test can separate them.
# They are separated GEOMETRICALLY, by which of EquineSaddleModel's cubes owns
# the texel. The cube list and the face layout below are that model's, and the
# two groups were measured to share no texels at all (seat 513 claimed, bridle
# 374, zero overlap), which is what makes a clean three-way split possible.
#
# Each zone's base is vanilla's colour DIVIDED BY the tint applied when undyed,
# so base x undyed reproduces vanilla exactly and an un-dyed saddle - on a horse,
# a donkey, a mule, a skeleton or a zombie horse - is pixel-identical to the one
# players already know. Every zone asserts that round trip.
#
# WINDOWS ONLY: uses System.Drawing because the repo's Node tools have no PNG
# codec. If this ever needs to run elsewhere, port it - the rules above are the
# whole spec.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$jar = Join-Path $repo 'neoforge-26.1.2\build\moddev\artifacts\minecraft-patched-26.1.2.100-merged.jar'
$dest = Join-Path $repo 'neoforge-26.1.2\src\main\resources\assets\minecraft\textures\entity\equipment'

# Undyed tints, one per zone. Leather uses vanilla's own leather constant so our
# asset and vanilla's agree on what "undyed leather" means. Metal uses its own
# brightest grey, because dividing the greys by the leather brown would clamp.
$UNDYED_LEATHER = @{ R = 0xA0; G = 0x65; B = 0x40 }   # 0xFFA06540 == -6265536
$UNDYED_METAL   = @{ R = 0x71; G = 0x71; B = 0x71 }   # 0xFF717171 == -9342607

# Horse family only. Deliberately NOT camel, pig, strider or nautilus: this is a
# horse mod and has no business repainting them.
$FOLDERS = @('horse_saddle', 'donkey_saddle', 'mule_saddle', 'skeleton_horse_saddle', 'zombie_horse_saddle')

# EquineSaddleModel's cubes. name, zone, texOffs u/v, box w/h/d.
$CUBES = @(
  @{ n='saddle';             z='SEAT';   u=26; v=0; w=10; h=9;  d=9  },
  @{ n='head_saddle';        z='BRIDLE'; u=1;  v=1; w=6;  h=5;  d=6  },
  @{ n='mouth_saddle_wrap';  z='BRIDLE'; u=19; v=0; w=4;  h=5;  d=2  },
  @{ n='left_saddle_mouth';  z='BRIDLE'; u=29; v=5; w=1;  h=2;  d=2  },
  @{ n='right_saddle_mouth'; z='BRIDLE'; u=29; v=5; w=1;  h=2;  d=2  },
  @{ n='left_saddle_line';   z='BRIDLE'; u=32; v=2; w=0;  h=3;  d=16 },
  @{ n='right_saddle_line';  z='BRIDLE'; u=32; v=2; w=0;  h=3;  d=16 }
)

# Minecraft's per-cube unwrap, for a box (w,h,d) at texOffs (u,v).
function Get-Faces($c) {
  $u=$c.u; $v=$c.v; $w=$c.w; $h=$c.h; $d=$c.d
  ,@(
    @{x=$u+$d;       y=$v;    cw=$w; ch=$d},   # down
    @{x=$u+$d+$w;    y=$v;    cw=$w; ch=$d},   # up
    @{x=$u;          y=$v+$d; cw=$d; ch=$h},   # west
    @{x=$u+$d;       y=$v+$d; cw=$w; ch=$h},   # north
    @{x=$u+$d+$w;    y=$v+$d; cw=$d; ch=$h},   # east
    @{x=$u+$d+$w+$d; y=$v+$d; cw=$w; ch=$h}    # south  (west d | north w | east d | south w)
  )
}

# texel -> zone, from the cube geometry. Built once; the sheet layout is the
# same for every folder because all five vanilla textures are byte-identical.
$zoneOf = @{}
foreach ($c in $CUBES) {
  foreach ($f in (Get-Faces $c)) {
    if ($f.cw -le 0 -or $f.ch -le 0) { continue }
    for ($y = $f.y; $y -lt $f.y + $f.ch; $y++) {
      for ($x = $f.x; $x -lt $f.x + $f.cw; $x++) {
        if ($x -lt 0 -or $x -ge 64 -or $y -lt 0 -or $y -ge 64) { continue }
        $k = "$x,$y"
        if ($zoneOf.ContainsKey($k) -and $zoneOf[$k] -ne $c.z) {
          throw "texel $k is claimed by both SEAT and BRIDLE - the zones are not disjoint"
        }
        $zoneOf[$k] = $c.z
      }
    }
  }
}

function Read-VanillaEntry([string]$path) {
  $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
  try {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq $path }
    if (-not $entry) { throw "not in the jar: $path" }
    $ms = New-Object System.IO.MemoryStream
    $entry.Open().CopyTo($ms); $ms.Position = 0
    return New-Object System.Drawing.Bitmap($ms)
  } finally { $zip.Dispose() }
}

function Split-Channel($value, $divisor) {
  return [int][Math]::Min(255, [Math]::Round(255.0 * $value / $divisor))
}

$anyFailed = $false
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

foreach ($folder in $FOLDERS) {
  $src = Read-VanillaEntry "assets/minecraft/textures/entity/equipment/$folder/saddle.png"
  $w = $src.Width; $h = $src.Height

  $img = @{ SEAT = New-Object System.Drawing.Bitmap($w,$h)
            BRIDLE = New-Object System.Drawing.Bitmap($w,$h)
            METAL = New-Object System.Drawing.Bitmap($w,$h) }
  $count = @{ SEAT = 0; BRIDLE = 0; METAL = 0 }
  $worst = 0; $orphans = 0

  for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
      $c = $src.GetPixel($x, $y)
      foreach ($z in @('SEAT','BRIDLE','METAL')) { $img[$z].SetPixel($x, $y, $clear) }
      if ($c.A -eq 0) { continue }

      if ($c.R -eq $c.G -and $c.G -eq $c.B) {
        $zone = 'METAL'; $u = $UNDYED_METAL
      } else {
        $k = "$x,$y"
        if (-not $zoneOf.ContainsKey($k)) { $orphans++; continue }
        $zone = $zoneOf[$k]; $u = $UNDYED_LEATHER
      }

      $r = Split-Channel $c.R $u.R; $g = Split-Channel $c.G $u.G; $b = Split-Channel $c.B $u.B
      $img[$zone].SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $r, $g, $b))
      $count[$zone]++

      $back = @([Math]::Round($r*$u.R/255.0), [Math]::Round($g*$u.G/255.0), [Math]::Round($b*$u.B/255.0))
      foreach ($i in 0..2) {
        $err = [Math]::Abs($back[$i] - @($c.R,$c.G,$c.B)[$i])
        if ($err -gt $worst) { $worst = $err }
      }
    }
  }

  $outDir = Join-Path $dest $folder
  New-Item -ItemType Directory -Force -Path $outDir | Out-Null
  $img.SEAT.Save((Join-Path $outDir 'saddle.png'), [System.Drawing.Imaging.ImageFormat]::Png)
  $img.BRIDLE.Save((Join-Path $outDir 'saddle_bridle.png'), [System.Drawing.Imaging.ImageFormat]::Png)
  $img.METAL.Save((Join-Path $outDir 'saddle_metal.png'), [System.Drawing.Imaging.ImageFormat]::Png)
  # The old two-way split's overlay; metal is a dyeable layer of its own now.
  $stale = Join-Path $outDir 'saddle_overlay.png'
  if (Test-Path $stale) { Remove-Item $stale -Force }

  $ok = ($worst -le 1 -and $orphans -eq 0 -and $count.SEAT -gt 0 -and $count.BRIDLE -gt 0 -and $count.METAL -gt 0)
  if (-not $ok) { $anyFailed = $true }
  Write-Output ("{0,-24} seat {1,4}  bridle {2,4}  metal {3,3}  orphan {4,3}  worst undyed error {5}  {6}" -f `
      $folder, $count.SEAT, $count.BRIDLE, $count.METAL, $orphans, $worst, $(if ($ok) { 'ok' } else { 'FAILED' }))
  if ($orphans -gt 0) {
    Write-Output ("  -> {0} painted texel(s) belong to no cube; the cube list is incomplete" -f $orphans)
  }

  $src.Dispose(); foreach ($z in @('SEAT','BRIDLE','METAL')) { $img[$z].Dispose() }
}

# --- the inventory icon -----------------------------------------------------
# One zone only. The icon is a flat picture with no geometry behind it, so there
# is nothing to separate seat from bridle by - and its three hardware pixels are
# WARM greys (~22% saturation), which the neutral test cannot see anyway. Three
# pixels out of ninety-two on a 16x16 icon are not worth a second layer.
#
# It needs its OWN undyed constant: its palette is genuinely brighter than the
# sheet's (maxima 241/153/136 against 138/85/46), so the leather divisor clamps.
$ITEM_UNDYED = @{ R = 0xF1; G = 0x99; B = 0x88 }   # 0xFFF19988 == -943736

$itemDir = Join-Path $repo 'neoforge-26.1.2\src\main\resources\assets\minecraft\textures\item'
$src = Read-VanillaEntry 'assets/minecraft/textures/item/saddle.png'
$base = New-Object System.Drawing.Bitmap($src.Width, $src.Height)
$leather = 0; $worstItem = 0; $mR = 0; $mG = 0; $mB = 0

for ($y = 0; $y -lt $src.Height; $y++) {
  for ($x = 0; $x -lt $src.Width; $x++) {
    $c = $src.GetPixel($x, $y)
    if ($c.A -eq 0) { $base.SetPixel($x, $y, $clear); continue }
    if ($c.R -gt $mR) { $mR = $c.R }; if ($c.G -gt $mG) { $mG = $c.G }; if ($c.B -gt $mB) { $mB = $c.B }
    $r = Split-Channel $c.R $ITEM_UNDYED.R; $g = Split-Channel $c.G $ITEM_UNDYED.G; $b = Split-Channel $c.B $ITEM_UNDYED.B
    $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $r, $g, $b)); $leather++
    $back = @([Math]::Round($r*$ITEM_UNDYED.R/255.0), [Math]::Round($g*$ITEM_UNDYED.G/255.0), [Math]::Round($b*$ITEM_UNDYED.B/255.0))
    foreach ($i in 0..2) {
      $err = [Math]::Abs($back[$i] - @($c.R,$c.G,$c.B)[$i])
      if ($err -gt $worstItem) { $worstItem = $err }
    }
  }
}

New-Item -ItemType Directory -Force -Path $itemDir | Out-Null
$base.Save((Join-Path $itemDir 'saddle.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$clamps = ($mR -gt $ITEM_UNDYED.R -or $mG -gt $ITEM_UNDYED.G -or $mB -gt $ITEM_UNDYED.B)
$itemOk = ($worstItem -le 1 -and $leather -gt 0 -and -not $clamps)
if (-not $itemOk) { $anyFailed = $true }
Write-Output ("{0,-24} leather {1,4}  max {2}/{3}/{4}  worst undyed error {5}  {6}" -f `
    'item icon', $leather, $mR, $mG, $mB, $worstItem, $(if ($itemOk) { 'ok' } else { 'FAILED' }))
if ($clamps) { Write-Output '  -> a channel exceeds the icon divisor; raise $ITEM_UNDYED and the tint default together' }
$src.Dispose(); $base.Dispose()

if ($anyFailed) {
  Write-Output ''
  Write-Output 'FAILED: an undyed saddle would not match vanilla. Do not ship this.'
  exit 1
}
Write-Output ''
Write-Output 'All saddles baked in three zones. Undyed renders pixel-identical to vanilla.'
exit 0
