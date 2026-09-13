# Synthesises the Little Bird weapon and rotor sounds as looping OGG Vorbis files.
#
# Everything is generated from ffmpeg's own generators (noise, tremolo) so the pack
# ships no third-party audio and can be regenerated at any time.
param([Parameter(Mandatory = $true)][string]$OutputDirectory)

$ffmpeg = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ffmpeg) { throw 'ffmpeg not found on PATH' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

# NOTE: the source parameter must not be called $input - that name is reserved by
# PowerShell and silently binds to nothing, which made ffmpeg treat -af as a file.
function Render([string]$name, [string]$filters, [string]$source) {
    $out = Join-Path $OutputDirectory $name
    & $ffmpeg -y -hide_banner -loglevel error -f lavfi -i $source -af $filters -c:a libvorbis -q:a 4 $out
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $name" }
    Write-Output ("{0}  {1} bytes" -f $name, (Get-Item $out).Length)
}

# Rotor: low rumble with an 18 Hz blade thump, two seconds for a clean loop.
Render 'engine.ogg' 'lowpass=f=900,highpass=f=40,tremolo=f=18:d=0.85,volume=2.2,afade=t=in:d=0.05,afade=t=out:st=1.95:d=0.05' 'anoisesrc=d=2:c=pink:a=0.6'

# Gatling: pink noise chopped at 30 Hz so it reads as a chain of shots.
Render 'gun.ogg' 'lowpass=f=4200,highpass=f=150,tremolo=f=30:d=0.8,volume=2.6,afade=t=in:d=0.01,afade=t=out:st=0.97:d=0.03' 'anoisesrc=d=1:c=pink:a=0.7'

# Single round transient, laid over the loop for mechanical texture.
Render 'gun_shot.ogg' 'lowpass=f=5200,highpass=f=220,volume=3.0,afade=t=out:st=0.02:d=0.1' 'anoisesrc=d=0.12:c=white:a=0.9'

# Rocket launch: noise burst with the top end left open.
Render 'rocket_launch.ogg' 'highpass=f=200,lowpass=f=6000,volume=2.4,afade=t=out:st=0.35:d=0.45' 'anoisesrc=d=0.8:c=pink:a=0.85'

# Warhead: low rumble with a long tail.
Render 'explosion.ogg' 'lowpass=f=700,volume=3.2,afade=t=out:st=0.25:d=1.25' 'anoisesrc=d=1.5:c=brown:a=0.95'
