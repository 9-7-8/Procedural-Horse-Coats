# Bakes the ONE greyscale plate-armour tint mask that every generated horse
# armour shares, out of vanilla's iron horse armour.
#
# WHY THERE IS ONLY ONE. This mod grows a horse armour for every ingot or gem
# another mod puts in the game, and the obvious implementation - recolour the
# iron sheet per material and hand the pack a PNG each - scales with the number
# of mods installed and bakes a guess about each metal's colour into an image
# nobody can change. Instead every generated armour points at the SAME two
# textures baked here and carries
#     {"dyeable": {"color_when_undyed": <the metal's colour>}}
# in its equipment asset, and vanilla multiplies that colour over the texture at
# render time. So the material list can be discovered at runtime and these two
# files never change again.
#
# That makes the requirement on these files absolute: they are TINT MASKS, not
# pictures. White texel times gold equals gold. A texel that is 60% grey times
# gold is 60% gold. Anything left in the art that is NOT a neutral grey survives
# the multiply as itself and shows up as a stain on every material.
#
# ---------------------------------------------------------------------------
# WHAT WAS MEASURED, 2026-09-18, and the one thing that surprised me
# ---------------------------------------------------------------------------
# The brief assumed iron's art was already near-neutral and the job was mostly
# normalisation. That is true of 93% of it and FALSE of the rest.
#
#   horse_body/iron.png   64x64, 1200 opaque texels, 27 distinct colours.
#                         1120 are neutral (R==G==B). EIGHTY ARE NOT, and they
#                         are not a cast - they are a saturated dark-red ramp:
#                         (34,2,2) (41,3,3) (58,4,4) (69,5,5) (81,7,7), channel
#                         spread up to 74. That is a deliberate RED STRAP in
#                         vanilla's artwork: two bands across the barrel at
#                         y=38..39 and y=45..46, x=22..31, and the leg wraps at
#                         x=6..8 / x=14..15 down rows 54..60.
#   item/iron_horse_armor.png  16x16, 89 opaque texels, 12 distinct colours.
#                         79 neutral, TEN the same red - the strap crossing the
#                         icon at x=5 and x=8, rows 8..12.
#
# It is invisible at a glance because every red texel is dark (luminance 9 to
# 23 out of 255), which is exactly what makes it dangerous: left alone, a gold
# plate armour would render with a dull red strap and an amethyst one likewise,
# because a red texel multiplied by gold is still red-ish and multiplied by
# anything is never that thing's colour.
#
# So this script DESATURATES BY LUMINANCE (Rec. 709: 0.2126 R, 0.7152 G,
# 0.0722 B) rather than pretending the sheet is neutral. The red strap becomes a
# dark grey strap, which still reads as a strap - it keeps its shape and its
# shading, it just takes the material's colour like everything else. THE RED IS
# GONE ON PURPOSE AND CANNOT BE KEPT: one texture multiplied by one colour has
# no way to hold a second, fixed hue. Wanting the strap back means a second,
# untinted overlay layer in the equipment asset - the shape
# bake-leather-armor-textures.ps1 uses for its SHADOW zone - not a change here.
#
# ---------------------------------------------------------------------------
# NORMALISATION - the same divide-a-colour-out trick as its two siblings
# ---------------------------------------------------------------------------
# bake-saddle-textures.ps1 divides each zone by its own brightest tone so that a
# later multiply by that tone reproduces vanilla. Same here, except the divisor
# is a grey and is measured, not chosen:
#
#   sheet  brightest opaque texel 187 (0xBB, neutral)  ->  x 255/187 = 1.36364
#   icon   brightest opaque texel 217 (0xD9, neutral)  ->  x 255/217 = 1.17512
#
# Two different factors on purpose. The icon's palette is genuinely brighter
# than the sheet's, exactly as the saddle's icon needed its own divisor for the
# same reason. Normalising each to its own peak is what makes both reach white.
#
# A CONSEQUENCE WORTH KNOWING BEFORE PICKING color_when_undyed: the sheet peaks
# at 0xBB, so reproducing vanilla's iron horse armour needs an undyed colour of
# 0xBBBBBB - NOT SaddleTint.IRON, which is 0x717171. That constant came off the
# saddle's brightest grey, and the saddle's hardware art is much darker than the
# armour's. Feeding 0x717171 to a plate armour renders it at 44% of vanilla's
# brightness. The two masks are normalised to different peaks and their undyed
# constants must be too.
#
# ---------------------------------------------------------------------------
# THE BLACK FLOOR - checked, because a zero texel is a one-way door
# ---------------------------------------------------------------------------
# A texel at zero multiplies to zero for every material, so it is black on gold,
# black on diamond, black on quartz. That is legitimate for a hard shadow and a
# disaster for anything else, so it gets counted rather than assumed. (It is not
# the same trap as SaddleTint.COAL, where a zero TINT makes the renderer skip
# the whole layer - that is about the colour, this is about the texture - but it
# is the same family of mistake and worth naming.)
#
# Measured: the darkest opaque texel is 5 on the sheet and 12 on the icon.
# NEITHER TEXTURE HAS A BLACK FLOOR. Normalising makes them brighter, not
# darker, so nothing can be driven to zero either. The script asserts that no
# output texel is zero and FAILS rather than silently flooring one, because a
# floor that fires quietly is how a future re-bake on different art would ship a
# mask that is subtly wrong everywhere.
#
# WINDOWS ONLY: uses System.Drawing, like both siblings, because the repo's Node
# tools have no PNG codec. If this ever needs to run elsewhere, port it - the
# rules above are the whole spec.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$jar = Join-Path $repo 'neoforge-26.1.2\build\moddev\artifacts\minecraft-patched-26.1.2.100-merged.jar'
$res = Join-Path $repo 'neoforge-26.1.2\src\main\resources\assets\horsegenetics\textures'

# Rec. 709 luminance. The same weights the eye uses, so a desaturated strap
# keeps the brightness it had rather than the brightness a naive (R+G+B)/3 would
# give it - on a (81,7,7) red those differ by a factor of two.
$LUM_R = 0.2126
$LUM_G = 0.7152
$LUM_B = 0.0722

# Anything at or under this counts as "near zero" in the report. Not a
# threshold the bake acts on - purely something to know about the art.
$NEAR_BLACK = 8

$JOBS = @(
  @{ name = 'sheet'
     src  = 'assets/minecraft/textures/entity/equipment/horse_body/iron.png'
     out  = (Join-Path $res 'entity\equipment\horse_body\plate.png') },
  @{ name = 'icon'
     src  = 'assets/minecraft/textures/item/iron_horse_armor.png'
     out  = (Join-Path $res 'item\plate_horse_armor.png') }
)

if (-not (Test-Path $jar)) {
  Write-Output "JAR MISSING: $jar"
  Write-Output 'Run a gradle task that resolves the vanilla artifacts first (:neoforge-26.1.2:build).'
  exit 1
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

# The sibling scripts' trick, one channel at a time: divide the colour out so a
# later multiply by that same colour puts it back. Here the divisor is a single
# grey, so this is called once per texel rather than three times.
function Split-Channel($value, $divisor) {
  return [int][Math]::Min(255, [Math]::Round(255.0 * $value / $divisor))
}

$anyFailed = $false
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
$report = @()

foreach ($job in $JOBS) {
  $src = Read-VanillaEntry $job.src
  $w = $src.Width; $h = $src.Height

  # --- pass one: measure. Nothing is decided by taste. -----------------------
  $opaque = 0; $neutral = 0; $tinted = 0
  $maxSpread = 0; $peakLum = -1; $peakAt = ''; $peakRgb = ''
  $srcMinLum = 999.0; $srcSumLum = 0.0
  $srcZero = 0; $srcNear = 0
  $sumR = 0.0; $sumG = 0.0; $sumB = 0.0
  $tintRamp = @{}
  $alphaValues = @{}

  for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
      $c = $src.GetPixel($x, $y)
      $a = [int]$c.A
      if (-not $alphaValues.ContainsKey($a)) { $alphaValues[$a] = 0 }
      $alphaValues[$a]++
      if ($a -eq 0) { continue }
      $opaque++

      $hi = [Math]::Max($c.R, [Math]::Max($c.G, $c.B))
      $lo = [Math]::Min($c.R, [Math]::Min($c.G, $c.B))
      $spread = $hi - $lo
      if ($spread -gt $maxSpread) { $maxSpread = $spread }
      if ($spread -eq 0) {
        $neutral++
      } else {
        $tinted++
        $k = '{0},{1},{2}' -f $c.R, $c.G, $c.B
        if (-not $tintRamp.ContainsKey($k)) { $tintRamp[$k] = 0 }
        $tintRamp[$k]++
      }

      $sumR += $c.R; $sumG += $c.G; $sumB += $c.B
      $lum = $LUM_R * $c.R + $LUM_G * $c.G + $LUM_B * $c.B
      $srcSumLum += $lum
      if ($lum -lt $srcMinLum) { $srcMinLum = $lum }
      if ($lum -gt $peakLum) {
        $peakLum = $lum
        $peakAt = "$x,$y"
        $peakRgb = '{0},{1},{2}' -f $c.R, $c.G, $c.B
      }
      if ($hi -eq 0) { $srcZero++ }
      if ($hi -le $NEAR_BLACK) { $srcNear++ }
    }
  }

  if ($opaque -eq 0) { throw ("no opaque texels at all in " + $job.src) }

  # The divisor. Measured, not chosen: the brightest opaque texel's luminance,
  # so that texel and no other lands exactly on white.
  # [int] deliberately: [Math]::Round returns a double, and a double has no 'X2'
  # format specifier - the summary below throws "Format specifier was invalid"
  # AFTER the PNGs are already written, which is the worst place to find out.
  $divisor = [int][Math]::Round($peakLum)
  if ($divisor -le 0) { throw ("brightest opaque texel is black in " + $job.src) }
  $scale = 255.0 / $divisor

  $meanR = $sumR / $opaque; $meanG = $sumG / $opaque; $meanB = $sumB / $opaque
  $meanColour = @{ R = [int][Math]::Round($meanR)
                   G = [int][Math]::Round($meanG)
                   B = [int][Math]::Round($meanB) }

  # --- pass two: write, and round-trip every texel as it goes. ---------------
  $dst = New-Object System.Drawing.Bitmap($w, $h)
  $outMin = 256; $outMax = -1; $outSum = 0.0
  $outZero = 0; $outNear = 0; $reachedWhite = 0
  $worstNeutral = 0; $worstTinted = 0
  $worstMean = 0; $worstIron = 0
  $IRON_SADDLE = 0x71   # SaddleTint.IRON's channel value, for the comparison below

  for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
      $c = $src.GetPixel($x, $y)
      # Alpha is carried verbatim and a transparent texel is never touched: it
      # is written back as the fully-clear colour it already was.
      if ($c.A -eq 0) { $dst.SetPixel($x, $y, $clear); continue }

      $lum = $LUM_R * $c.R + $LUM_G * $c.G + $LUM_B * $c.B
      $v = Split-Channel $lum $divisor

      if ($v -le 0) {
        throw ("texel $x,$y in " + $job.src + " normalises to pure black; a zero texel multiplies to black for every material")
      }
      $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $v, $v, $v))

      if ($v -lt $outMin) { $outMin = $v }
      if ($v -gt $outMax) { $outMax = $v }
      $outSum += $v
      if ($v -eq 0) { $outZero++ }
      if ($v -le $NEAR_BLACK) { $outNear++ }
      if ($v -eq 255) { $reachedWhite++ }

      # Round trip A, the one that matters: multiply the mask back by the very
      # colour it was divided by. This is the saddle script's $worst exactly -
      # "would an undyed piece match vanilla" - and for a neutral texel it must
      # come home within 1. For a red texel it cannot and must not: the red was
      # deliberately thrown away, and the error here is the size of that loss.
      $back = [int][Math]::Round($v * $divisor / 255.0)
      $errR = [Math]::Abs($back - $c.R)
      $errG = [Math]::Abs($back - $c.G)
      $errB = [Math]::Abs($back - $c.B)
      $err = [Math]::Max($errR, [Math]::Max($errG, $errB))
      if ($c.R -eq $c.G -and $c.G -eq $c.B) {
        if ($err -gt $worstNeutral) { $worstNeutral = $err }
      } else {
        if ($err -gt $worstTinted) { $worstTinted = $err }
      }

      # Round trip B: against the sheet's own MEAN opaque colour, per the brief.
      # Reported because it was asked for, not because it is an inverse - the
      # mean is far darker than the peak, so this number is dominated by that
      # gap and says nothing about the mask's fidelity. See the summary note.
      $bR = [int][Math]::Round($v * $meanColour.R / 255.0)
      $bG = [int][Math]::Round($v * $meanColour.G / 255.0)
      $bB = [int][Math]::Round($v * $meanColour.B / 255.0)
      $e2 = [Math]::Max([Math]::Abs($bR - $c.R), [Math]::Max([Math]::Abs($bG - $c.G), [Math]::Abs($bB - $c.B)))
      if ($e2 -gt $worstMean) { $worstMean = $e2 }

      # Round trip C: against SaddleTint.IRON (0x717171), to show in numbers why
      # that constant must not be reused for the plate armour.
      $b3 = [int][Math]::Round($v * $IRON_SADDLE / 255.0)
      $e3 = [Math]::Max([Math]::Abs($b3 - $c.R), [Math]::Max([Math]::Abs($b3 - $c.G), [Math]::Abs($b3 - $c.B)))
      if ($e3 -gt $worstIron) { $worstIron = $e3 }
    }
  }

  $outDir = Split-Path -Parent $job.out
  New-Item -ItemType Directory -Force -Path $outDir | Out-Null
  $dst.Save($job.out, [System.Drawing.Imaging.ImageFormat]::Png)
  $dst.Dispose()

  # --- pass three: read the file back off disk and prove the alpha survived. --
  # Not the in-memory bitmap - the saved PNG, because the encoder is the thing
  # most likely to quietly drop or premultiply a channel.
  $check = New-Object System.Drawing.Bitmap($job.out)
  $alphaBad = 0; $greyBad = 0
  if ($check.Width -ne $w -or $check.Height -ne $h) {
    throw ("saved " + $job.out + " is " + $check.Width + "x" + $check.Height + ", expected ${w}x${h}")
  }
  for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
      $o = $src.GetPixel($x, $y)
      $n = $check.GetPixel($x, $y)
      if ($o.A -ne $n.A) { $alphaBad++ }
      if ($n.A -ne 0 -and -not ($n.R -eq $n.G -and $n.G -eq $n.B)) { $greyBad++ }
    }
  }
  $check.Dispose()
  $src.Dispose()

  $ok = $true
  if ($worstNeutral -gt 1) { $ok = $false }
  if ($outZero -ne 0)      { $ok = $false }
  if ($outMax -ne 255)     { $ok = $false }
  if ($alphaBad -ne 0)     { $ok = $false }
  if ($greyBad -ne 0)      { $ok = $false }
  if (-not $ok) { $anyFailed = $true }

  $report += @{
    name = $job.name; src = $job.src; out = $job.out
    w = $w; h = $h; opaque = $opaque; neutral = $neutral; tinted = $tinted
    maxSpread = $maxSpread; ramp = $tintRamp
    peakLum = $peakLum; peakAt = $peakAt; peakRgb = $peakRgb
    divisor = $divisor; scale = $scale
    srcMin = $srcMinLum; srcMean = ($srcSumLum / $opaque)
    srcZero = $srcZero; srcNear = $srcNear
    mean = $meanColour
    outMin = $outMin; outMax = $outMax; outMean = ($outSum / $opaque)
    outZero = $outZero; outNear = $outNear; white = $reachedWhite
    worstNeutral = $worstNeutral; worstTinted = $worstTinted
    worstMean = $worstMean; worstIron = $worstIron
    alphaBad = $alphaBad; greyBad = $greyBad; alphas = $alphaValues
    ok = $ok
  }
}

# --- the summary -------------------------------------------------------------

function Signed($v) {
  # Little-endian, so the bytes go B, G, R, A and come back as 0xAARRGGBB. Taken
  # verbatim from bake-leather-armor-textures.ps1, and for the same reason: the
  # arithmetic form throws, because PowerShell widens 0xFF000000 to a long.
  return [BitConverter]::ToInt32([byte[]]@($v, $v, $v, 0xFF), 0)
}

foreach ($r in $report) {
  Write-Output ''
  Write-Output ('=== {0}  {1}x{2}' -f $r.name, $r.w, $r.h)
  Write-Output ('    from  {0}' -f $r.src)
  Write-Output ('    to    {0}' -f $r.out)
  Write-Output ('  opaque {0}   transparent {1}   alpha values {2}' -f `
      $r.opaque, (($r.w * $r.h) - $r.opaque), (($r.alphas.GetEnumerator() | Sort-Object Name | ForEach-Object { '{0}x{1}' -f $_.Name, $_.Value }) -join ' '))
  Write-Output ('  neutral (R==G==B) {0}   NOT neutral {1}   worst channel spread {2}' -f `
      $r.neutral, $r.tinted, $r.maxSpread)
  if ($r.tinted -gt 0) {
    Write-Output ('    the non-neutral ramp, desaturated by luminance: {0}' -f `
        (($r.ramp.GetEnumerator() | Sort-Object { [int](($_.Name -split ',')[0]) } | ForEach-Object { '({0})x{1}' -f $_.Name, $_.Value }) -join ' '))
  }
  Write-Output ('  brightest opaque texel BEFORE: ({0}) at {1}, luminance {2:N2}' -f $r.peakRgb, $r.peakAt, $r.peakLum)
  Write-Output ('  scale factor 255/{0} = {1:N5}   ->  brightest AFTER: {2}' -f $r.divisor, $r.scale, $r.outMax)
  Write-Output ('  source luminance  min {0:N2}  mean {1:N2}  max {2:N2}' -f $r.srcMin, $r.srcMean, $r.peakLum)
  Write-Output ('  OUTPUT luminance  min {0}  mean {1:N2}  max {2}   ({3} texels at pure white)' -f `
      $r.outMin, $r.outMean, $r.outMax, $r.white)
  Write-Output ('  black floor: source 0-texels {0}, <={1} {2}  ->  output 0-texels {3}, <={1} {4}' -f `
      $r.srcZero, $NEAR_BLACK, $r.srcNear, $r.outZero, $r.outNear)
  Write-Output ('  round trip x 0x{0:X2}{0:X2}{0:X2} (the inverse): worst neutral {1}   worst on the red ramp {2}' -f `
      $r.divisor, $r.worstNeutral, $r.worstTinted)
  Write-Output ('  round trip x mean colour ({0},{1},{2}): worst {3}  - not an inverse, see header' -f `
      $r.mean.R, $r.mean.G, $r.mean.B, $r.worstMean)
  Write-Output ('  round trip x SaddleTint.IRON 0x717171: worst {0}  - too dark for this mask, do not reuse it' -f $r.worstIron)
  Write-Output ('  alpha mismatches vs source (re-read from disk) {0}   non-grey output texels {1}' -f $r.alphaBad, $r.greyBad)
  Write-Output ('  {0}' -f $(if ($r.ok) { 'ok' } else { 'FAILED' }))
  if (-not $r.ok) {
    if ($r.worstNeutral -gt 1) { Write-Output ('    -> worst neutral round-trip error {0}, must be <= 1' -f $r.worstNeutral) }
    if ($r.outZero -ne 0)      { Write-Output ('    -> {0} output texel(s) are pure black and will multiply to black for every material' -f $r.outZero) }
    if ($r.outMax -ne 255)     { Write-Output ('    -> brightest output texel is {0}, not 255; the mask is not normalised' -f $r.outMax) }
    if ($r.alphaBad -ne 0)     { Write-Output ('    -> {0} texel(s) changed alpha; the PNG encoder did not preserve it' -f $r.alphaBad) }
    if ($r.greyBad -ne 0)      { Write-Output ('    -> {0} output texel(s) are not grey; the mask would stain every material' -f $r.greyBad) }
  }
}

Write-Output ''
Write-Output 'color_when_undyed to reproduce VANILLA iron over these masks:'
foreach ($r in $report) {
  Write-Output ('  {0,-6} 0x{1:X2}{1:X2}{1:X2}  -> {2}' -f $r.name, $r.divisor, (Signed $r.divisor))
}
Write-Output '  (the sheet and the icon are normalised to different peaks, so these differ;'
Write-Output '   a real material colour replaces both, and its own brightness sets the look.)'

if ($anyFailed) {
  Write-Output ''
  Write-Output 'FAILED: these masks would not tint cleanly. Do not ship them.'
  exit 1
}
Write-Output ''
Write-Output 'Both plate masks baked: pure greyscale, peaked at white, no black floor, alpha untouched.'
exit 0
