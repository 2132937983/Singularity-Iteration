#!/usr/bin/env python3
"""
0.1.10.3: calmer, heavier jetpack sounds (helpers of sfx_181; pure Python, Ogg Vorbis via ffmpeg).

Player report: the thruster loops were too loud, and a strange "biu" sounded when the thrust ended.
Measured on the 0.1.10.2 files: the loops ran at about -15.7 dBFS RMS, and the shutdown cue was
built on a falling sine sweep (its dominant tone moved from 1050 Hz to 325 Hz in the first second):
that sweep was the "biu". This tool rebuilds:

  sfx_jetpack_shutdown        flame-out: a short low thump, the exhaust roar darkening as it dies
                              (a falling low-pass on noise, no tone), a rumble tail and a soft gas
                              release; no tonal sweep, no metal tick (1.0 s)
  sfx_jetpack_electric_loop   ducted fan: air rush and a low body carry it; the fan tone is a quiet
                              740 Hz hum instead of the 1180 Hz whine (3.6 s seamless loop)
  sfx_jetpack_advanced_loop   jet: deep roar and low band; hiss and combustion crackle softer
  sfx_jetpack_ionic_loop      ion drive: the beating hum and the swept whoosh; plasma hiss and ion
                              crackle softer

Loops are cross-faded with equal power (the end flows into the start) and every file is
normalised lower than before (about -19 to -21 dBFS RMS). The ignite cue keeps its file; the game
plays it quieter.

Usage: sfx_1103.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import subprocess
import sys
import wave
import struct

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_181 as s  # noqa: E402

SR = s.SR
s.rng.seed(20261014103)
LOOP = 3.6
FADE = 0.4
TOTAL = LOOP + FADE


def seamless(x, fade=FADE):
    """Equal-power cross-fade of the last {@code fade} seconds into the first; the tail is cut."""
    n = len(x)
    f = int(SR * fade)
    out = x[:n - f]
    for i in range(f):
        t = i / f
        out[i] = x[i] * math.sin(t * math.pi / 2) + x[n - f + i] * math.cos(t * math.pi / 2)
    return out


def rms(x):
    return math.sqrt(sum(v * v for v in x) / max(1, len(x)))


def to_rms(x, target_db, peak_cap=0.62):
    """Scales to an RMS level (dBFS), with the peak kept under {@code peak_cap}."""
    g = 10 ** (target_db / 20) / max(1e-9, rms(x))
    m = max(abs(v) for v in x) * g
    if m > peak_cap:
        g *= peak_cap / m
    return [v * g for v in x]


def electric_loop():
    out = s.silence(TOTAL)
    # low body: the ducted fan's pressure, 60-260 Hz, slow flutter
    body = s.bandpass(s.noise(TOTAL), 60, 260)
    s.mix(out, s.shaped(body, lambda t, k: 0.85 + 0.15 * math.sin(2 * math.pi * 2.5 * t)), 0, 2.6)
    # air rush: the main voice, rounded on top
    air = s.bandpass(s.noise(TOTAL), 250, 2800)
    s.mix(out, s.shaped(air, lambda t, k: 0.88 + 0.12 * math.sin(2 * math.pi * 1.25 * t)), 0, 0.9)
    # fan hum: a quiet 740 Hz tone with a slow vibrato (the old 1180 Hz whine was harsh)
    s.mix(out, s.tone(lambda t: 740 * (1 + 0.003 * math.sin(2 * math.pi * 0.75 * t)), TOTAL), 0, 0.018)
    # blade buzz, low and soft
    buzz = s.lowpass(s.tone(lambda t: 120, TOTAL, s.saw), 600)
    s.mix(out, buzz, 0, 0.12)
    # rumble
    s.mix(out, s.lowpass(s.noise(TOTAL), 140), 0, 1.8)
    return to_rms(seamless(out), -20.5)


def advanced_loop():
    out = s.silence(TOTAL)
    s.mix(out, s.highpass(s.lowpass(s.brown(TOTAL), 200), 26), 0, 1.0)
    s.mix(out, s.bandpass(s.noise(TOTAL), 50, 380), 0, 2.6)
    s.mix(out, s.bandpass(s.noise(TOTAL), 300, 900), 0, 0.8)
    hiss = s.bandpass(s.noise(TOTAL), 900, 5000)
    s.mix(out, s.shaped(hiss, lambda t, k: 0.8 + 0.2 * math.sin(2 * math.pi * 0.5 * t)), 0, 0.6)
    crackle = s.silence(TOTAL)
    t = 0.0
    while t < TOTAL - 0.02:
        burst = s.highpass(s.noise(0.012), 1500)
        burst = [v * e for v, e in zip(burst, s.env_exp(len(burst), 0.0025, 0.0002))]
        s.mix(crackle, burst, t, s.rng.uniform(0.25, 0.7))
        t += s.rng.expovariate(30)
    s.mix(out, crackle, 0, 0.32)
    return to_rms(seamless(out), -19.5)


def ionic_loop():
    out = s.silence(TOTAL)
    for f, g in ((55.0, 0.14), (55.5, 0.11), (110.0, 0.08), (165.0, 0.04)):
        s.mix(out, s.tone(lambda t, f=f: f, TOTAL), 0, g)
    swirl = s.silence(TOTAL)
    base = s.noise(TOTAL)
    lo_band = s.bandpass(base, 220, 650)
    hi_band = s.bandpass(base, 650, 1400)
    for i in range(len(swirl)):
        k = 0.5 + 0.5 * math.sin(2 * math.pi * (1 / LOOP) * 2 * i / SR)   # two sweeps per loop: seamless
        swirl[i] = lo_band[i] * (1 - k) + hi_band[i] * k
    s.mix(out, swirl, 0, 1.4)
    hiss = s.highpass(s.noise(TOTAL), 3500)
    s.mix(out, s.shaped(hiss, lambda t, k: 0.7 + 0.3 * abs(math.sin(2 * math.pi * 11 * t))), 0, 0.08)
    t = 0.05
    while t < TOTAL - 0.03:
        tick = s.highpass(s.noise(0.02), 4000)
        tick = [v * e for v, e in zip(tick, s.env_exp(len(tick), 0.004, 0.0003))]
        s.mix(out, tick, t, s.rng.uniform(0.1, 0.25))
        t += s.rng.expovariate(6)
    return to_rms(seamless(out), -20.0)


def shutdown():
    out = s.silence(1.0)
    # flame-out thump: very low, short (below 70 Hz the small drop reads as a thump, not a sweep)
    thump = s.tone(lambda t: 38 + 26 * math.exp(-t * 18), 0.3)
    s.mix(out, [v * e for v, e in zip(thump, s.env_exp(len(thump), 0.08, 0.004))], 0, 0.9)
    # exhaust roar dying away: noise through a low-pass whose cutoff falls from 3500 to 300 Hz
    src = s.noise(0.9)
    roar, st = [], 0.0
    for i, v in enumerate(src):
        t = i / SR
        cut = 300 + 3200 * math.exp(-t * 4.0)
        a = 1 - math.exp(-2 * math.pi * cut / SR)
        st += a * (v - st)
        roar.append(st * math.exp(-t / 0.24) * min(1.0, t / 0.01))
    s.mix(out, roar, 0, 1.3)
    # rumble tail
    rumble = s.lowpass(s.noise(0.8), 150)
    s.mix(out, [v * math.exp(-i / (SR * 0.28)) for i, v in enumerate(rumble)], 0, 2.2)
    # soft gas release from the nozzle
    rel = s.bandpass(s.noise(0.4), 1500, 5000)
    s.mix(out, [v * min(1, i / (SR * 0.05)) * math.exp(-i / (SR * 0.12)) for i, v in enumerate(rel)], 0.2, 0.18)
    # fade the very end to silence
    n = len(out)
    for i in range(int(n * 0.85), n):
        out[i] *= (n - i) / (n * 0.15)
    return to_rms(out, -23.0, 0.55)


SOUNDS = (
    ('sfx_jetpack_shutdown', shutdown),
    ('sfx_jetpack_electric_loop', electric_loop),
    ('sfx_jetpack_advanced_loop', advanced_loop),
    ('sfx_jetpack_ionic_loop', ionic_loop),
)


def decode_rms(ffmpeg, path, work, name):
    raw = os.path.join(work, name + '.chk.raw')
    subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', path, '-f', 's16le', '-ac', '1', '-ar', str(SR), raw], check=True)
    import array
    a = array.array('h')
    a.frombytes(open(raw, 'rb').read())
    x = [v / 32768 for v in a]
    return 20 * math.log10(max(1e-9, rms(x))), max(abs(v) for v in x)


def main():
    ffmpeg, sounds, work = sys.argv[1:4]
    os.makedirs(work, exist_ok=True)
    for name, fn in SOUNDS:
        ogg = os.path.join(sounds, name + '.ogg')
        before = decode_rms(ffmpeg, ogg, work, name) if os.path.exists(ogg) else (float('nan'), 0)
        wav = os.path.join(work, name + '.wav')
        s.write(wav, fn())
        subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1', ogg], check=True)
        after = decode_rms(ffmpeg, ogg, work, name)
        print('%-28s %.1f dBFS (peak %.2f) -> %.1f dBFS (peak %.2f)' % (name, before[0], before[1], after[0], after[1]))


if __name__ == '__main__':
    main()
