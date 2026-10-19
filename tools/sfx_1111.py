#!/usr/bin/env python3
"""
0.1.10.11: the sound of the mining laser's Ultra-High Energy Blast (helpers of sfx_181; pure
Python, Ogg Vorbis via ffmpeg). Before, the blast used the vanilla explosion sound.

  sfx_laser_ultra_blast   (3.2 s) an energy blast, not a chemical one:
                          - a 60 ms charge snap (bright crackle, a short rising zap)
                          - the plasma crack: a broadband burst with a sharp attack
                          - the boom: a deep sub thump (32-70 Hz) and a low body (40-200 Hz)
                          - an electric sizzle that dies away (band noise with random arcs)
                          - a long rolling rumble tail (low-passed brown noise)

The game plays explosion sounds at about pitch 0.7 (Explosion#finalizeExplosion); the bright
layers are tuned a little higher for that. Normalised to about -14 dBFS RMS, peak 0.9.

Usage: sfx_1111.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_181 as s  # noqa: E402

SR = s.SR
s.rng.seed(20261019111)
TOTAL = 3.2


def rms(x):
    return math.sqrt(sum(v * v for v in x) / max(1, len(x)))


def to_rms(x, target_db, peak_cap=0.9):
    g = 10 ** (target_db / 20) / max(1e-9, rms(x))
    m = max(abs(v) for v in x) * g
    if m > peak_cap:
        g *= peak_cap / m
    return [v * g for v in x]


def env(x, attack, tau):
    return [v * min(1.0, i / max(1, SR * attack)) * math.exp(-i / (SR * tau)) for i, v in enumerate(x)]


def ultra_blast():
    out = s.silence(TOTAL)
    # charge snap: a short rising zap and bright crackle just before the blast
    zap = s.tone(lambda t: 900 + 5200 * t / 0.06, 0.06, s.saw)
    s.mix(out, env(s.highpass(zap, 700), 0.004, 0.03), 0.0, 0.16)
    crackle = s.highpass(s.noise(0.07), 3500)
    s.mix(out, env(crackle, 0.002, 0.025), 0.0, 0.5)
    t0 = 0.055
    # plasma crack: broadband burst, very sharp attack
    crack = s.bandpass(s.noise(0.5), 400, 9000)
    s.mix(out, env(crack, 0.0015, 0.07), t0, 1.4)
    crack_hi = s.highpass(s.noise(0.25), 5000)
    s.mix(out, env(crack_hi, 0.001, 0.035), t0, 0.7)
    # boom: deep sub thump with a falling pitch (below 70 Hz it reads as pressure, not a sweep)
    thump = s.tone(lambda t: 32 + 38 * math.exp(-t * 9), 1.6)
    s.mix(out, env(thump, 0.006, 0.42), t0, 1.25)
    body = s.bandpass(s.noise(1.4), 40, 200)
    s.mix(out, env(body, 0.01, 0.35), t0, 3.2)
    # electric sizzle: band noise that dies away, with random arc pops
    sizzle = s.bandpass(s.noise(1.6), 1800, 7000)
    sizzle = [v * (0.6 + 0.4 * math.sin(2 * math.pi * 31 * i / SR) ** 2) for i, v in enumerate(sizzle)]
    s.mix(out, env(sizzle, 0.02, 0.38), t0 + 0.05, 0.22)
    t = t0 + 0.08
    while t < 1.6:
        pop = s.highpass(s.noise(0.012), 2500)
        level = 0.35 * math.exp(-(t - t0) / 0.6)
        s.mix(out, env(pop, 0.0005, 0.004), t, s.rng.uniform(0.4, 1.0) * level)
        t += s.rng.expovariate(22)
    # rolling rumble tail: low-passed brown noise, slow swell and decay
    rumble = s.lowpass(s.brown(TOTAL - t0), 120)
    rumble = [v * min(1.0, i / (SR * 0.12)) * math.exp(-i / (SR * 0.95)) for i, v in enumerate(rumble)]
    s.mix(out, rumble, t0, 2.6)
    # fade the last 0.4 s to silence
    n = len(out)
    f = int(SR * 0.4)
    for i in range(n - f, n):
        out[i] *= (n - i) / f
    return to_rms(out, -14.0)


SOUNDS = (
    ('sfx_laser_ultra_blast', ultra_blast),
)


def decode_rms(ffmpeg, path, work, name):
    raw = os.path.join(work, name + '.chk.raw')
    subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', path, '-f', 's16le', '-ac', '1', '-ar', str(SR), raw], check=True)
    import array
    a = array.array('h')
    a.frombytes(open(raw, 'rb').read())
    x = [v / 32768 for v in a]
    return 20 * math.log10(max(1e-9, rms(x))), max(abs(v) for v in x), len(x) / SR


def main():
    ffmpeg, sounds, work = sys.argv[1:4]
    os.makedirs(work, exist_ok=True)
    for name, fn in SOUNDS:
        ogg = os.path.join(sounds, name + '.ogg')
        wav = os.path.join(work, name + '.wav')
        s.write(wav, fn())
        subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1', ogg], check=True)
        db, peak, sec = decode_rms(ffmpeg, ogg, work, name)
        print('%-26s %.1f dBFS RMS, peak %.2f, %.2f s, %d bytes' % (name, db, peak, sec, os.path.getsize(ogg)))


if __name__ == '__main__':
    main()
