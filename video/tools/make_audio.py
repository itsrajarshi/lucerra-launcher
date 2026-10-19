"""Original sound design for the Lucerra film. Pure synthesis (numpy/scipy): no third-party samples,
so the result can be distributed with the video.

Writes public/audio/bed.wav (ambience + music) and public/audio/fx.wav (hits, whooshes, UI ticks).
Replace either file with your own 14.1 s, 44.1 kHz stereo WAV to change the sound.
"""
import os
import wave

import numpy as np
from scipy.signal import butter, fftconvolve, lfilter

SR = 44100
DUR = 14.07
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


SLOW = 1.635          # scene-4 stretch factor vs. original pace is now a compression
def T4(t):
    """Old scene-4 time (18-28 s at original pace) -> film time (experience scene starts 3.2 s)."""
    return 3.2 + (t - 18.0) / SLOW
END0 = 304 / 30

A = dict(A1=55.0, E2=82.4, A2=110.0, E3=164.8, B3=246.9, Cs4=277.2, A3=220.0, E4=329.6, A4=440.0, B4=493.9, Cs5=554.4, E5=659.3, A5=880.0)

# One soft continuous pad (never stops), a few plucks, tiny UI ticks. No whooshes, risers or impacts.
bed_dry = np.zeros((N, 2))
bed = np.zeros((N, 2))
place(bed_dry, pad([A['A1'], A['E2'], A['A2'], A['E3']], DUR, a=1.5, r=2.0, cutoff=700), 0.0, 0.34)
place(bed_dry, pad([A['B3'], A['Cs4'], A['E4']], DUR - 1.7, a=1.2, r=2.0, cutoff=1500), 1.7, 0.22)   # brightens as Lucerra arrives
for at, f in [(1.75, A['A5']), (T4(18.6), A['Cs5']), (T4(20.4), A['E5']), (T4(22.8), A['B4']), (T4(25.2), A['A4'])]:
    place(bed, pluck(f, 2.4), at, 0.2, pan=np.sin(at))
for i, f in enumerate([A['A3'], A['E4'], A['A4'], A['Cs5'], A['E5']]):
    place(bed, pluck(f, 3.2), END0 + 0.5 + i * 0.13, 0.26, pan=-0.3 + 0.15 * i)
bed = reverb(bed, 3.0, 0.8) + bed_dry

fx = np.zeros((N, 2))
for at, f, g in [(18.93, 1500, 0.1), (19.33, 1750, 0.09), (19.73, 2000, 0.08), (20.07, 1650, 0.08), (21.1, 1100, 0.1),
                 (23.0, 2300, 0.07), (23.27, 1400, 0.1), (26.0, 1250, 0.1), (27.9, 1900, 0.08)]:
    place(fx, tick(f), T4(at), g, pan=0.15)
fx = reverb(fx, 1.5, 0.3)

mix = 0.9 * bed + 0.85 * fx
g = 10 ** (-1.5 / 20) / np.abs(mix).max()
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
