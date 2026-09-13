# Generates the 16x16 icons for the survival crafting chain.
# Usage: powershell -File tools/generate_component_textures.ps1 -Kind <kind> -OutputPath <path>
# Note: the helpers are deliberately not called R or C - PowerShell resolves a one letter alias
# (r = Invoke-History) before a function of the same name.
param(
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [Parameter(Mandatory = $true)]
    [ValidateSet('airframe_section', 'rotor_assembly', 'aircraft_engine', 'avionics_unit',
                 'canopy_glass', 'weapon_pylon', 'targeting_computer',
                 'apache_airframe', 'apache_weapons')]
    [string]$Kind
)

Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

function NewBrush([int]$r, [int]$gr, [int]$b) {
    return [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, $r, $gr, $b))
}
function FillBox($brush, [int]$x, [int]$y, [int]$w, [int]$h) {
    $g.FillRectangle($brush, $x, $y, $w, $h)
}

$steel = NewBrush 150 156 150
$dark = NewBrush 86 92 88
$darkOlive = NewBrush 58 63 58
$glass = NewBrush 40 62 74
$board = NewBrush 32 74 52
$gold = NewBrush 198 160 60
$red = NewBrush 170 60 52
$cyan = NewBrush 90 220 210
$grey = NewBrush 120 126 120

switch ($Kind) {
    'airframe_section' {
        FillBox $steel 2 2 12 2
        FillBox $steel 2 12 12 2
        FillBox $steel 2 4 2 8
        FillBox $steel 12 4 2 8
        FillBox $dark 7 4 2 8
    }
    'rotor_assembly' {
        FillBox $dark 6 6 4 4
        FillBox $steel 7 1 2 5
        FillBox $steel 7 10 2 5
        FillBox $steel 1 7 5 2
        FillBox $steel 10 7 5 2
        FillBox $gold 7 7 2 2
    }
    'aircraft_engine' {
        FillBox $steel 3 5 10 6
        FillBox $dark 2 6 2 4
        FillBox $dark 12 6 2 4
        FillBox $gold 6 5 2 6
        FillBox $dark 7 3 2 2
    }
    'avionics_unit' {
        FillBox $board 2 3 12 10
        FillBox $gold 4 5 8 1
        FillBox $gold 4 8 8 1
        FillBox $gold 4 11 6 1
        FillBox $dark 5 6 3 2
        FillBox $dark 9 9 3 2
        FillBox $red 11 11 2 2
    }
    'canopy_glass' {
        FillBox $glass 4 4 8 7
        FillBox $cyan 5 5 3 2
        FillBox $dark 3 11 10 1
        FillBox $steel 5 12 6 1
    }
    'weapon_pylon' {
        FillBox $dark 4 3 8 2
        FillBox $steel 7 5 2 4
        FillBox $darkOlive 5 9 6 4
        FillBox $dark 4 12 2 2
        FillBox $dark 10 12 2 2
    }
    'targeting_computer' {
        FillBox $gold 2 3 12 10
        FillBox $dark 3 4 10 8
        FillBox $cyan 8 5 1 6
        FillBox $cyan 5 8 6 1
        FillBox $red 7 7 2 2
    }
    'apache_airframe' {
        FillBox $grey 1 3 14 1
        FillBox $dark 8 4 1 3
        FillBox $grey 4 7 8 4
        FillBox $grey 12 8 3 2
        FillBox $grey 1 7 3 2
        FillBox $grey 1 5 1 3
        FillBox $steel 0 5 1 3
        FillBox $dark 6 11 1 2
        FillBox $dark 10 11 1 2
    }
    'apache_weapons' {
        FillBox $dark 2 6 6 5
        FillBox $steel 3 7 2 2
        FillBox $steel 5 7 2 2
        FillBox $steel 3 10 2 2
        FillBox $steel 5 10 2 2
        FillBox $dark 8 6 3 4
        FillBox $steel 10 7 5 2
        FillBox $gold 14 7 2 2
    }
}

$steel.Dispose(); $dark.Dispose(); $darkOlive.Dispose(); $glass.Dispose(); $board.Dispose()
$gold.Dispose(); $red.Dispose(); $cyan.Dispose(); $grey.Dispose()
$g.Dispose()
$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output ("wrote {0} ({1} bytes)" -f (Split-Path $OutputPath -Leaf), (Get-Item $OutputPath).Length)
