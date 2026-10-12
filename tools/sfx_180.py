#!/usr/bin/env python3
"""
0.1.8.0: original synthesized sound effects (no samples, no third-party Python modules).

The script writes 16-bit mono WAV files. Encode them to Ogg Vorbis with ffmpeg, for example:
  ffmpeg -y -i sfx_armory_seal.wav -c:a libvorbis -q:a 5 sfx_armory_seal.ogg

Sounds:
  sfx_armory_seal       heavy mechanical lock of a suit piece: servo whir, bolt slide, deep
                        clunk, metallic ring, pressure seal hiss (about 1.1 s)
  sfx_uu_fabricator     UU-matter fabricator loop: soft harmonic hum, slow shimmer, quiet
                        crystalline chime; a seamless 4 s loop
  sfx_kinetic_drive     kinetic generator loop: smooth turbine tone with a gentle pulse; a
                        seamless 4 s loop

Usage: sfx_180.py <output dir>
"""
import math
import os
import random
import struct
import sys
import wave

SR = 44100
rng = random.Random(20261012)


def silence(sec):
    return [0.0] * int(SR * sec)


def mix(dst, src, at=0.0, gain=1.0):
    start = int(SR * at)
    for i, v in enumerate(src):
        j = start + i
        if j >= len(dst):
            break
        dst[j] += v * gain
    return dst


def lowpass(x, cutoff):
    a = 1 - math.exp(-2 * math.pi * cutoff / SR)
    y, s = [], 0.0
    for v in x:
        s += a * (v - s)
        y.append(s)
    return y


def highpass(x, cutoff):
    low = lowpass(x, cutoff)
    return [a - b for a, b in zip(x, low)]


def bandpass(x, lo, hi):
    return lowpass(highpass(x, lo), hi)


def noise(sec):
    return [rng.uniform(-1, 1) for _ in range(int(SR * sec))]


def env_exp(n, tau, attack=0.002):
    out = []
    for i in range(n):
        t = i / SR
        a = min(1.0, t / attack) if attack > 0 else 1.0
        out.append(a * math.exp(-t / tau))
    return out


def tone(freq_fn, sec, wave_fn=math.sin):
    out, phase = [], 0.0
    for i in range(int(SR * sec)):
        phase += 2 * math.pi * freq_fn(i / SR) / SR
        out.append(wave_fn(phase))
    return out


def saw(phase):
    return 2 * ((phase / (2 * math.pi)) % 1.0) - 1


def normalize(x, peak=0.89):
    m = max(1e-9, max(abs(v) for v in x))
    return [v * peak / m for v in x]


def write(path, x):
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, v)) * 32767)) for v in x))


def armory_seal():
    out = silence(1.15)
    # 1. servo whir: rising saw through a band-pass, 0.00-0.16 s
    whir = tone(lambda t: 260 + 900 * (t / 0.16) ** 1.4, 0.16, saw)
    whir = bandpass(whir, 400, 2200)
    whir = [v * min(1, i / (SR * 0.02)) * (1 - i / len(whir)) ** 0.5 for i, v in enumerate(whir)]
    mix(out, whir, 0.0, 0.28)
    # 2. bolt slide: short scrape of filtered noise, 0.14-0.22 s
    slide = bandpass(noise(0.08), 1800, 6000)
    slide = [v * math.sin(math.pi * i / len(slide)) for i, v in enumerate(slide)]
    mix(out, slide, 0.14, 0.22)
    # 3. deep clunk: pitched-down sine thump and low noise burst, at 0.22 s
    thump = tone(lambda t: 95 * math.exp(-t * 9) + 42, 0.45)
    thump = [v * e for v, e in zip(thump, env_exp(len(thump), 0.11, 0.001))]
    mix(out, thump, 0.22, 0.95)
    body = lowpass(noise(0.12), 420)
    body = [v * e for v, e in zip(body, env_exp(len(body), 0.03, 0.0005))]
    mix(out, body, 0.22, 0.9)
    # 4. metallic ring: inharmonic partials, fast decay
    for freq, gain, tau in ((612, 0.20, 0.22), (1381, 0.16, 0.15), (2211, 0.10, 0.09), (3517, 0.06, 0.05)):
        ring = tone(lambda t, f=freq: f, 0.5)
        ring = [v * e for v, e in zip(ring, env_exp(len(ring), tau, 0.0008))]
        mix(out, ring, 0.225, gain)
    # 5. second, lighter lock click at 0.34 s
    click = highpass(noise(0.02), 2500)
    click = [v * e for v, e in zip(click, env_exp(len(click), 0.004, 0.0002))]
    mix(out, click, 0.34, 0.5)
    # 6. pressure seal hiss: band-passed noise with a soft tail, 0.40-1.10 s
    hiss = bandpass(noise(0.7), 2500, 9000)
    hiss = [v * (min(1, i / (SR * 0.04))) * math.exp(-i / (SR * 0.25)) for i, v in enumerate(hiss)]
    mix(out, hiss, 0.40, 0.18)
    return normalize(out)


def seamless(x, fade=0.25):
    """Cross-fades the end into the start, so the loop has no click."""
    n = int(SR * fade)
    body, tail = x[:-n], x[-n:]
    for i in range(n):
        k = i / n
        body[i] = body[i] * k + tail[i] * (1 - k)
    return body


def uu_fabricator():
    sec = 4.25
    out = silence(sec)
    # harmonic hum: a soft chord (110, 165, 220, 330 Hz) with slow tremolo
    for freq, gain in ((110, 0.30), (165, 0.18), (220, 0.14), (330, 0.07), (440, 0.04)):
        hum = tone(lambda t, f=freq: f * (1 + 0.002 * math.sin(2 * math.pi * 0.5 * t)), sec)
        hum = [v * (0.85 + 0.15 * math.sin(2 * math.pi * 0.75 * i / SR + freq)) for i, v in enumerate(hum)]
        mix(out, hum, 0, gain)
    # shimmer: high band noise, slowly modulated
    sh = bandpass(noise(sec), 4000, 9000)
    sh = [v * (0.5 + 0.5 * math.sin(2 * math.pi * 0.5 * i / SR)) for i, v in enumerate(sh)]
    mix(out, sh, 0, 0.05)
    # crystalline chime: two soft bell tones per cycle
    for at, freq in ((0.4, 1318.5), (2.4, 1567.98)):
        bell = tone(lambda t, f=freq: f, 1.5)
        bell = [v * e for v, e in zip(bell, env_exp(len(bell), 0.5, 0.01))]
        mix(out, bell, at, 0.06)
    return normalize(seamless(out), 0.6)


def kinetic_drive():
    sec = 4.25
    out = silence(sec)
    # turbine tone: a sine at 180 Hz with a whine at 4x, gently pulsing at 2 Hz
    base = tone(lambda t: 180, sec)
    whine = tone(lambda t: 720 + 6 * math.sin(2 * math.pi * 0.5 * t), sec)
    for i in range(len(out)):
        pulse = 0.8 + 0.2 * math.sin(2 * math.pi * 2.0 * i / SR)
        out[i] += base[i] * 0.35 * pulse + whine[i] * 0.08
    # air: low band noise
    air = bandpass(noise(sec), 200, 1500)
    mix(out, air, 0, 0.12)
    return normalize(seamless(out), 0.6)


def main():
    folder = sys.argv[1] if len(sys.argv) > 1 else '.'
    os.makedirs(folder, exist_ok=True)
    for name, fn in (('sfx_armory_seal', armory_seal), ('sfx_uu_fabricator', uu_fabricator), ('sfx_kinetic_drive', kinetic_drive)):
        path = os.path.join(folder, name + '.wav')
        write(path, fn())
        print('wrote', path)


if __name__ == '__main__':
    main()
