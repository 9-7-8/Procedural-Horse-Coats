# Bakes the four equestrians' profession textures out of one piece of source art.
#
# A villager profession texture is found by vanilla's VillagerProfessionLayer
# purely by convention - <namespace>:textures/entity/villager/profession/<path>.png
# for the profession's registry key - so four professions need four PNGs whether
# or not they differ. Ours differ in exactly one thing: the colour of the hat.
#
#   tools/villagers/equestrian.source.png   the drawn art, shipped nowhere. The
#                                           former horseman.png, kept here the
#                                           way tools/barn/*.source.nbt is kept:
#                                           an input to a bake, not an asset.
#   assets/.../profession/equestrian_*.png  the four generated overlays.
#   assets/.../villager/cowboy.png          the SAME art, hat unrecoloured.
#
# The cowboy's copy is why this writes five files and not four. He is an entity
# and not a profession, but CowboyRenderer draws him as a plains villager wearing
# this same overlay, and he used to read it straight out of the horseman's
# profession folder. That stopped working the moment the horseman became four
# villagers in four colours: there was no longer a file at that path, and the
# nearest one would have handed him somebody else's hat.
#
# So he gets his own copy, and the bake is what keeps the five from drifting -
# repaint the source, re-run this, and the cowboy and all four equestrians move
# together. That is the same guarantee the shared file used to give, held by the
# bake instead of by a path.
#
# ---------------------------------------------------------------------------
# WHERE THE HAT IS - and it is TWO rectangles, not one
# ---------------------------------------------------------------------------
# This is the trap in the whole script, and it was walked into once: the hat is
# in two places on the sheet, a long way apart, because vanilla's VillagerModel
# draws it as two parts.
#
#   hat      texOffs 32,0    the crown - the box that sits on the head
#   hatRim   texOffs 30,47   the brim - a flat 16x16x1 plate, laid horizontal
#
# A 16x16x1 plate unwraps with its two big faces side by side below a one-texel
# strip, so the brim's painted face lands at x=31..46, y=48..63 - the far corner
# of the sheet, nowhere near the crown. Measured 2026-09-18 rather than argued:
#
#   hat crown   x 32..63, y  0..15    192 opaque   (art occupies y 0..11)
#   hat rim     x 31..46, y 48..63    256 opaque   (a solid 16x16 face)
#   rim reverse x 48..63, y 48..63      0 opaque   (only one face is painted)
#   jacket      x  0..27, y 53..57     44 opaque   <- the ONLY non-hat art
#
# Recolouring only the crown is the failure this guards against, and it is a
# quiet one: the villager gets a teal hat with a brown underside, which reads as
# a texture bug rather than as a mistake in a bake. Both rectangles, always.
#
# The 44 texels at x=0..27 are the belt across the villager's coat - part of the
# `jacket` cube, texOffs 0,38 - and they stay brown in all four. One outfit, four
# hats: they are meant to read as one family.
#
# A colour test instead of geometry would not work either. The crown, the brim
# and the belt are all drawn in the same browns, so "recolour the brown texels"
# repaints the coat too.
#
# The counts are asserted below: if the source art is redrawn and the hat stops
# being 448 texels across those two rectangles, this fails loudly rather than
# silently painting half a hat.
#
# ---------------------------------------------------------------------------
# HOW THE RECOLOUR WORKS
# ---------------------------------------------------------------------------
# Not a flat fill. The hat carries about two dozen shades of one brown and all of
# that is its shading - fill it and you get a flat sticker. Each texel is instead
# retinted around the target colour while keeping its own brightness relative to
# the hat's mean:
#
#   scale = luminance(texel) / mean luminance of the hat
#   out   = clamp(target * scale)
#
# so the mean texel comes out exactly the target colour, and every highlight and
# shadow keeps the ratio it was drawn with.
#
# ---------------------------------------------------------------------------
# THE COLOURS, and the one constraint on them
# ---------------------------------------------------------------------------
# Owner's call: four different hats, and NONE of them purple. Purple is taken -
# CowboyOverlayLayer.ARCANE_HAT (0xC9A0FF) tints the arcane dealer's hat, and he
# is the one villager in this mod a player must be able to pick out of a crowd at
# a glance. A fifth purple hat would cost him that.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$src  = Join-Path $PSScriptRoot 'equestrian.source.png'
$out  = Join-Path $root 'src\main\resources\assets\horsegenetics\textures\entity\villager\profession'

# The crown and the brim. See the map above - two rectangles, not one.
$HAT_RECTS = @(
  @{ name = 'crown'; x0 = 32; y0 =  0; x1 = 63; y1 = 15 }
  @{ name = 'brim';  x0 = 31; y0 = 48; x1 = 46; y1 = 63 }
)
$HAT_TEXELS = 448

function Test-InHat($x, $y) {
  foreach ($r in $HAT_RECTS) {
    if ($x -ge $r.x0 -and $x -le $r.x1 -and $y -ge $r.y0 -and $y -le $r.y1) { return $true }
  }
  return $false
}

$HATS = @(
  # Warm russet - he works leather, and the one hat that stays in the family of
  # the brown the art was drawn in.
  @{ id = 'equestrian_leatherworker'; rgb = @(0xA0, 0x52, 0x2D) }
  # Teal. Nothing else in this mod is teal, and it reads as "not a farmhand".
  @{ id = 'equestrian_scientist';     rgb = @(0x1E, 0x7A, 0x7A) }
  # Olive green, for the one who sells carrots and rope.
  @{ id = 'equestrian_supplier';      rgb = @(0x4E, 0x7A, 0x2A) }
  # Cold steel blue-grey, for the one who sells metal.
  @{ id = 'equestrian_metalsmith';    rgb = @(0x55, 0x60, 0x6E) }
)

# Rec. 709. Any sensible luminance would do - what matters is that one formula is
# used for both the mean and each texel, so the ratio is honest.
function Get-Luminance($r, $g, $b) { return 0.2126 * $r + 0.7152 * $g + 0.0722 * $b }

if (-not (Test-Path $src)) { throw "no source art at $src" }
$bmp = [System.Drawing.Bitmap]::FromFile($src)
try {
  if ($bmp.Width -ne 64 -or $bmp.Height -ne 64) {
    throw "source is $($bmp.Width)x$($bmp.Height); a villager overlay is 64x64"
  }

  # ---- measure the hat before touching it ---------------------------------
  # One mean across BOTH rectangles, not one each. The crown and the brim are
  # drawn as one hat and the brim is the darker half of it; normalising them
  # separately would flatten that and give a brim the same tone as the crown.
  $sum = 0.0
  $n = 0
  foreach ($r in $HAT_RECTS) {
    $rn = 0
    for ($y = $r.y0; $y -le $r.y1; $y++) {
      for ($x = $r.x0; $x -le $r.x1; $x++) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.A -eq 0) { continue }
        $sum += Get-Luminance $c.R $c.G $c.B
        $rn++
      }
    }
    Write-Host ("  {0,-6} {1,4} texels" -f $r.name, $rn)
    $n += $rn
  }
  if ($n -ne $HAT_TEXELS) {
    throw "the hat holds $n opaque texels across its two rectangles, expected $HAT_TEXELS - the source art moved, so re-map it before trusting this bake"
  }
  $mean = $sum / $n
  Write-Host ("hat: {0} texels, mean luminance {1:N1}" -f $n, $mean)

  New-Item -ItemType Directory -Force -Path $out | Out-Null

  foreach ($hat in $HATS) {
    $dst = New-Object System.Drawing.Bitmap($bmp.Width, $bmp.Height)
    try {
      # Everything outside the two hat rectangles is copied verbatim: the coat's
      # belt, and every transparent texel. Four villagers, one outfit, four hats.
      for ($y = 0; $y -lt 64; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
          $c = $bmp.GetPixel($x, $y)
          if (-not (Test-InHat $x $y) -or $c.A -eq 0) {
            $dst.SetPixel($x, $y, $c)
            continue
          }
          $scale = (Get-Luminance $c.R $c.G $c.B) / $mean
          $ch = @(0, 1, 2) | ForEach-Object {
            [int][Math]::Max(0, [Math]::Min(255, [Math]::Round($hat.rgb[$_] * $scale)))
          }
          $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $ch[0], $ch[1], $ch[2]))
        }
      }
      $png = Join-Path $out ($hat.id + '.png')
      $dst.Save($png, [System.Drawing.Imaging.ImageFormat]::Png)

      # The .mcmeta is what tells vanilla the hat covers the villager type's own
      # headwear rather than sitting under it. Same for all four, written here so
      # a fifth profession cannot arrive without one.
      Set-Content -Path (Join-Path $out ($hat.id + '.png.mcmeta')) `
        -Value '{"villager": {"hat": "full"}}' -Encoding utf8 -NoNewline

      Write-Host ("  {0}  #{1:X2}{2:X2}{3:X2}" -f $hat.id, $hat.rgb[0], $hat.rgb[1], $hat.rgb[2])
    } finally { $dst.Dispose() }
  }
  # ---- and the cowboy's copy, hat untouched -------------------------------
  # A straight copy of the source. Written by the bake rather than left as a
  # checked-in file so that repainting the art moves him with the other four -
  # see the class note above.
  $cowboyDir = Join-Path $root 'src\main\resources\assets\horsegenetics\textures\entity\villager'
  New-Item -ItemType Directory -Force -Path $cowboyDir | Out-Null
  $bmp.Save((Join-Path $cowboyDir 'cowboy.png'), [System.Drawing.Imaging.ImageFormat]::Png)
  Set-Content -Path (Join-Path $cowboyDir 'cowboy.png.mcmeta') `
    -Value '{"villager": {"hat": "full"}}' -Encoding utf8 -NoNewline
  Write-Host "  cowboy  (unrecoloured)"
} finally { $bmp.Dispose() }

Write-Host "done - $($HATS.Count) profession textures, plus the cowboy's"
