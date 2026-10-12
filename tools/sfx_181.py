#!/usr/bin/env python3
"""
0.1.8.1: original synthesized sound effects (no samples, no third-party Python modules).

The script writes 16-bit mono WAV files. Encode them to Ogg Vorbis with ffmpeg, for example:
  ffmpeg -y -i sfx_jetpack_ignite.wav -c:a libvorbis -q:a 5 sfx_jetpack_ignite.ogg

Sounds (loops are seamless; the end is cross-faded into the start):
  sfx_jetpack_electric_loop   electric ducted-fan thruster: fan whine, blade buzz, air rush,
                              low rumble (4 s loop)
  sfx_jetpack_advanced_loop   heavy jet thruster: deep roar, combustion crackle, jet hiss,
                              faint turbine (4 s loop)
  sfx_jetpack_ionic_loop      quantum ion drive: deep beating hum, swept resonant whoosh,
                              plasma hiss, ion crackle (4 s loop)
  sfx_jetpack_ignite          igniter clicks, spark burst, low thump, rising whoosh (0.9 s)
  sfx_jetpack_shutdown        falling spool-down whine, decaying hiss, pressure release,
                              metal tick (1.2 s)
  sfx_loop_compressor         reciprocating compressor: piston thumps (3 Hz), valve hiss,
                              motor hum (4 s loop)
  sfx_loop_servo              servo actuators: whir up / whir down, relay clicks, faint hum
                              (4 s loop)
  sfx_loop_discharge          electric discharge: arc crackle clusters, mains buzz, three
                              bright zaps (4 s loop)
  sfx_loop_bubbler            electrolysis / fermentation: random bubble chirps, liquid
                              rumble, faint hum (4 s loop)

Usage: sfx_181.py <output dir>
"""
import math
import os
import random
import struct
import sys
import wave

SR = 44100
rng = random.Random(20261012181)


def silence(sec):
    return [0.0] * int(SR * sec)


def mix(dst, src, at=0.0, gain=1.0):
    start = int(SR * at)
    for i, v in enumerate(src):
        j = start + i
        if j >= len(dst):
            break
        if j >= 0:
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


def brown(sec):
    out, s = [], 0.0
    for _ in range(int(SR * sec)):
        s = 0.995 * s + rng.uniform(-1, 1) * 0.1
        out.append(s)
    return out


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


def shaped(x, fn):
    n = len(x)
    return [v * fn(i / SR, i / max(1, n - 1)) for i, v in enumerate(x)]


def normalize(x, peak=0.89):
    m = max(1e-9, max(abs(v) for v in x))
    return [v * peak / m for v in x]


def seamless(x, fade=0.3):
    """Cross-fades the end into the start, so the loop has no click."""
    n = int(SR * fade)
    body, tail = x[:-n], x[-n:]
    for i in range(n):
        k = i / n
        body[i] = body[i] * k + tail[i] * (1 - k)
    return body


def write(path, x):
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, v)) * 32767)) for v in x))


LOOP = 4.0
TOTAL = LOOP + 0.3


# ---------------------------------------------------------------- jetpack loops
def jetpack_electric_loop():
    out = silence(TOTAL)
    # fan whine: 1180 Hz with a slow vibrato, a quiet second harmonic
    whine = tone(lambda t: 1180 * (1 + 0.004 * math.sin(2 * math.pi * 0.75 * t)), TOTAL)
    mix(out, whine, 0, 0.07)
    mix(out, tone(lambda t: 2360 * (1 + 0.004 * math.sin(2 * math.pi * 0.75 * t)), TOTAL), 0, 0.025)
    # blade buzz: 185 Hz saw through a band-pass, gentle tremolo at 6.25 Hz
    buzz = bandpass(tone(lambda t: 185, TOTAL, saw), 150, 900)
    mix(out, shaped(buzz, lambda t, k: 0.8 + 0.2 * math.sin(2 * math.pi * 6.25 * t)), 0, 0.22)
    # air rush: band-passed noise with a small flutter
    air = bandpass(noise(TOTAL), 400, 4200)
    mix(out, shaped(air, lambda t, k: 0.85 + 0.15 * math.sin(2 * math.pi * 3.0 * t)), 0, 0.55)
    # low rumble
    mix(out, lowpass(noise(TOTAL), 160), 0, 1.6)
    return normalize(seamless(out), 0.72)


def jetpack_advanced_loop():
    out = silence(TOTAL)
    # deep roar: brown noise low-passed (DC removed by a 28 Hz high-pass), plus a low band of white noise
    mix(out, highpass(lowpass(brown(TOTAL), 220), 28), 0, 0.9)
    mix(out, bandpass(noise(TOTAL), 60, 420), 0, 2.2)
    mix(out, bandpass(noise(TOTAL), 300, 900), 0, 0.9)
    # jet hiss: wide band noise with a slow swell (0.5 Hz)
    hiss = bandpass(noise(TOTAL), 900, 6500)
    mix(out, shaped(hiss, lambda t, k: 0.75 + 0.25 * math.sin(2 * math.pi * 0.5 * t)), 0, 1.2)
    # combustion crackle: sparse sharp impulses, about 45 per second
    crackle = silence(TOTAL)
    t = 0.0
    while t < TOTAL - 0.02:
        burst = highpass(noise(0.012), 1500)
        burst = [v * e for v, e in zip(burst, env_exp(len(burst), 0.0025, 0.0002))]
        mix(crackle, burst, t, rng.uniform(0.3, 1.0))
        t += rng.expovariate(45)
    mix(out, crackle, 0, 0.7)
    # faint turbine core
    mix(out, tone(lambda t: 820 + 4 * math.sin(2 * math.pi * 0.5 * t), TOTAL), 0, 0.05)
    mix(out, tone(lambda t: 1640, TOTAL), 0, 0.02)
    return normalize(seamless(out), 0.8)


def jetpack_ionic_loop():
    out = silence(TOTAL)
    # deep hum with a slow beat: 55 Hz and 55.5 Hz, harmonics
    for f, g in ((55.0, 0.14), (55.5, 0.11), (110.0, 0.09), (165.0, 0.06), (220.25, 0.04)):
        mix(out, tone(lambda t, f=f: f, TOTAL), 0, g)
    # resonant whoosh: band of noise swept by a 0.5 Hz LFO (two passes, slow and fast)
    swirl = silence(TOTAL)
    base = noise(TOTAL)
    lo_band = bandpass(base, 250, 700)
    hi_band = bandpass(base, 700, 1600)
    for i in range(len(swirl)):
        k = 0.5 + 0.5 * math.sin(2 * math.pi * 0.5 * i / SR)
        swirl[i] = lo_band[i] * (1 - k) + hi_band[i] * k
    mix(out, swirl, 0, 1.5)
    # plasma hiss with a fast flicker
    hiss = highpass(noise(TOTAL), 3500)
    mix(out, shaped(hiss, lambda t, k: 0.6 + 0.4 * abs(math.sin(2 * math.pi * 11 * t))), 0, 0.22)
    # ion crackle: short bright ticks, about 8 per second
    t = 0.05
    while t < TOTAL - 0.03:
        tick = highpass(noise(0.02), 4000)
        tick = [v * e for v, e in zip(tick, env_exp(len(tick), 0.004, 0.0003))]
        mix(out, tick, t, rng.uniform(0.3, 0.6))
        t += rng.expovariate(8)
    return normalize(seamless(out), 0.75)


def jetpack_ignite():
    out = silence(0.95)
    # two igniter clicks
    for at in (0.0, 0.045):
        click = highpass(noise(0.01), 3000)
        click = [v * e for v, e in zip(click, env_exp(len(click), 0.002, 0.0002))]
        mix(out, click, at, 0.8)
    # spark burst: dense crackle 0.05-0.2 s
    t = 0.05
    while t < 0.2:
        burst = highpass(noise(0.008), 2000)
        burst = [v * e for v, e in zip(burst, env_exp(len(burst), 0.002, 0.0002))]
        mix(out, burst, t, rng.uniform(0.3, 0.7))
        t += rng.expovariate(140)
    # low thump when the flame catches
    thump = tone(lambda t: 80 * math.exp(-t * 6) + 38, 0.5)
    mix(out, [v * e for v, e in zip(thump, env_exp(len(thump), 0.12, 0.002))], 0.09, 0.9)
    # rising whoosh: noise through a band whose upper edge rises, swelling then settling
    n = int(SR * 0.8)
    src = noise(0.8)
    whoosh, s_lo, s_hi = [], 0.0, 0.0
    for i in range(n):
        t = i / SR
        hi = 600 + 5000 * min(1.0, t / 0.45)
        a_hi = 1 - math.exp(-2 * math.pi * hi / SR)
        a_lo = 1 - math.exp(-2 * math.pi * 250 / SR)
        s_hi += a_hi * (src[i] - s_hi)
        s_lo += a_lo * (s_hi - s_lo)
        swell = min(1.0, t / 0.35) * (1 - 0.45 * max(0.0, (t - 0.45) / 0.35))
        whoosh.append((s_hi - s_lo) * swell)
    mix(out, whoosh, 0.08, 0.9)
    return normalize(out, 0.85)


def jetpack_shutdown():
    out = silence(1.25)
    # spool-down whine: 1100 Hz falling to about 250 Hz, decaying
    whine = tone(lambda t: 250 + 850 * math.exp(-t * 2.6), 1.1)
    mix(out, [v * math.exp(-i / (SR * 0.45)) for i, v in enumerate(whine)], 0, 0.22)
    # decaying air hiss
    hiss = bandpass(noise(0.9), 500, 4500)
    mix(out, [v * math.exp(-i / (SR * 0.22)) for i, v in enumerate(hiss)], 0, 0.6)
    # low rumble tail
    rumble = lowpass(noise(0.7), 180)
    mix(out, [v * math.exp(-i / (SR * 0.18)) for i, v in enumerate(rumble)], 0, 2.0)
    # pressure release at 0.55 s
    rel = bandpass(noise(0.35), 2500, 8000)
    mix(out, [v * min(1, i / (SR * 0.02)) * math.exp(-i / (SR * 0.09)) for i, v in enumerate(rel)], 0.55, 0.3)
    # metal tick of the cooling nozzle at 0.95 s
    for freq, gain, tau in ((1900, 0.18, 0.05), (3100, 0.1, 0.03)):
        ring = tone(lambda t, f=freq: f, 0.2)
        mix(out, [v * e for v, e in zip(ring, env_exp(len(ring), tau, 0.0005))], 0.95, gain)
    return normalize(out, 0.8)


# ---------------------------------------------------------------- machine loops
def loop_compressor():
    out = silence(TOTAL)
    # motor hum 100 Hz and 200 Hz, and a light mechanical rattle band
    mix(out, tone(lambda t: 100, TOTAL), 0, 0.08)
    mix(out, tone(lambda t: 200, TOTAL), 0, 0.05)
    mix(out, bandpass(noise(TOTAL), 400, 1400), 0, 0.12)
    # piston strokes at 3 Hz: low thump plus a valve hiss after each stroke
    period = 1 / 3.0
    k = 0
    while k * period < TOTAL - 0.05:
        at = k * period
        thump = tone(lambda t: 72 * math.exp(-t * 10) + 45, 0.18)
        mix(out, [v * e for v, e in zip(thump, env_exp(len(thump), 0.05, 0.002))], at, 0.4)
        knock = bandpass(noise(0.03), 300, 1500)
        mix(out, [v * e for v, e in zip(knock, env_exp(len(knock), 0.008, 0.0005))], at, 0.9)
        hiss = bandpass(noise(0.14), 2500, 8000)
        mix(out, [v * math.sin(math.pi * i / len(hiss)) for i, v in enumerate(hiss)], at + 0.12, 0.45)
        k += 1
    return normalize(seamless(out), 0.65)


def loop_servo():
    out = silence(TOTAL)
    mix(out, tone(lambda t: 120, TOTAL), 0, 0.04)
    mix(out, bandpass(noise(TOTAL), 80, 400), 0, 0.25)

    def whir(f0, f1, sec):
        w = tone(lambda t: f0 + (f1 - f0) * (t / sec), sec, saw)
        w = bandpass(w, 300, 2600)
        n = len(w)
        return [v * min(1, i / (SR * 0.03)) * min(1, (n - i) / (SR * 0.05)) for i, v in enumerate(w)]

    def relay():
        c = highpass(noise(0.012), 2000)
        return [v * e for v, e in zip(c, env_exp(len(c), 0.0025, 0.0002))]

    for base in (0.0, 2.0):
        mix(out, whir(380, 980, 0.38), base + 0.15, 0.22)
        mix(out, relay(), base + 0.56, 0.5)
        mix(out, whir(900, 420, 0.32), base + 1.05, 0.2)
        mix(out, relay(), base + 1.4, 0.45)
        mix(out, whir(520, 760, 0.18), base + 1.6, 0.14)
    return normalize(seamless(out), 0.6)


def loop_discharge():
    out = silence(TOTAL)
    # mains buzz: 100 Hz saw, band-limited
    buzz = bandpass(tone(lambda t: 100, TOTAL, saw), 90, 1800)
    mix(out, shaped(buzz, lambda t, k: 0.75 + 0.25 * math.sin(2 * math.pi * 1.5 * t)), 0, 0.2)
    # arc crackle in clusters
    t = 0.02
    while t < TOTAL - 0.05:
        cluster = rng.randint(3, 9)
        ct = t
        for _ in range(cluster):
            burst = highpass(noise(0.006), 2500)
            burst = [v * e for v, e in zip(burst, env_exp(len(burst), 0.0015, 0.0001))]
            mix(out, burst, ct, rng.uniform(0.2, 0.6))
            ct += rng.uniform(0.004, 0.018)
        t += rng.expovariate(5.0)
    # three bright zaps
    for at in (0.6, 1.9, 3.2):
        zap = highpass(noise(0.07), 1200)
        zap = [v * e for v, e in zip(zap, env_exp(len(zap), 0.02, 0.0005))]
        mix(out, zap, at, 0.9)
        crack = tone(lambda t: 2400 * math.exp(-t * 30) + 300, 0.06, saw)
        mix(out, [v * e for v, e in zip(crack, env_exp(len(crack), 0.015, 0.0005))], at, 0.2)
    return normalize(seamless(out), 0.62)


def loop_bubbler():
    out = silence(TOTAL)
    # liquid rumble and a faint electric hum
    mix(out, lowpass(noise(TOTAL), 280), 0, 0.9)
    mix(out, tone(lambda t: 100, TOTAL), 0, 0.025)
    # bubbles: short rising chirps at random times, about 22 per second
    t = 0.0
    while t < TOTAL - 0.06:
        f0 = rng.uniform(380, 900)
        dur = rng.uniform(0.018, 0.045)
        chirp = tone(lambda tt, f0=f0, dur=dur: f0 * (1 + 1.4 * tt / dur), dur)
        chirp = [v * math.sin(math.pi * i / len(chirp)) ** 1.5 for i, v in enumerate(chirp)]
        mix(out, chirp, t, rng.uniform(0.08, 0.22))
        t += rng.expovariate(22)
    return normalize(seamless(out), 0.6)


SOUNDS = (
    ('sfx_jetpack_electric_loop', jetpack_electric_loop),
    ('sfx_jetpack_advanced_loop', jetpack_advanced_loop),
    ('sfx_jetpack_ionic_loop', jetpack_ionic_loop),
    ('sfx_jetpack_ignite', jetpack_ignite),
    ('sfx_jetpack_shutdown', jetpack_shutdown),
    ('sfx_loop_compressor', loop_compressor),
    ('sfx_loop_servo', loop_servo),
    ('sfx_loop_discharge', loop_discharge),
    ('sfx_loop_bubbler', loop_bubbler),
)


def main():
    folder = sys.argv[1] if len(sys.argv) > 1 else '.'
    os.makedirs(folder, exist_ok=True)
    for name, fn in SOUNDS:
        path = os.path.join(folder, name + '.wav')
        write(path, fn())
        print('wrote', path)


if __name__ == '__main__':
    main()
