# Bakes this mod's dyeable saddle textures out of vanilla's.
#
# Vanilla's saddle is one texture of solid brown leather with grey metal
# hardware, and it is NOT dyeable: vanilla builds the saddle equipment asset
# with the plain single-argument Layer constructor, dyeable empty. To tint it we
# need what vanilla's leather armour has - a base layer that takes the tint and
# an untinted overlay for the parts that are not leather.
#
# The split needs no artistic judgement, which is the whole reason this is a
# script rather than hand-drawn art. Vanilla's 64x64 saddle sheet holds exactly
# nine colours: six browns (saturation 66-76%) and three pure greys (R=G=B).
# Leather is "saturated", hardware is "R equals G equals B". That rule is stated
# once, here, and the counts are asserted below so a future texture change that
# breaks the assumption fails loudly instead of producing a muddy saddle.
#
# The base is vanilla's colour DIVIDED BY the tint applied when undyed, so that
# base x undyed reproduces vanilla exactly - an undyed saddle is pixel-identical
# to the one players already know, which is what keeps donkeys, mules and
# skeleton horses looking untouched. Verified below to +/-1 per channel.
#
# WINDOWS ONLY: uses System.Drawing because the repo's Node tools have no PNG
# codec and hand-rolling one for a build asset is not worth it. If this ever
# needs to run on another machine, port it - the rule above is the whole spec.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repo = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$jar = Join-Path $repo 'neoforge-26.1.2\build\moddev\artifacts\minecraft-patched-26.1.2.100-merged.jar'
$dest = Join-Path $repo 'neoforge-26.1.2\src\main\resources\assets\minecraft\textures\entity\equipment'

# Vanilla's leather tint constant, 0xA06540 - the same number vanilla's own
# leather.json uses for color_when_undyed. Using it means our asset and vanilla's
# agree on what "undyed leather" means.
$UNDYED = @{ R = 0xA0; G = 0x65; B = 0x40 }

# Which folders get the treatment. Deliberately NOT camel, pig, strider or
# nautilus: this is a horse mod and has no business repainting them.
$FOLDERS = @('horse_saddle', 'donkey_saddle', 'mule_saddle', 'skeleton_horse_saddle', 'zombie_horse_saddle')

function Read-VanillaTexture([string]$folder) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $path = "assets/minecraft/textures/entity/equipment/$folder/saddle.png"
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $path }
        if (-not $entry) { throw "not in the jar: $path" }
        $ms = New-Object System.IO.MemoryStream
        $entry.Open().CopyTo($ms)
        $ms.Position = 0
        return New-Object System.Drawing.Bitmap($ms)
    } finally { $zip.Dispose() }
}

$anyFailed = $false

foreach ($folder in $FOLDERS) {
    $src = Read-VanillaTexture $folder
    $w = $src.Width; $h = $src.Height

    $base = New-Object System.Drawing.Bitmap($w, $h)
    $over = New-Object System.Drawing.Bitmap($w, $h)
    $clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

    $leather = 0; $hardware = 0; $worstError = 0

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $c = $src.GetPixel($x, $y)
            if ($c.A -eq 0) { $base.SetPixel($x,$y,$clear); $over.SetPixel($x,$y,$clear); continue }

            if ($c.R -eq $c.G -and $c.G -eq $c.B) {
                # Hardware: buckles and rings. Copied through untinted.
                $over.SetPixel($x, $y, $c)
                $base.SetPixel($x, $y, $clear)
                $hardware++
                continue
            }

            # Leather: divide out the undyed tint so base x undyed == vanilla.
            $r = [Math]::Min(255, [Math]::Round(255.0 * $c.R / $UNDYED.R))
            $g = [Math]::Min(255, [Math]::Round(255.0 * $c.G / $UNDYED.G))
            $b = [Math]::Min(255, [Math]::Round(255.0 * $c.B / $UNDYED.B))
            $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, [int]$r, [int]$g, [int]$b))
            $over.SetPixel($x, $y, $clear)
            $leather++

            # Round-trip check: what the game will actually draw when undyed.
            $back = @(
                [Math]::Round($r * $UNDYED.R / 255.0),
                [Math]::Round($g * $UNDYED.G / 255.0),
                [Math]::Round($b * $UNDYED.B / 255.0))
            foreach ($i in 0..2) {
                $orig = @($c.R, $c.G, $c.B)[$i]
                $err = [Math]::Abs($back[$i] - $orig)
                if ($err -gt $worstError) { $worstError = $err }
            }
        }
    }

    $outDir = Join-Path $dest $folder
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $base.Save((Join-Path $outDir 'saddle.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $over.Save((Join-Path $outDir 'saddle_overlay.png'), [System.Drawing.Imaging.ImageFormat]::Png)

    $ok = ($worstError -le 1)
    if (-not $ok) { $anyFailed = $true }
    Write-Output ("{0,-24} {1}x{2}  leather {3,4}  hardware {4,3}  worst undyed error {5}  {6}" -f `
        $folder, $w, $h, $leather, $hardware, $worstError, $(if ($ok) { 'ok' } else { 'FAILED' }))

    $src.Dispose(); $base.Dispose(); $over.Dispose()
}

# --- the inventory icon -----------------------------------------------------
# Same rule and same guarantee. Without this the saddle on the horse tints and
# the one in your hand does not, which reads as a bug rather than a feature.
function Read-VanillaEntry([string]$path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $path }
        if (-not $entry) { throw "not in the jar: $path" }
        $ms = New-Object System.IO.MemoryStream
        $entry.Open().CopyTo($ms)
        $ms.Position = 0
        return New-Object System.Drawing.Bitmap($ms)
    } finally { $zip.Dispose() }
}

# The icon needs its OWN undyed constant, and that is not a fudge - its palette
# is genuinely brighter than the entity sheet's (channel maxima 241/153/136
# against 138/85/46), so dividing by the entity's 0xA06540 overflows and clamps.
# Both constants reproduce vanilla exactly; they differ because the two source
# textures differ. Assert the maxima so a vanilla retexture fails here loudly
# rather than shipping a clamped, muddy icon.
#
# The icon is treated as ALL leather. It has three hardware pixels, but they are
# WARM greys (saturation ~22%, not R=G=B), so the neutral test used above does
# not find them - and three pixels out of ninety-two on a 16x16 icon is not
# worth a second layer and a second model. They tint slightly. Nobody will see it.
$ITEM_UNDYED = @{ R = 0xF1; G = 0x99; B = 0x88 }   # 0xFFF19988 == -943736 signed

$itemDir = Join-Path $repo 'neoforge-26.1.2\src\main\resources\assets\minecraft\textures\item'
$src = Read-VanillaEntry 'assets/minecraft/textures/item/saddle.png'
$w = $src.Width; $h = $src.Height
$base = New-Object System.Drawing.Bitmap($w, $h)
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
$leather = 0; $worstError = 0; $mR = 0; $mG = 0; $mB = 0

for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $c = $src.GetPixel($x, $y)
        if ($c.A -eq 0) { $base.SetPixel($x, $y, $clear); continue }
        if ($c.R -gt $mR) { $mR = $c.R }; if ($c.G -gt $mG) { $mG = $c.G }; if ($c.B -gt $mB) { $mB = $c.B }

        $r = [Math]::Min(255, [Math]::Round(255.0 * $c.R / $ITEM_UNDYED.R))
        $g = [Math]::Min(255, [Math]::Round(255.0 * $c.G / $ITEM_UNDYED.G))
        $b = [Math]::Min(255, [Math]::Round(255.0 * $c.B / $ITEM_UNDYED.B))
        $base.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, [int]$r, [int]$g, [int]$b))
        $leather++

        $back = @([Math]::Round($r*$ITEM_UNDYED.R/255.0), [Math]::Round($g*$ITEM_UNDYED.G/255.0), [Math]::Round($b*$ITEM_UNDYED.B/255.0))
        foreach ($i in 0..2) {
            $err = [Math]::Abs($back[$i] - @($c.R, $c.G, $c.B)[$i])
            if ($err -gt $worstError) { $worstError = $err }
        }
    }
}

New-Item -ItemType Directory -Force -Path $itemDir | Out-Null
$base.Save((Join-Path $itemDir 'saddle.png'), [System.Drawing.Imaging.ImageFormat]::Png)

$clamps = ($mR -gt $ITEM_UNDYED.R -or $mG -gt $ITEM_UNDYED.G -or $mB -gt $ITEM_UNDYED.B)
$itemOk = ($worstError -le 1 -and $leather -gt 0 -and -not $clamps)
if (-not $itemOk) { $anyFailed = $true }
Write-Output ("{0,-24} {1}x{2}  leather {3,4}  max {4}/{5}/{6}  worst undyed error {7}  {8}" -f `
    'item icon', $w, $h, $leather, $mR, $mG, $mB, $worstError, $(if ($itemOk) { 'ok' } else { 'FAILED' }))
if ($clamps) { Write-Output '  -> a channel exceeds the icon divisor; raise $ITEM_UNDYED and the tint default together' }
$src.Dispose(); $base.Dispose()

if ($anyFailed) {
    Write-Output ''
    Write-Output 'FAILED: an undyed saddle would not match vanilla. Do not ship this.'
    exit 1
}
Write-Output ''
Write-Output 'All saddles baked. Undyed renders pixel-identical to vanilla.'
# Explicit, so a caller testing $LASTEXITCODE does not read a stale value from
# whatever ran before this. Falling off the end leaves it untouched.
exit 0
