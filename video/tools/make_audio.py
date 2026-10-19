"""Original sound design for the Lucerra film. Pure synthesis (numpy/scipy): no third-party samples,
so the result can be distributed with the video.

Writes public/audio/bed.wav (ambience + music) and public/audio/fx.wav (hits, whooshes, UI ticks).
Replace either file with your own 42.9 s, 44.1 kHz stereo WAV to change the sound.
"""
import os
import wave

import numpy as np
from scipy.signal import butter, fftconvolve, lfilter

SR = 44100
DUR = 42.87
N = int(SR * DUR)
rs = np.random.RandomState(11)
OUT = os.path.join(os.path.dirname(__file__), '..', 'public', 'audio')


def t_(n):
    return np.arange(n) / SR


def lp(x, f, order=2):
    b, a = butter(order, f / (SR / 2), 'low')
    return lfilter(b, a, x)


def hp(x, f, order=2):
    b, a = butter(order, f / (SR / 2), 'high')
    return lfilter(b, a, x)


def bp(x, lo, hi):
    b, a = butter(2, [lo / (SR / 2), hi / (SR / 2)], 'band')
    return lfilter(b, a, x)


def place(buf, sig, at, gain=1.0, pan=0.0):
    """Mix mono `sig` into stereo `buf` starting at `at` seconds."""
    i = int(at * SR)
    if i >= buf.shape[0]:
        return
    sig = sig[: buf.shape[0] - i]
    l = gain * np.cos((pan + 1) * np.pi / 4)
    r = gain * np.sin((pan + 1) * np.pi / 4)
    buf[i : i + len(sig), 0] += sig * l
    buf[i : i + len(sig), 1] += sig * r


def env_ar(n, a, r, curve=2.0):
    e = np.ones(n)
    na = max(1, int(a * SR))
    nr = max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na) ** curve
    e[-nr:] = np.minimum(e[-nr:], np.linspace(1, 0, nr) ** curve)
    return e


def saw(f, n):
    ph = (f * t_(n)) % 1.0
    return 2 * ph - 1


def pad(freqs, dur, a=2.5, r=2.5, cutoff=900):
    n = int(dur * SR)
    t = t_(n)
    sig = np.zeros(n)
    for f in freqs:
        for det in (-0.12, 0.0, 0.12):
            ff = f * (1 + det * 0.01)
            sig += 0.6 * np.sin(2 * np.pi * ff * t) + 0.25 * saw(ff, n) * 0.5
    sig = lp(sig, cutoff)
    return sig / len(freqs) * env_ar(n, a, r)


def pluck(f, dur=1.6):
    n = int(dur * SR)
    t = t_(n)
    sig = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 7) + 0.12 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t * 11)
    return sig * np.exp(-t * 3.2) * np.minimum(1, t * 400)


def tick(f, dur=0.12):
    n = int(dur * SR)
    t = t_(n)
    body = np.sin(2 * np.pi * f * t) * np.exp(-t * 55)
    click = hp(rs.randn(n), 3000) * np.exp(-t * 400) * 0.25
    return body + click


def noise_sweep(dur, f0, f1, shape=2.0, rise=True):
    """Band-limited noise whose centre frequency glides f0 -> f1 and whose level swells (rise) or decays."""
    n = int(dur * SR)
    x = rs.randn(n)
    out = np.zeros(n)
    steps = 40
    seg = n // steps
    for k in range(steps):
        fc = f0 * (f1 / f0) ** (k / (steps - 1))
        a, b = k * seg, (k + 1) * seg if k < steps - 1 else n
        out[a:b] = bp(x, max(60, fc * 0.6), min(SR / 2 - 100, fc * 1.4))[a:b]
    u = np.linspace(0, 1, n)
    amp = (u**shape) if rise else ((1 - u) ** shape)
    amp *= np.minimum(1, u * 40) * np.minimum(1, (1 - u) * 40 + 0.0)
    return out * amp


def impact(dur=2.2, f0=70.0, f1=38.0):
    n = int(dur * SR)
    t = t_(n)
    f = f1 + (f0 - f1) * np.exp(-t * 6)
    ph = 2 * np.pi * np.cumsum(f) / SR
    body = np.sin(ph) * np.exp(-t * 2.1)
    air = lp(rs.randn(n), 500) * np.exp(-t * 7) * 0.5
    return body + air


def reverb(stereo, seconds=3.0, wet=0.35):
    n = int(seconds * SR)
    t = t_(n)
    out = np.zeros_like(stereo)
    for ch in range(2):
        ir = rs.randn(n) * np.exp(-t * 2.4)
        ir = lp(ir, 5500)
        wetsig = fftconvolve(stereo[:, ch], ir)[: stereo.shape[0]] * 0.06
        out[:, ch] = wet * wetsig
    return stereo * (1 - wet * 0.4) + out


def fade(x, a=0.02):
    n = int(a * SR)
    x = x.copy()
    ramp = np.linspace(0, 1, n)
    if x.ndim == 2:
        ramp = ramp[:, None]
    x[:n] *= ramp
    x[-n:] *= ramp[::-1]
    return x


SLOW = 1.4
def T4(t):
    """Old scene-4 time (18-28 s at original pace) -> stretched film time (scene starts 21.33 s)."""
    return 21.33 + (t - 18.0) * SLOW
END0 = 37.2  # end scene start


A = dict(A1=55.0, E2=82.4, A2=110.0, E3=164.8, B3=246.9, Cs4=277.2, A3=220.0, E4=329.6, A4=440.0, B4=493.9, Cs5=554.4, E5=659.3, A5=880.0)

# ---------------------------------------------------------------- bed
bed = np.zeros((N, 2))
bed_dry = np.zeros((N, 2))

# Scene 1: low drone that swells, then is cut dead at 6.0 s (the "freeze").
d = np.zeros(int(6.0 * SR))
t = t_(len(d))
d += lp(saw(A['A1'], len(d)), 220) * 0.5 + np.sin(2 * np.pi * A['E2'] * t) * 0.25 + np.sin(2 * np.pi * A['A1'] * t) * 0.4
d *= np.linspace(0.12, 1.0, len(d)) ** 2.2
n_cut = int(0.03 * SR)
d[-n_cut:] *= np.linspace(1, 0, n_cut)
d = np.concatenate([d, np.zeros(int(1.5 * SR))])
d[int(5.2 * SR):] *= np.linspace(1, 0, len(d) - int(5.2 * SR))   # decays under the pad: no gap
place(bed_dry, d, 0.0, 0.5)
# Uneasy upper layer: slow tremolo on a minor second that resolves away at the cut.
t = t_(int(5.0 * SR))
up = np.sin(2 * np.pi * 440 * t) * 0.5 + np.sin(2 * np.pi * 466.2 * t) * 0.5
up = lp(up, 1800) * (0.5 + 0.5 * np.sin(2 * np.pi * 5.5 * t)) * np.linspace(0, 1, len(t)) ** 3
up[-n_cut:] *= np.linspace(1, 0, n_cut)
place(bed_dry, up, 1.0, 0.06)

# 6.0-6.45 s: silence. Scene 2: a quiet pad grows.
place(bed_dry, pad([A['A1'], A['E2'], A['A2']], 6.5, a=1.2, r=2.0, cutoff=500), 5.0, 0.3)
# Scene 3: A major add9 swells in with the disc, peaks at the logo.
chord = [A['A2'], A['E3'], A['B3'], A['Cs4']]
place(bed_dry, pad(chord, 31.0, a=3.0, r=3.0, cutoff=1400), 10.5, 0.4)   # holds through the whole UI tour
place(bed_dry, np.sin(2*np.pi*A['A1']*t_(int(36*SR)))*env_ar(int(36*SR), 4.0, 3.0, 1.0), 4.5, 0.3)   # continuous sub
# Scene 4 motif: sparse, four notes.
for at, f in [(T4(18.6), A['Cs5']), (T4(20.4), A['E5']), (T4(22.8), A['B4']), (T4(25.2), A['A4'])]:
    place(bed, pluck(f, 2.4), at, 0.26, pan=np.sin(at))
# Final chord and arpeggio.
for i, f in enumerate([A['A3'], A['E4'], A['A4'], A['Cs5'], A['E5']]):
    place(bed, pluck(f, 3.2), END0 + 1.9 + i * 0.13, 0.3, pan=-0.3 + 0.15 * i)
place(bed_dry, pad([A['A2'], A['E3'], A['A3'], A['B3'], A['Cs4'], A['E4']], 5.0, a=0.25, r=3.0, cutoff=2200), END0 + 2.0, 0.5)
bed = reverb(bed, 3.2, 0.9) + bed_dry

# ---------------------------------------------------------------- fx
fx = np.zeros((N, 2))
# Scene 1 digital texture: sparse tiny clicks, then a riser.
for at in np.sort(rs.uniform(1.0, 5.6, 26)):
    f = rs.choice([1800, 2400, 3100, 4200])
    place(fx, hp(rs.randn(200), 2500) * np.exp(-t_(200) * 300) * 0.08, at, 1.0, pan=rs.uniform(-0.8, 0.8))
riser = noise_sweep(2.4, 400, 6000, shape=2.6)
riser[-int(0.03 * SR) :] *= np.linspace(1, 0, int(0.03 * SR))
place(fx, riser, 3.6, 0.35)
# Freeze impact + pull whoosh
place(fx, impact(1.6, 62, 36), 6.4, 0.55)
place(fx, noise_sweep(1.8, 300, 2200, shape=2.2), 6.7, 0.22)
place(fx, tick(900), 10.95, 0.12)
# Logo
place(fx, impact(2.6, 75, 40), 12.7, 0.75)
place(fx, pluck(A['A5'], 2.8), 12.72, 0.12)
place(fx, noise_sweep(1.0, 800, 5000, shape=1.6, rise=False), 12.7, 0.12)
# Lockup move, TV rise
place(fx, noise_sweep(0.9, 500, 3000, shape=1.8), 14.7, 0.16)
place(fx, noise_sweep(1.5, 300, 2500, shape=2.0), 16.0, 0.22)
place(fx, impact(1.2, 55, 42), 17.7, 0.28)
# Scene 4: UI ticks, each its own pitch/level.
for at, f, g in [(18.93, 1500, 0.2), (19.33, 1750, 0.18), (19.73, 2000, 0.17), (20.07, 1650, 0.16), (20.5, 1100, 0.2),
                 (23.27, 1400, 0.2), (26.0, 1250, 0.22)]:
    place(fx, tick(f), T4(at), g, pan=0.15)
place(fx, noise_sweep(1.6, 400, 4000, shape=1.6), 19.5, 0.14)   # stock Android home -> Lucerra home
place(fx, impact(1.2, 70, 44), 20.1, 0.22)
place(fx, impact(0.9, 90, 60), T4(21.1), 0.3)       # menu rises
place(fx, tick(2300), T4(23.0), 0.12)               # menu dismisses
place(fx, noise_sweep(1.0, 700, 3500, shape=1.4), T4(23.33), 0.12)   # row lift
place(fx, noise_sweep(0.7, 1200, 4500, shape=1.4), T4(26.2), 0.1)    # apps open
# Now Playing push, then pull back and settle (end scene starts 29.33 s)
place(fx, tick(1900), T4(28.45), 0.16, pan=0.2)
place(fx, noise_sweep(1.6, 250, 3200, shape=1.8), END0, 0.24)
place(fx, impact(1.4, 64, 40), END0 + 1.6, 0.45)
place(fx, noise_sweep(2.6, 2000, 9000, shape=1.2, rise=False), END0 + 2.05, 0.1)
fx = reverb(fx, 2.2, 0.5)

# ---------------------------------------------------------------- level so the Remotion mix (0.9 bed + 0.85 fx) peaks at -1.5 dBFS
mix = 0.9 * bed + 0.85 * fx
peak = np.abs(mix).max()
g = 10 ** (-1.5 / 20) / peak
bed *= g
fx *= g
os.makedirs(OUT, exist_ok=True)
for name, buf in [('bed', bed), ('fx', fx)]:
    buf = fade(np.clip(buf, -1, 1), 0.01)
    pcm = (buf * 32767).astype('<i2')
    with wave.open(os.path.join(OUT, f'{name}.wav'), 'wb') as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
final = 0.9 * bed + 0.85 * fx
print(f'mix peak {20*np.log10(np.abs(final).max()):.2f} dBFS, rms {20*np.log10(np.sqrt((final**2).mean())):.1f} dBFS')
