# Bakes this mod's three-zone leather horse armour out of vanilla's.
#
# THIS IS NOT THE SADDLE'S PROBLEM, and the difference is the whole reason this
# is a separate script. Vanilla's saddle was ONE texture with the leather colour
# baked in, so that script had to divide the colour out of everything. Vanilla's
# leather horse armour is already two textures and already half-dyeable:
#
#   leather.png          64x64, 624 opaque texels, ALL of them greyscale (four
#                        flat tones). It is a pure tint mask - that is why
#                        vanilla's dyed leather armour looks as good as it does.
#                        WE DO NOT TOUCH IT. See the warning below.
#   leather_overlay.png  64x64, 588 opaque texels. The fixed trim vanilla never
#                        tints: brown straps and saddlebags, plus metal bars.
#
# So the base needs no work at all, and all the content here is splitting the
# OVERLAY into the parts that should take a colour and the parts that must not.
#
# ---------------------------------------------------------------------------
# WARNING: leather.png AND equipment/leather.json ARE SHARED WITH PLAYER ARMOUR
# ---------------------------------------------------------------------------
# There is no leather_horse_armor.json. The asset is equipment/leather.json, and
# that ONE file carries horse_body, humanoid, humanoid_baby and humanoid_leggings
# together - leather horse armour and every leather chestplate in the game read
# the same file and the same leather.png. Our override must therefore rewrite the
# horse_body entry ONLY and copy the other three verbatim, exactly as our
# saddle.json leaves camels, pigs, striders and nautiluses alone. Changing
# leather.png would repaint the player's armour too, so this script never writes
# it and only ever reads vanilla's.
#
# ---------------------------------------------------------------------------
# THE THREE-WAY SPLIT, and why the obvious rule is wrong here
# ---------------------------------------------------------------------------
# The saddle found metal by colour: "R == G == B". That worked because vanilla's
# saddle sheet holds exactly three pure greys and all three are buckles. The
# overlay is NOT like that - it holds twenty-odd greys, and a plain greyscale
# test claims 171 texels of which most are not hardware.
#
# Measured on 2026-09-16 with a false-colour map rather than by argument, which
# refuted two guesses in a row:
#
#   Guess 1: the dark greys are the OUTLINES around the leather shapes.
#            WRONG. The bag outlines came back coloured - they are dark brown,
#            not grey, and were never in the grey set at all.
#   Guess 2: hardware is grey brighter than 0x3E.
#            WRONG. At 0x3E the metal bars render as dark bodies with a few
#            bright speckles: the threshold cuts each bar in half rather than
#            separating hardware from anything.
#
# The truth is that a bar is dark grey body PLUS bright grey highlight, and the
# real boundary sits lower. Thresholds 0x20 and 0x2D give an identical answer -
# a plateau, so the number is not balanced on a knife edge - and at 0x20 every
# bar is solid and the remaining dark greys are the shading inside the bags and
# the bands across the central box. Hence:
#
#   FITTINGS  79 texels   grey, >= 0x20            dyeable, undyed 0x9C9C9C
#   TRIM     417 texels   coloured (the browns)    dyeable, undyed 0x95652B
#   SHADOW    92 texels   grey, < 0x20             NOT dyeable, copied verbatim
#
# Shadow stays untinted on purpose. It is the darkest part of the artwork, it
# reads as depth rather than as material, and tinting it is exactly how dyed
# tack starts looking like flat stickers.
#
# Each dyeable zone's base is vanilla's colour DIVIDED BY its undyed constant,
# so base x undyed reproduces vanilla and an undyed leather horse armour is the
# one players already know. Both constants are the per-channel MAXIMUM of their
# zone, which is what gives the dyes headroom - the lesson from the saddle on
# 2026-09-16, where dividing by vanilla's leather brown capped blue at 183 and
# made every blue and purple dye come out muddy. Verified before writing: zero
# channels clamp and the worst round-trip error is 1.
#
# WINDOWS ONLY: uses System.Drawing, like its sibling. If this ever needs to run
# elsewhere, port it - the rules above are the whole spec.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$jar = Join-Path $repo 'neoforge-26.1.2\build\moddev\artifacts\minecraft-patched-26.1.2.100-merged.jar'
$dest = Join-Path $repo 'neoforge-26.1.2\src\main\resources\assets\minecraft\textures\entity\equipment\horse_body'
$SRC = 'assets/minecraft/textures/entity/equipment/horse_body/leather_overlay.png'

$HARDWARE_FLOOR = 0x20
$UNDYED_FITTINGS = @{ R = 0x9C; G = 0x9C; B = 0x9C }
$UNDYED_TRIM     = @{ R = 0x95; G = 0x65; B = 0x2B }

if (-not (Test-Path $jar)) { Write-Output "JAR MISSING: $jar"; exit 1 }
New-Item -ItemType Directory -Force -Path $dest | Out-Null

$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$entry = @($zip.Entries | Where-Object { $_.FullName -eq $SRC })
if ($entry.Count -eq 0) { Write-Output "OVERLAY MISSING FROM JAR: $SRC"; $zip.Dispose(); exit 1 }
$tmp = Join-Path $env:TEMP 'phc_leather_overlay.png'
[System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry[0], $tmp, $true)
$zip.Dispose()

$src = New-Object System.Drawing.Bitmap($tmp)
$w = $src.Width; $h = $src.Height
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

$trim = New-Object System.Drawing.Bitmap($w, $h)
$fit  = New-Object System.Drawing.Bitmap($w, $h)
$shad = New-Object System.Drawing.Bitmap($w, $h)

$nTrim = 0; $nFit = 0; $nShad = 0; $nOpaque = 0
$worst = 0; $clamped = 0

for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $p = $src.GetPixel($x, $y)
        $trim.SetPixel($x, $y, $clear)
        $fit.SetPixel($x, $y, $clear)
        $shad.SetPixel($x, $y, $clear)
        if ($p.A -eq 0) { continue }
        $nOpaque++

        $isGrey = ($p.R -eq $p.G -and $p.G -eq $p.B)

        if ($isGrey -and $p.R -lt $HARDWARE_FLOOR) {
            # Shadow: copied byte for byte, never tinted.
            $shad.SetPixel($x, $y, $p)
            $nShad++
            continue
        }

        $undyed = if ($isGrey) { $UNDYED_FITTINGS } else { $UNDYED_TRIM }
        $br = [int][math]::Round(255.0 * $p.R / $undyed.R)
        $bg = [int][math]::Round(255.0 * $p.G / $undyed.G)
        $bb = [int][math]::Round(255.0 * $p.B / $undyed.B)
        foreach ($c in @($br, $bg, $bb)) { if ($c -gt 255) { $clamped++ } }
        $br = [math]::Min(255, $br); $bg = [math]::Min(255, $bg); $bb = [math]::Min(255, $bb)

        # What the renderer will actually show for an undyed piece.
        $back = @(
            [math]::Floor($br * $undyed.R / 255.0),
            [math]::Floor($bg * $undyed.G / 255.0),
            [math]::Floor($bb * $undyed.B / 255.0))
        $err = @([math]::Abs($back[0] - $p.R), [math]::Abs($back[1] - $p.G), [math]::Abs($back[2] - $p.B))
        foreach ($e in $err) { if ($e -gt $worst) { $worst = $e } }

        $baked = [System.Drawing.Color]::FromArgb($p.A, $br, $bg, $bb)
        if ($isGrey) { $fit.SetPixel($x, $y, $baked); $nFit++ }
        else         { $trim.SetPixel($x, $y, $baked); $nTrim++ }
    }
}

$trim.Save((Join-Path $dest 'leather_trim.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$fit.Save((Join-Path $dest 'leather_fittings.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$shad.Save((Join-Path $dest 'leather_shadow.png'), [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output ('trim      ' + $nTrim + ' texels  -> leather_trim.png')
Write-Output ('fittings  ' + $nFit + ' texels  -> leather_fittings.png')
Write-Output ('shadow    ' + $nShad + ' texels  -> leather_shadow.png')
Write-Output ('total     ' + ($nTrim + $nFit + $nShad) + ' of ' + $nOpaque + ' opaque')
Write-Output ''

$ok = $true
if (($nTrim + $nFit + $nShad) -ne $nOpaque) {
    Write-Output ('FAIL: the three zones do not partition the overlay - ' + ($nOpaque - $nTrim - $nFit - $nShad) + ' orphaned')
    $ok = $false
}
if ($clamped -ne 0) { Write-Output ("FAIL: $clamped channels clamped - an undyed piece will not match vanilla"); $ok = $false }
if ($worst -gt 1)   { Write-Output ("FAIL: worst undyed error $worst, must be <= 1"); $ok = $false }

# The equipment asset wants SIGNED ARGB, which is the single easiest thing to
# get wrong by hand, so it is printed rather than worked out.
#
# Via BitConverter rather than arithmetic, and that is not fussiness. The obvious
# "(0xFF000000 -bor ...) - 0x100000000" throws: PowerShell widens 0xFF000000 to a
# LONG, so the expression never wraps into Int32 the way the same line would in
# C or Java, and the cast fails with "value was either too large or too small".
# Reinterpreting the four bytes cannot drift, whatever the literal's width.
function Signed($c) {
    # Little-endian, so the bytes go B, G, R, A and come back as 0xAARRGGBB.
    # Two attempts at this by arithmetic both threw, for opposite reasons -
    # 0xFF000000 is neither a long NOR a uint here, it is Int32 -16777216 - so
    # there is no literal in this version at all.
    return [BitConverter]::ToInt32([byte[]]@($c.B, $c.G, $c.R, 0xFF), 0)
}
Write-Output ''
Write-Output 'color_when_undyed for assets/minecraft/equipment/leather.json (horse_body ONLY):'
Write-Output ('  leather_trim       0x{0:X2}{1:X2}{2:X2}  -> {3}' -f $UNDYED_TRIM.R, $UNDYED_TRIM.G, $UNDYED_TRIM.B, (Signed $UNDYED_TRIM))
Write-Output ('  leather_fittings   0x{0:X2}{1:X2}{2:X2}  -> {3}' -f $UNDYED_FITTINGS.R, $UNDYED_FITTINGS.G, $UNDYED_FITTINGS.B, (Signed $UNDYED_FITTINGS))
Write-Output '  leather (base)     unchanged, vanilla -6265536'

$src.Dispose(); $trim.Dispose(); $fit.Dispose(); $shad.Dispose()

if (-not $ok) { Write-Output ''; Write-Output 'BAKE FAILED'; exit 1 }
Write-Output ''
Write-Output ('worst undyed error ' + $worst + ', no clamping, zero orphans - bake OK')
exit 0
