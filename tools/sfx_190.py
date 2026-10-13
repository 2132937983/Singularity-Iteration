#!/usr/bin/env python3
"""
0.1.9.0: synthesized sound effects of the frame cores (pure Python, helpers of sfx_185).

  sfx_core_discharge  Advanced Propulsion arc discharge: a hard crack, a buzzing arc crackle, a deep
                      shock wave thump and a tail of fading sparks
  sfx_core_install    a core cartridge seats in its socket: slide, heavy latch, power-up chime

Usage: sfx_190.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_185 as s  # noqa: E402


def discharge():
    rnd = random.Random(190)
    y = s.zeros(1.5)
    # the crack: a very short broadband burst
    s.place(y, s.env(s.highpass(s.noise(0.05), 1200), 0.0003, 0.008), 0.0, 1.4)
    # arc buzz: noise gated at the mains-like rate with random spikes
    buzz = s.bandpass(s.noise(0.7), 900, 6500)
    gate = []
    for k in range(len(buzz)):
        t = k / s.RATE
        g = 0.55 + 0.45 * math.sin(2 * math.pi * 97 * t) ** 2
        if rnd.random() < 0.0009:
            g += 2.5
        gate.append(g * math.exp(-t / 0.28))
    s.place(y, [v * g for v, g in zip(buzz, gate)], 0.01, 0.75)
    # shock wave thump: a falling low tone
    thump = s.env(s.glide(0.6, 150, 38), 0.002, 0.18)
    s.place(y, s.soft_clip(thump, 1.6), 0.015, 1.0)
    # spark tail
    for i in range(14):
        at = 0.25 + rnd.random() * 0.9
        spark = s.env(s.bandpass(s.noise(0.02), 2500 + rnd.random() * 3000, 9000), 0.0004, 0.004)
        s.place(y, spark, at, 0.5 * (1.2 - at / 1.2))
    return s.normalize(s.fade_out(y, 0.1), 0.92)


def install():
    y = s.zeros(1.1)
    slide = s.sweep_lowpass(s.noise(0.22), 500, 3500)
    slide = [v * math.sin(math.pi * k / len(slide)) for k, v in enumerate(slide)]
    s.place(y, s.highpass(slide, 250), 0.0, 0.35)
    # heavy latch: a low clunk and a metal click
    s.place(y, s.env(s.lowpass(s.noise(0.1), 380), 0.001, 0.035), 0.22, 1.2)
    s.place(y, s.env(s.bandpass(s.noise(0.03), 2200, 7000), 0.0004, 0.006), 0.225, 0.8)
    ring = [math.sin(2 * math.pi * 640 * k / s.RATE) + 0.4 * math.sin(2 * math.pi * 1720 * k / s.RATE) for k in range(s.n(0.3))]
    s.place(y, s.env(ring, 0.001, 0.05), 0.225, 0.25)
    # power-up: rising hum and two chime notes
    hum = s.glide(0.5, 110, 220)
    hum = [v * min(1, k / s.n(0.2)) * math.exp(-k / s.n(0.35)) for k, v in enumerate(hum)]
    s.place(y, hum, 0.32, 0.35)
    for at, f in ((0.48, 1046), (0.6, 1568)):
        c = [math.sin(2 * math.pi * f * k / s.RATE) + 0.3 * math.sin(2 * math.pi * 2 * f * k / s.RATE) for k in range(s.n(0.45))]
        s.place(y, s.env(c, 0.003, 0.12), at, 0.3)
    return s.normalize(s.fade_out(y, 0.06), 0.8)


SOUNDS = {
    'sfx_core_discharge': (discharge, 'core.discharge', 24),
    'sfx_core_install': (install, 'core.install', 12),
}

if __name__ == '__main__':
    s.SOUNDS = SOUNDS
    s.main()
