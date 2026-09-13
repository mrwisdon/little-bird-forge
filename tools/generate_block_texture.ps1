# Generates the 16x16 ammunition crate block texture.
param([Parameter(Mandatory = $true)][string]$OutputPath)
Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
function NewBrush([int]$r, [int]$gr, [int]$b) {
    return [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, $r, $gr, $b))
}
function FillBox($brush, [int]$x, [int]$y, [int]$w, [int]$h) { $g.FillRectangle($brush, $x, $y, $w, $h) }
$body = NewBrush 92 88 70
$edge = NewBrush 62 60 48
$strap = NewBrush 150 156 150
$shell = NewBrush 198 160 60
FillBox $body 0 0 16 16
FillBox $edge 0 0 16 1
FillBox $edge 0 15 16 1
FillBox $edge 0 0 1 16
FillBox $edge 15 0 1 16
FillBox $strap 3 0 2 16
FillBox $strap 11 0 2 16
FillBox $strap 0 7 16 2
FillBox $shell 6 3 4 3
FillBox $shell 7 6 2 1
$body.Dispose(); $edge.Dispose(); $strap.Dispose(); $shell.Dispose()
$g.Dispose()
$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output ("wrote {0} ({1} bytes)" -f (Split-Path $OutputPath -Leaf), (Get-Item $OutputPath).Length)
