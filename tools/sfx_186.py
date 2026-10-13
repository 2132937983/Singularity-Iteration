#!/usr/bin/env python3
"""
0.1.8.6: synthesized sound effects of the nano suit phase 2 (pure Python, helpers of sfx_185).

  sfx_nano_mesh_on   kinetic armor mesh: plates lock in (metal clicks rising), a resonant hum
  sfx_nano_mesh_hit  a hit on the mesh: bright deflection ping over a dull thud
  sfx_nano_recon     recon cloud: a hissing swarm sweep with a sonar ping
  sfx_nano_rebound   shock gel rebound: soft gel squash and a springy upward boing
  sfx_nano_repair    self-repair: two soft rising chimes

Usage: sfx_186.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_185 as s  # noqa: E402


def mesh_on():
    y = s.zeros(0.9)
    for i in range(6):                                   # plates locking in, faster and higher
        at = 0.02 + i * 0.045
        click = s.env(s.bandpass(s.noise(0.03), 1800 + 300 * i, 7000), 0.0005, 0.006)
        s.place(y, click, at, 0.9)
        ring = s.env([math.sin(2 * math.pi * (900 + 160 * i) * k / s.RATE) for k in range(s.n(0.08))], 0.001, 0.025)
        s.place(y, ring, at, 0.25)
    hum = [math.sin(2 * math.pi * 110 * k / s.RATE) + 0.5 * math.sin(2 * math.pi * 220 * k / s.RATE) for k in range(s.n(0.6))]
    hum = [v * min(1, k / s.n(0.05)) * math.exp(-k / s.n(0.3)) for k, v in enumerate(hum)]
    s.place(y, hum, 0.28, 0.35)
    return s.normalize(s.fade_out(y, 0.05), 0.85)


def mesh_hit():
    y = s.zeros(0.4)
    s.place(y, s.env(s.lowpass(s.noise(0.12), 600), 0.001, 0.03), 0.0, 1.2)
    ping = [math.sin(2 * math.pi * 2350 * k / s.RATE) + 0.5 * math.sin(2 * math.pi * 3520 * k / s.RATE) for k in range(s.n(0.35))]
    s.place(y, s.env(ping, 0.001, 0.07), 0.003, 0.5)
    return s.normalize(s.fade_out(y, 0.03), 0.8)


def recon():
    y = s.zeros(1.1)
    sweep = s.sweep_lowpass(s.noise(0.8), 600, 9000)
    sweep = [v * math.sin(math.pi * k / len(sweep)) for k, v in enumerate(sweep)]
    s.place(y, s.highpass(sweep, 400), 0.0, 0.5)
    for at, f in ((0.15, 1760), (0.55, 1760)):          # sonar pings
        p = s.env([math.sin(2 * math.pi * f * k / s.RATE) for k in range(s.n(0.4))], 0.002, 0.12)
        s.place(y, p, at, 0.4 if at < 0.3 else 0.2)
    return s.normalize(s.fade_out(y, 0.08), 0.78)


def rebound():
    y = s.zeros(0.5)
    s.place(y, s.env(s.lowpass(s.noise(0.08), 400), 0.002, 0.03), 0.0, 1.0)
    boing = s.env(s.glide(0.35, 180, 620), 0.004, 0.12)
    boing = [v * (1 + 0.25 * math.sin(2 * math.pi * 22 * k / s.RATE)) for k, v in enumerate(boing)]
    s.place(y, boing, 0.03, 0.6)
    return s.normalize(s.fade_out(y, 0.04), 0.8)


def repair():
    y = s.zeros(0.7)
    for at, f in ((0.0, 1318), (0.12, 1760)):
        c = [math.sin(2 * math.pi * f * k / s.RATE) + 0.3 * math.sin(2 * math.pi * 2 * f * k / s.RATE) for k in range(s.n(0.5))]
        s.place(y, s.env(c, 0.004, 0.15), at, 0.35)
    return s.normalize(s.fade_out(y, 0.05), 0.6)


SOUNDS = {
    'sfx_nano_mesh_on': (mesh_on, 'nano.mesh_on', 16),
    'sfx_nano_mesh_hit': (mesh_hit, 'nano.mesh_hit', 16),
    'sfx_nano_recon': (recon, 'nano.recon', 16),
    'sfx_nano_rebound': (rebound, 'nano.rebound', 16),
    'sfx_nano_repair': (repair, 'nano.repair', 8),
}

if __name__ == '__main__':
    s.SOUNDS = SOUNDS
    s.main()
