# Synthesises the PHL-191 launch / flight / impact / HUD sounds as OGG Vorbis files.
#
# Everything is generated from noise and sine waves with numpy, so the pack ships no
# third-party audio and the sounds can be regenerated at any time. Windows ffmpeg
# (libvorbis) does the encoding.
import os, sys, wave
import numpy as np

SR = 44100
OUT = os.path.dirname(os.path.abspath(__file__))
WAV = os.path.join(OUT, "wav")
os.makedirs(WAV, exist_ok=True)
rng = np.random.default_rng(191)

def t(dur):
    return np.linspace(0.0, dur, int(SR * dur), endpoint=False)

def envelope(n, attack, decay, curve=3.0):
    a = np.minimum(1.0, np.linspace(0, 1, n) / max(attack, 1e-6))
    d = np.exp(-curve * np.linspace(0, 1, n))
    return a * d

def brown(n):
    x = rng.normal(0, 1, n)
    return np.cumsum(x) / np.sqrt(np.arange(1, n + 1))

def lowpass(x, cutoff):
    # one pole low pass
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = a * acc + (1 - a) * x[i]
        y[i] = acc
    return y

def highpass(x, cutoff):
    return x - lowpass(x, cutoff)

def normalize(x, peak=0.92):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x

def save(name, x):
    x = normalize(x)
    pcm = (x * 32767).astype("<i2")
    with wave.open(os.path.join(WAV, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    print("wrote", name, len(x) / SR, "s")

# ---- launch: ignition crack, roar, low thump ---------------------------------
dur = 2.6
n = int(SR * dur)
x = np.zeros(n)
tt = np.arange(n) / SR
crack = highpass(rng.normal(0, 1, n), 1500) * envelope(n, 0.005, 3.5) * 0.9
roar = lowpass(brown(n), 420) * envelope(n, 0.03, 2.2, 2.6)
roar *= 0.6 + 0.4 * np.sin(2 * np.pi * 23 * tt)
thump = np.sin(2 * np.pi * (70 - 40 * tt / dur) * tt) * np.exp(-6 * tt) * 0.9
hiss = highpass(rng.normal(0, 1, n), 2500) * envelope(n, 0.1, 1.6) * 0.22
x = crack * 0.8 + roar * 1.0 + thump + hiss
save("phl_launch", x)

# ---- flight: motor roar, loopable -------------------------------------------
dur = 1.0
n = int(SR * dur)
tt = np.arange(n) / SR
fly = lowpass(brown(n), 900) * 0.9
fly += highpass(rng.normal(0, 1, n), 3000) * 0.18
fly *= 0.85 + 0.15 * np.sin(2 * np.pi * 31 * tt)
fade = np.minimum(np.minimum(tt / 0.05, (dur - tt) / 0.05), 1.0)
save("phl_fly", fly * fade)

# ---- impact: sub thump, boom, crack, debris, rumble tail --------------------
dur = 3.2
n = int(SR * dur)
tt = np.arange(n) / SR
x = np.zeros(n)
sub = np.sin(2 * np.pi * (80 - 55 * np.clip(tt / 0.9, 0, 1)) * tt) * np.exp(-3.2 * tt) * 1.1
boom = lowpass(brown(n), 260) * np.exp(-2.4 * tt) * 1.0
crack = highpass(rng.normal(0, 1, n), 1200) * envelope(n, 0.004, 4.0) * 0.75
debris = np.zeros(n)
for _ in range(180):
    at = int(rng.uniform(0.05, 1.6) * SR)
    ln = int(rng.uniform(0.002, 0.012) * SR)
    debris[at:at + ln] += rng.normal(0, 1, ln) * rng.uniform(0.1, 0.5)
debris = lowpass(debris, 5000) * 0.6
rumble = lowpass(brown(n), 120) * np.exp(-1.1 * tt) * 0.9
x = sub + boom + crack + debris + rumble
save("phl_impact", x)

# ---- QTE tick / confirm ------------------------------------------------------
dur = 0.09
n = int(SR * dur)
tt = np.arange(n) / SR
tick = np.sin(2 * np.pi * 1850 * tt) * np.exp(-45 * tt)
save("qte_tick", tick * 0.9)

dur = 0.3
n = int(SR * dur)
tt = np.arange(n) / SR
hit = np.sin(2 * np.pi * 880 * tt) * np.exp(-8 * tt)
hit += np.sin(2 * np.pi * 1320 * tt) * np.exp(-6 * tt) * 0.7
save("qte_hit", hit)

# ---- reload clank ------------------------------------------------------------
dur = 0.35
n = int(SR * dur)
tt = np.arange(n) / SR
clank = np.zeros(n)
for freq, amp in ((260, 1.0), (430, 0.7), (880, 0.45)):
    clank += np.sin(2 * np.pi * freq * tt) * np.exp(-18 * tt) * amp
clank += highpass(rng.normal(0, 1, n), 800) * np.exp(-60 * tt) * 0.8
save("reload_clank", clank)

# ---- radar ping --------------------------------------------------------------
dur = 0.5
n = int(SR * dur)
tt = np.arange(n) / SR
ping = np.sin(2 * np.pi * 1250 * tt) * np.exp(-9 * tt)
ping += np.sin(2 * np.pi * 2500 * tt) * np.exp(-14 * tt) * 0.25
save("radar_ping", ping)
print("done")
