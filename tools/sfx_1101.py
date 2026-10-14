#!/usr/bin/env python3
"""
0.1.10.1: synthesized sound effects of quantum phase 2 (pure Python, helpers of sfx_185).

  sfx_suit_link_on          a link is made: two detuned tones glide into one unison, a glassy sparkle
  sfx_suit_link_off         a link is cut: the unison splits apart and fades, a small relay click
  sfx_suit_combo            a timing combo: a fast rising arpeggio, a bright stinger chord, a whoosh
  sfx_suit_resonance_burst  resonant deflection / phase counter wave: a struck metal bell (inharmonic
                            partials) over a deep thump and a short crack

Usage: sfx_1101.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_185 as s  # noqa: E402

rnd = random.Random(1101)
R = s.RATE


def sine(sec, f):
    return [math.sin(2 * math.pi * f * i / R) for i in range(s.n(sec))]


def link_on():
    y = s.zeros(0.9)
    # the two strands meet: 600 Hz and 1000 Hz glide to the same 784 Hz
    s.place(y, s.env(s.glide(0.35, 600, 784), 0.01, 0.6), 0.0, 0.25)
    s.place(y, s.env(s.glide(0.35, 1000, 784), 0.01, 0.6), 0.0, 0.25)
    # held unison with a slow shimmer
    held = [math.sin(2 * math.pi * 784 * i / R) * (0.75 + 0.25 * math.sin(2 * math.pi * 9 * i / R)) for i in range(s.n(0.45))]
    s.place(y, s.env(held, 0.02, 0.18), 0.33, 0.3)
    for k, f in enumerate((3136, 3951, 4699)):
        s.place(y, s.env(sine(0.3, f), 0.002, 0.07), 0.34 + k * 0.03, 0.12)
    s.place(y, s.env(s.bandpass(s.noise(0.02), 2000, 8000), 0.0005, 0.006), 0.33, 0.4)
    return s.normalize(s.fade_out(y, 0.06), 0.7)


def link_off():
    y = s.zeros(0.7)
    s.place(y, s.env(s.glide(0.45, 784, 520), 0.005, 0.18), 0.0, 0.3)
    s.place(y, s.env(s.glide(0.45, 784, 1180), 0.005, 0.14), 0.0, 0.2)
    s.place(y, s.env(s.bandpass(s.noise(0.01), 1500, 6000), 0.0003, 0.003), 0.0, 0.7)
    s.place(y, s.env(s.tone(0.05, 240, 110, curve=0.01), 0.0005, 0.01), 0.0, 0.35)
    return s.normalize(s.fade_out(y, 0.05), 0.6)


def combo():
    y = s.zeros(1.2)
    for k, f in enumerate((523, 659, 784, 1047, 1319)):
        note = [math.sin(2 * math.pi * f * i / R) + 0.35 * math.sin(4 * math.pi * f * i / R) for i in range(s.n(0.12))]
        s.place(y, s.env(note, 0.002, 0.04), k * 0.045, 0.3)
    whoosh = s.sweep_lowpass(s.noise(0.45), 500, 9000)
    whoosh = [v * math.sin(math.pi * i / len(whoosh)) for i, v in enumerate(whoosh)]
    s.place(y, whoosh, 0.05, 0.35)
    # stinger: a bright major chord with a slight detune
    for f in (1047, 1319, 1568, 2093):
        st = [math.sin(2 * math.pi * f * i / R) + 0.6 * math.sin(2 * math.pi * f * 1.004 * i / R) for i in range(s.n(0.8))]
        s.place(y, s.env(st, 0.004, 0.3), 0.24, 0.13)
    s.place(y, s.env(s.tone(0.3, 160, 60, curve=0.04), 0.001, 0.08), 0.24, 0.8)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.1), 1.2), 0.85)


def resonance_burst():
    y = s.zeros(1.4)
    # struck bell: inharmonic partials with longer tails on the low ones
    base = 210
    for ratio, gain, tau in ((1.0, 0.5, 0.55), (2.76, 0.32, 0.35), (5.40, 0.22, 0.2), (8.93, 0.14, 0.12), (13.3, 0.08, 0.07)):
        s.place(y, s.env(sine(1.3, base * ratio), 0.001, tau), 0.0, gain)
    s.place(y, s.env(s.tone(0.4, 120, 42, curve=0.05), 0.001, 0.12), 0.0, 1.0)
    crack = s.env(s.bandpass(s.noise(0.12), 600, 9000), 0.0005, 0.025)
    s.place(y, crack, 0.0, 0.9)
    # a soft ring-modulated shimmer on the tail (the energy that goes out)
    tail = [math.sin(2 * math.pi * 1680 * i / R) * math.sin(2 * math.pi * 23 * i / R) for i in range(s.n(0.8))]
    s.place(y, s.env(tail, 0.05, 0.25), 0.08, 0.12)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.12), 1.4), 0.9)


SOUNDS = {
    'sfx_suit_link_on': (link_on, 'suit.link_on', 8),
    'sfx_suit_link_off': (link_off, 'suit.link_off', 8),
    'sfx_suit_combo': (combo, 'suit.combo', 24),
    'sfx_suit_resonance_burst': (resonance_burst, 'suit.resonance_burst', 24),
}

if __name__ == '__main__':
    s.SOUNDS = SOUNDS
    s.main()
