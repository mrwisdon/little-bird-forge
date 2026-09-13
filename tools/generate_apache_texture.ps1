# Generates the AH-64 entity texture: a 512x512 painted panel sheet.
#
# Dark grey-green gunship scheme, deliberately a different hue from the Little Bird's graphite
# so the two aircraft read apart at a glance.
# Bands (matching ApacheModel's texOffs calls):
#   y   0-111  olive airframe panels (cabin, wings, boom, engines), rivets and panel lines
#   y 112-207  smoke glazing with frame lines
#   y 208-303  dark metal (mast, hub, radar, pylons, rails, exhausts) + blade stripe x 440-458
#   y 304-399  rocket pod black with dark red tube caps at x 480-511
#   y 400-511  turret, barrels, wheels and the instrument strip at x 300-380
param([Parameter(Mandatory = $true)][string]$OutputPath)

Add-Type -AssemblyName System.Drawing

$size = 512
$bitmap = [System.Drawing.Bitmap]::new($size, $size)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

function Fill-Band([int]$x, [int]$y, [int]$w, [int]$h, [int]$r, [int]$g, [int]$b) {
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, $r, $g, $b))
    $graphics.FillRectangle($brush, $x, $y, $w, $h)
    $brush.Dispose()
}

# Base bands: olive airframe, smoke glazing, dark metal, pod black, gunmetal
Fill-Band 0 0 512 112 54 60 50
Fill-Band 0 112 512 96 24 30 30
Fill-Band 0 208 512 96 46 50 52
Fill-Band 0 304 512 96 22 24 22
Fill-Band 0 400 512 112 18 20 22

# Band A: panel lines, rivet rows and a subdued warning stripe block
$panel = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 40, 46, 38), 1.0)
for ($y = 8; $y -lt 112; $y += 20) { $graphics.DrawLine($panel, 0, $y, 511, $y) }
for ($x = 0; $x -lt 512; $x += 40) { $graphics.DrawLine($panel, $x, 0, $x, 111) }
$panel.Dispose()
for ($x = 400; $x -lt 512; $x += 16) {
    Fill-Band $x 0 8 40 92 84 34
    Fill-Band ($x + 8) 0 8 40 20 20 20
}
for ($x = 16; $x -lt 380; $x += 32) {
    for ($y = 16; $y -lt 104; $y += 32) { Fill-Band $x $y 2 2 84 90 76 }
}

# Band B: canopy frame ribs
$frame = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 70, 80, 84), 2.0)
for ($y = 112; $y -lt 208; $y += 24) { $graphics.DrawLine($frame, 0, $y, 511, $y) }
for ($x = 0; $x -lt 512; $x += 48) { $graphics.DrawLine($frame, $x, 112, $x, 207) }
$frame.Dispose()

# Band C: dark metal with a pale blade tip stripe
Fill-Band 440 208 18 96 196 196 190
$metal = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 60, 64, 66), 1.0)
for ($y = 208; $y -lt 304; $y += 16) { $graphics.DrawLine($metal, 0, $y, 439, $y) }
$metal.Dispose()

# Band D: pod ribs and dark red tube caps
for ($x = 0; $x -lt 480; $x += 24) { Fill-Band $x 304 4 96 30 32 30 }
Fill-Band 480 304 32 96 96 34 30

# Band E: turret and barrels, wheels, instrument strip
Fill-Band 0 400 280 112 30 32 34
Fill-Band 400 400 112 112 26 26 28
Fill-Band 404 404 104 16 60 60 62
Fill-Band 300 400 80 112 12 14 16
for ($y = 408; $y -lt 504; $y += 12) {
    Fill-Band 304 $y 72 6 40 150 190
}
Fill-Band 288 400 40 40 70 74 70

$graphics.Dispose()
$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output ("wrote " + $OutputPath)
