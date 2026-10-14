#!/usr/bin/env python3
"""
0.1.9.1: synthesized sound effects (pure Python, helpers of sfx_185).

  sfx_wingman_boost        Wingman evasive boost: a sharp ignition pop and a short jet whoosh
  sfx_wingman_lock         Wingman target lock: two rising chirps over a soft scan sweep
  sfx_suit_photon_barrier  deflector photon barrier: a crystalline shimmer of bell partials,
                           a glassy noise sparkle and a low soft thump

Usage: sfx_191.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_185 as s  # noqa: E402


def boost():
    y = s.zeros(0.7)
    s.place(y, s.env(s.bandpass(s.noise(0.04), 800, 6000), 0.0005, 0.01), 0.0, 1.0)
    whoosh = s.sweep_lowpass(s.noise(0.55), 5000, 700)
    whoosh = [v * math.sin(math.pi * min(1.0, k / (len(whoosh) * 0.35))) * math.exp(-k / s.n(0.22)) for k, v in enumerate(whoosh)]
    s.place(y, s.highpass(whoosh, 150), 0.01, 0.9)
    rumble = s.env(s.glide(0.4, 120, 70), 0.004, 0.12)
    s.place(y, rumble, 0.0, 0.35)
    return s.normalize(s.fade_out(y, 0.05), 0.8)


def lock():
    y = s.zeros(0.5)
    sweep = s.env(s.bandpass(s.noise(0.3), 2500, 6500), 0.02, 0.1)
    s.place(y, sweep, 0.0, 0.18)
    for at, f0, f1 in ((0.02, 1400, 1900), (0.11, 1900, 2600)):
        chirp = s.env(s.glide(0.07, f0, f1), 0.002, 0.03)
        s.place(y, chirp, at, 0.45)
    ok = [math.sin(2 * math.pi * 2637 * k / s.RATE) for k in range(s.n(0.18))]
    s.place(y, s.env(ok, 0.002, 0.05), 0.2, 0.22)
    return s.normalize(s.fade_out(y, 0.04), 0.6)


def photon_barrier():
    rnd = random.Random(191)
    y = s.zeros(1.0)
    s.place(y, s.env(s.lowpass(s.noise(0.12), 300), 0.002, 0.05), 0.0, 0.5)
    for f, g, tau in ((1568, 0.35, 0.35), (2349, 0.3, 0.28), (3136, 0.25, 0.22), (4186, 0.2, 0.16), (5274, 0.12, 0.12)):
        bell = [math.sin(2 * math.pi * f * k / s.RATE + 0.3 * math.sin(2 * math.pi * 5 * k / s.RATE)) for k in range(s.n(0.9))]
        s.place(y, s.env(bell, 0.002, tau), 0.004, g)
    for i in range(18):
        at = rnd.random() * 0.45
        sparkle = s.env(s.bandpass(s.noise(0.015), 6000, 12000), 0.0003, 0.004)
        s.place(y, sparkle, at, 0.25 * (1 - at))
    return s.normalize(s.fade_out(y, 0.08), 0.7)


SOUNDS = {
    'sfx_wingman_boost': (boost, 'wingman.boost', 20),
    'sfx_wingman_lock': (lock, 'wingman.lock', 12),
    'sfx_suit_photon_barrier': (photon_barrier, 'suit.photon_barrier', 16),
}

if __name__ == '__main__':
    s.SOUNDS = SOUNDS
    s.main()
