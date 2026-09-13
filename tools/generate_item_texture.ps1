# Generates the 16x16 deployment item icons (Little Bird and Apache).
# Usage: powershell -File tools/generate_item_texture.ps1 -Kind apache -OutputPath <path>
param(
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [ValidateSet('littlebird', 'apache')][string]$Kind = 'littlebird'
)

Add-Type -AssemblyName System.Drawing
$size = 16
$bitmap = [System.Drawing.Bitmap]::new($size, $size)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

$olive = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 96, 108, 74))
$dark = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 58, 63, 58))
$glass = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 40, 62, 74))
$steel = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 150, 156, 150))

if ($Kind -eq 'littlebird') {
    # main rotor, tail boom, cabin, canopy, skids, tail rotor
    $graphics.FillRectangle($steel, 1, 3, 14, 1)
    $graphics.FillRectangle($olive, 3, 6, 9, 4)
    $graphics.FillRectangle($glass, 11, 6, 3, 3)
    $graphics.FillRectangle($olive, 2, 8, 2, 2)
    $graphics.FillRectangle($olive, 12, 7, 3, 1)
    $graphics.FillRectangle($steel, 15, 5, 1, 5)
    $graphics.FillRectangle($dark, 3, 10, 9, 1)
    $graphics.FillRectangle($dark, 4, 11, 1, 2)
    $graphics.FillRectangle($dark, 10, 11, 1, 2)
    $graphics.FillRectangle($dark, 2, 4, 1, 2)
    $graphics.FillRectangle($dark, 13, 4, 1, 2)
} else {
    # Apache: reads nose right like the Little Bird, but longer, with a tandem canopy,
    # a chin gun, stub wings with a rocket pod and a tail rotor on a long boom.
    $graphics.FillRectangle($steel, 0, 2, 16, 1)        # main rotor
    $graphics.FillRectangle($dark, 8, 3, 1, 3)          # mast
    $graphics.FillRectangle($olive, 4, 6, 8, 4)         # fuselage
    $graphics.FillRectangle($olive, 12, 6, 3, 3)        # long nose
    $graphics.FillRectangle($glass, 9, 4, 2, 2)         # gunner canopy
    $graphics.FillRectangle($glass, 11, 4, 2, 2)        # pilot canopy
    $graphics.FillRectangle($dark, 13, 9, 3, 1)         # chin gun
    $graphics.FillRectangle($dark, 6, 10, 4, 2)         # stub wing
    $graphics.FillRectangle($steel, 5, 11, 1, 2)        # rocket pod
    $graphics.FillRectangle($olive, 1, 6, 3, 2)         # tail boom
    $graphics.FillRectangle($olive, 1, 4, 1, 3)         # tail fin
    $graphics.FillRectangle($steel, 0, 4, 1, 4)         # tail rotor
    $graphics.FillRectangle($dark, 7, 12, 1, 2)         # wheels
    $graphics.FillRectangle($dark, 11, 12, 1, 2)
}

$olive.Dispose(); $dark.Dispose(); $glass.Dispose(); $steel.Dispose()
$graphics.Dispose()
$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output ("wrote {0} ({1} bytes)" -f $OutputPath, (Get-Item $OutputPath).Length)
