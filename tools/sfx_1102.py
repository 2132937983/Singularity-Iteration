#!/usr/bin/env python3
"""
0.1.10.2: synthesized sound effects of the quantum command, the holo table and the propulsion
(pure Python, helpers of sfx_185 and sfx_1100).

  sfx_command_wheel_open      the wheel unfolds: a fast rising shimmer over a soft fifth
  sfx_command_wheel_select    a sector lights: a tiny glassy blip
  sfx_command_order_ack       a Wingman acknowledges: a squelch tick and a two-tone radio chirp
  sfx_command_quantum_recall  the suits fold away: a falling phased chord, then a rising streak and a ping
  sfx_command_holo_open       the holo table projects: a spinning-up hum, a scan buzz and a cascade of sparkles
  sfx_command_holo_close      the holo table folds: a falling hum and a soft click
  sfx_suit_thruster_land      a suit lands from maneuver flight: a low thump, a blast of air, a little grit

Usage: sfx_1102.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_185 as s  # noqa: E402
from sfx_1100 import flange  # noqa: E402

rnd = random.Random(1102)
R = s.RATE


def sine(sec, f):
    return [math.sin(2 * math.pi * f * i / R) for i in range(s.n(sec))]


def wheel_open():
    y = s.zeros(0.45)
    sweep = s.sweep_lowpass(s.noise(0.25), 800, 9000)
    sweep = [v * math.sin(math.pi * i / len(sweep)) for i, v in enumerate(sweep)]
    s.place(y, sweep, 0.0, 0.35)
    s.place(y, s.env(s.glide(0.18, 900, 1760), 0.01, 0.08), 0.0, 0.25)
    for f in (1318, 1976):
        s.place(y, s.env(sine(0.3, f), 0.02, 0.12), 0.08, 0.15)
    return s.normalize(s.fade_out(y, 0.05), 0.55)


def wheel_select():
    y = s.zeros(0.09)
    s.place(y, s.env(sine(0.08, 2489), 0.001, 0.018), 0.0, 0.5)
    s.place(y, s.env(s.bandpass(s.noise(0.006), 3000, 9000), 0.0002, 0.002), 0.0, 0.5)
    return s.normalize(s.fade_out(y, 0.02), 0.45)


def order_ack():
    y = s.zeros(0.42)
    # squelch: a short band-limited noise tick at both ends of the transmission
    s.place(y, s.env(s.bandpass(s.noise(0.03), 800, 3500), 0.001, 0.01), 0.0, 0.5)
    s.place(y, s.env(s.bandpass(s.noise(0.03), 800, 3500), 0.001, 0.01), 0.33, 0.35)
    # two-tone chirp, slightly gritty like a radio
    for k, (f0, f1) in enumerate(((1400, 1500), (1800, 1900))):
        tone = s.glide(0.09, f0, f1)
        tone = [round(v * 6) / 6 for v in tone]
        s.place(y, s.env(tone, 0.003, 0.05), 0.04 + k * 0.12, 0.35)
    return s.normalize(s.lowpass(s.fade_out(y, 0.04), 6000), 0.6)


def quantum_recall():
    y = s.zeros(1.5)
    chord = [0.0] * s.n(0.8)
    for f in (523, 659, 784):
        g = s.glide(0.8, f, f * 0.6)
        for i in range(len(chord)):
            chord[i] += g[i]
    chord = [v * math.exp(-i / s.n(0.45)) for i, v in enumerate(chord)]
    s.place(y, flange(chord, 0.0005, 0.007, 0.9), 0.0, 0.22)
    streak = s.sweep_lowpass(s.noise(0.55), 400, 11000)
    streak = [v * (i / len(streak)) ** 1.5 * math.exp(-max(0, i - s.n(0.45)) / s.n(0.05)) for i, v in enumerate(streak)]
    s.place(y, streak, 0.45, 0.7)
    s.place(y, s.env(s.glide(0.5, 300, 2400), 0.02, 0.2), 0.45, 0.2)
    s.place(y, s.env(sine(0.6, 3136), 0.002, 0.2), 1.0, 0.25)
    s.place(y, s.env(sine(0.6, 4699), 0.002, 0.15), 1.02, 0.12)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.12), 1.2), 0.8)


def holo_open():
    y = s.zeros(1.1)
    hum = []
    ph = 0.0
    for i in range(s.n(1.0)):
        u = i / s.n(1.0)
        f = 60 + 70 * min(1, u * 1.6)
        ph += 2 * math.pi * f / R
        hum.append((math.sin(ph) + 0.5 * math.sin(2 * ph) + 0.25 * math.sin(3 * ph)) * min(1, u * 4) * math.exp(-max(0, u - 0.5) * 3))
    s.place(y, hum, 0.0, 0.45)
    buzz = [math.sin(2 * math.pi * 120 * i / R) * (1 if (i // 220) % 2 else 0.3) for i in range(s.n(0.4))]
    s.place(y, s.env(s.bandpass(buzz, 200, 4000), 0.02, 0.15), 0.25, 0.25)
    for k in range(9):
        f = 2093 * (2 ** (rnd.choice((0, 3, 7, 10, 12)) / 12))
        s.place(y, s.env(sine(0.25, f), 0.002, 0.07), 0.35 + k * 0.045, 0.1)
    return s.normalize(s.fade_out(y, 0.1), 0.7)


def holo_close():
    y = s.zeros(0.65)
    hum = []
    ph = 0.0
    for i in range(s.n(0.55)):
        u = i / s.n(0.55)
        ph += 2 * math.pi * (130 - 80 * u) / R
        hum.append((math.sin(ph) + 0.5 * math.sin(2 * ph)) * (1 - u))
    s.place(y, hum, 0.0, 0.4)
    s.place(y, s.env(s.glide(0.25, 1760, 700), 0.005, 0.08), 0.0, 0.18)
    s.place(y, s.env(s.bandpass(s.noise(0.01), 1500, 6000), 0.0003, 0.003), 0.5, 0.6)
    return s.normalize(s.fade_out(y, 0.05), 0.6)


def thruster_land():
    y = s.zeros(0.7)
    s.place(y, s.env(s.tone(0.3, 95, 40, curve=0.05), 0.002, 0.09), 0.0, 1.0)
    air = s.lowpass(s.noise(0.5), 2500)
    air = [v * math.exp(-i / s.n(0.12)) for i, v in enumerate(air)]
    s.place(y, air, 0.0, 0.8)
    grit = [0.0] * s.n(0.4)
    for i in range(len(grit)):
        if rnd.random() < 0.006 * math.exp(-i / s.n(0.15)):
            grit[i] = rnd.uniform(-1, 1)
    s.place(y, s.highpass(grit, 1800), 0.02, 3.0)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.08), 1.3), 0.8)


SOUNDS = {
    'sfx_command_wheel_open': (wheel_open, 'command.wheel_open', 8),
    'sfx_command_wheel_select': (wheel_select, 'command.wheel_select', 8),
    'sfx_command_order_ack': (order_ack, 'command.order_ack', 8),
    'sfx_command_quantum_recall': (quantum_recall, 'command.quantum_recall', 32),
    'sfx_command_holo_open': (holo_open, 'command.holo_open', 8),
    'sfx_command_holo_close': (holo_close, 'command.holo_close', 8),
    'sfx_suit_thruster_land': (thruster_land, 'suit.thruster_land', 16),
}

if __name__ == '__main__':
    s.SOUNDS = SOUNDS
    s.main()
