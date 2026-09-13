# Generates the Little Bird entity texture: a 512x512 painted panel sheet.
#
# Dark gunship scheme requested for the Little Bird, so every band is graphite
# black with only enough contrast to read panels, frames and service markings.
# Bands:
#   y   0-111  graphite airframe panels, rivets, subdued hazard stripes at x 400-511
#   y 112-207  smoke-tinted glazing with frame lines
#   y 208-303  dark metal (hub, mast, tail rotor, blur bars) + blade tip stripe at x 440-458
#   y 304-399  rocket pod black, dark red nose caps at x 480-511
#   y 400-511  guns, skids and dashboard, instrument strip at x 300-380
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

# Base bands: graphite airframe, smoke glazing, dark metal, black pods, gunmetal
Fill-Band 0 0 512 112 30 33 36
Fill-Band 0 112 512 96 22 28 32
Fill-Band 0 208 512 96 42 45 48
Fill-Band 0 304 512 96 20 22 24
Fill-Band 0 400 512 112 16 18 20

# Band A: subdued hazard stripes on the fin, panel lines and rivet rows
for ($x = 400; $x -lt 512; $x += 16) {
    Fill-Band $x 0 8 64 74 68 26
    Fill-Band ($x + 8) 0 8 64 14 14 14
}
$panel = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 46, 50, 54), 1.0)
for ($y = 8; $y -lt 112; $y += 24) { $graphics.DrawLine($panel, 0, $y, 399, $y) }
for ($x = 0; $x -lt 400; $x += 48) { $graphics.DrawLine($panel, $x, 0, $x, 111) }
$panel.Dispose()

# Band B: canopy frame
$frame = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 64, 74, 82), 2.0)
for ($y = 112; $y -lt 208; $y += 32) { $graphics.DrawLine($frame, 0, $y, 511, $y) }
for ($x = 0; $x -lt 512; $x += 64) { $graphics.DrawLine($frame, $x, 112, $x, 207) }
$frame.Dispose()

# Band C: faint blade tip marking
Fill-Band 440 208 18 96 104 110 114

# Band D: dark red nose caps for the rocket pods
Fill-Band 480 304 32 96 104 30 26

# Band E: instrument strip used by the cockpit dashboard
Fill-Band 300 400 80 112 132 138 142
Fill-Band 300 430 80 6 40 44 48
Fill-Band 300 460 80 6 40 44 48

$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$bitmap.Dispose()
Write-Output ("wrote {0} ({1} bytes)" -f $OutputPath, (Get-Item $OutputPath).Length)
