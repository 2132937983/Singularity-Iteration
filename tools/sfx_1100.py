#!/usr/bin/env python3
"""
0.1.10.0: synthesized sound effects of the quantum actions (pure Python, helpers of sfx_185).

  sfx_suit_quantum_blink  quantum blink: a rising inhale, a crisp pop, a falling shimmer of bells
  sfx_suit_phase_on       phase shift on: a falling phaser sweep over a glassy fifth
  sfx_suit_phase_off      phase shift off: a rising phaser and a soft thump
  sfx_suit_field_on       force field up: a deep hum that rises, a crystal cluster and a whoosh
  sfx_suit_field_off      force field down: a power-down glide and crackle
  sfx_suit_field_catch    a projectile stops in the field: a dull thunk and a high zing
  sfx_suit_dilation_on    time dilation: a chord that slows down like a stopping tape, a deep boom, a slow tick
  sfx_suit_dilation_off   time dilation ends: the tape starts again (pitch rises back)
  sfx_suit_volley_order   entangled volley: three rising lock chirps over two beating tones
  sfx_suit_volley_impact  volley impact: a sharp crack and a bright shimmer
  sfx_suit_decoherence    decoherence: a bit-crushed falling tone with a stuttering gate
  sfx_suit_visor_boot     FCS visor boot: a rising sweep, three blips and a relay click

Usage: sfx_1100.py <ffmpeg> <sounds dir> <work dir>
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sfx_185 as s  # noqa: E402

rnd = random.Random(1100)
R = s.RATE


def flange(x, d0, d1, depth=1.0):
    """Adds a copy delayed by a delay that moves from d0 to d1 seconds (phaser/flanger sweep)."""
    out = []
    n = len(x)
    for i in range(n):
        d = d0 + (d1 - d0) * i / max(1, n - 1)
        j = i - int(d * R)
        out.append(x[i] + depth * (x[j] if j >= 0 else 0.0))
    return out


def tape(sec, freqs, f_from, f_to, curve=0.35):
    """A chord whose pitch glides by the factor f_from -> f_to (a tape slowing or starting)."""
    out = [0.0] * s.n(sec)
    for f in freqs:
        ph = 0.0
        for i in range(len(out)):
            u = i / len(out)
            k = f_from + (f_to - f_from) * (u ** curve if f_to < f_from else u ** (1 / curve))
            ph += 2 * math.pi * f * max(0.02, k) / R
            out[i] += math.sin(ph) + 0.3 * math.sin(2 * ph)
    return out


def blink():
    y = s.zeros(0.8)
    inhale = s.sweep_lowpass(s.noise(0.22), 400, 7000)
    inhale = [v * (i / len(inhale)) ** 2 for i, v in enumerate(inhale)]
    s.place(y, inhale, 0.0, 0.7)
    s.place(y, s.env(s.bandpass(s.noise(0.03), 1500, 9000), 0.0005, 0.008), 0.22, 1.2)
    s.place(y, s.env(s.tone(0.2, 220, 70, curve=0.04), 0.001, 0.05), 0.22, 0.9)
    for k, f in enumerate((2637, 3136, 3951, 4699)):
        bell = s.env(s.glide(0.5, f, f * 0.55), 0.002, 0.16 - k * 0.02)
        s.place(y, bell, 0.23 + k * 0.02, 0.18)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.06), 1.3), 0.85)


def phase_on():
    y = s.zeros(0.9)
    base = s.bandpass(s.noise(0.8), 300, 6000)
    base = [v * math.sin(math.pi * min(1, i / s.n(0.75))) for i, v in enumerate(base)]
    s.place(y, flange(base, 0.0004, 0.006, 0.9), 0.0, 0.45)
    for f in (660, 990):
        chord = [math.sin(2 * math.pi * f * i / R) * (0.6 + 0.4 * math.sin(2 * math.pi * 7 * i / R)) for i in range(s.n(0.7))]
        s.place(y, s.env(chord, 0.05, 0.25), 0.05, 0.22)
    return s.normalize(s.fade_out(y, 0.08), 0.75)


def phase_off():
    y = s.zeros(0.7)
    base = s.bandpass(s.noise(0.5), 400, 7000)
    base = [v * math.sin(math.pi * min(1, i / s.n(0.45))) for i, v in enumerate(base)]
    s.place(y, flange(base, 0.006, 0.0004, 0.9), 0.0, 0.45)
    s.place(y, s.env(s.tone(0.3, 120, 50, curve=0.05), 0.002, 0.08), 0.42, 0.9)
    s.place(y, s.env(s.bandpass(s.noise(0.05), 600, 4000), 0.001, 0.02), 0.42, 0.5)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.06), 1.3), 0.8)


def field_on():
    y = s.zeros(1.2)
    hum = [math.sin(2 * math.pi * (55 + 40 * min(1, i / s.n(0.5))) * i / R) + 0.5 * math.sin(2 * math.pi * 110.5 * i / R) for i in range(s.n(1.1))]
    hum = [v * min(1, i / s.n(0.25)) * math.exp(-max(0, i - s.n(0.4)) / s.n(0.35)) for i, v in enumerate(hum)]
    s.place(y, hum, 0.0, 0.6)
    whoosh = s.sweep_lowpass(s.noise(0.5), 600, 8000)
    whoosh = [v * math.sin(math.pi * i / len(whoosh)) for i, v in enumerate(whoosh)]
    s.place(y, whoosh, 0.05, 0.4)
    for k, f in enumerate((1568, 2093, 2637, 3136, 4186)):
        s.place(y, s.env([math.sin(2 * math.pi * f * i / R) for i in range(s.n(0.6))], 0.003, 0.2), 0.3 + k * 0.035, 0.16)
    return s.normalize(s.fade_out(y, 0.1), 0.85)


def field_off():
    y = s.zeros(0.8)
    glide = s.env(s.tone(0.7, 330, 45, curve=0.2), 0.005, 0.3)
    s.place(y, glide, 0.0, 0.6)
    crack = [0.0] * s.n(0.6)
    for i in range(len(crack)):
        if rnd.random() < 0.01 * math.exp(-i / s.n(0.25)):
            crack[i] = rnd.uniform(-1, 1)
    s.place(y, s.highpass(s.lowpass(crack, 8000), 1500), 0.02, 5.0)
    return s.normalize(s.fade_out(y, 0.08), 0.75)


def field_catch():
    y = s.zeros(0.4)
    s.place(y, s.env(s.tone(0.15, 180, 90, curve=0.03), 0.001, 0.04), 0.0, 0.9)
    s.place(y, s.env(s.glide(0.3, 5200, 3600), 0.001, 0.08), 0.005, 0.25)
    s.place(y, s.env(s.bandpass(s.noise(0.02), 2000, 9000), 0.0005, 0.006), 0.0, 0.6)
    return s.normalize(s.fade_out(y, 0.04), 0.7)


def dilation_on():
    y = s.zeros(1.6)
    slow = tape(1.2, (196, 247, 294, 392), 1.0, 0.18, curve=0.55)
    slow = [v * math.exp(-i / s.n(0.7)) for i, v in enumerate(slow)]
    s.place(y, slow, 0.0, 0.25)
    s.place(y, s.env(s.tone(1.0, 70, 32, curve=0.2), 0.004, 0.4), 0.05, 1.0)
    for k in range(3):
        tick = s.env(s.bandpass(s.noise(0.02), 2500, 6000), 0.0005, 0.006)
        s.place(y, tick, 0.55 + k * 0.32, 0.5)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.12), 1.2), 0.85)


def dilation_off():
    y = s.zeros(1.0)
    fast = tape(0.8, (196, 247, 294, 392), 0.18, 1.0, curve=0.55)
    fast = [v * min(1, i / s.n(0.4)) * math.exp(-max(0, i - s.n(0.6)) / s.n(0.12)) for i, v in enumerate(fast)]
    s.place(y, fast, 0.0, 0.25)
    s.place(y, s.env(s.bandpass(s.noise(0.06), 800, 5000), 0.001, 0.03), 0.68, 0.5)
    return s.normalize(s.fade_out(y, 0.08), 0.75)


def volley_order():
    y = s.zeros(0.8)
    beat = [math.sin(2 * math.pi * 440 * i / R) + math.sin(2 * math.pi * 446 * i / R) for i in range(s.n(0.7))]
    s.place(y, s.env(beat, 0.04, 0.3), 0.0, 0.15)
    for k, (f0, f1) in enumerate(((1200, 1600), (1500, 2000), (1900, 2600))):
        s.place(y, s.env(s.glide(0.06, f0, f1), 0.002, 0.025), 0.05 + k * 0.11, 0.4)
    s.place(y, s.env([math.sin(2 * math.pi * 3136 * i / R) for i in range(s.n(0.25))], 0.002, 0.08), 0.42, 0.25)
    return s.normalize(s.fade_out(y, 0.05), 0.7)


def volley_impact():
    y = s.zeros(0.7)
    s.place(y, s.env(s.bandpass(s.noise(0.25), 300, 9000), 0.0005, 0.05), 0.0, 1.2)
    s.place(y, s.env(s.tone(0.4, 140, 45, curve=0.05), 0.002, 0.1), 0.0, 1.0)
    for f in (3520, 4435, 5274):
        s.place(y, s.env([math.sin(2 * math.pi * f * i / R) for i in range(s.n(0.4))], 0.002, 0.1), 0.01, 0.12)
    return s.normalize(s.soft_clip(s.fade_out(y, 0.06), 1.6), 0.9)


def decoherence():
    y = s.zeros(0.9)
    tone = s.tone(0.8, 880, 110, curve=0.25, shape='square')
    crushed = []
    hold = 0.0
    for i, v in enumerate(tone):
        if i % 23 == 0:
            hold = round(v * 3) / 3
        crushed.append(hold)
    gate = [1.0 if (int(i / s.n(0.035)) * 7919) % 4 else 0.0 for i in range(len(crushed))]
    crushed = [v * g * math.exp(-i / s.n(0.4)) for i, (v, g) in enumerate(zip(crushed, gate))]
    s.place(y, s.lowpass(crushed, 5000), 0.0, 0.3)
    s.place(y, s.env(s.bandpass(s.noise(0.6), 1000, 8000), 0.005, 0.2), 0.0, 0.25)
    return s.normalize(s.fade_out(y, 0.06), 0.75)


def visor_boot():
    y = s.zeros(1.0)
    s.place(y, s.env(s.glide(0.35, 300, 1800), 0.05, 0.2), 0.0, 0.2)
    for k, f in enumerate((1760, 2093, 2637)):
        s.place(y, s.env([math.sin(2 * math.pi * f * i / R) for i in range(s.n(0.08))], 0.002, 0.02), 0.32 + k * 0.12, 0.35)
    s.place(y, s.env(s.bandpass(s.noise(0.01), 1500, 6000), 0.0003, 0.003), 0.75, 0.8)
    s.place(y, s.env(s.tone(0.05, 260, 120, curve=0.01), 0.0005, 0.01), 0.75, 0.4)
    return s.normalize(s.fade_out(y, 0.05), 0.6)


SOUNDS = {
    'sfx_suit_quantum_blink': (blink, 'suit.quantum_blink', 24),
    'sfx_suit_phase_on': (phase_on, 'suit.phase_on', 16),
    'sfx_suit_phase_off': (phase_off, 'suit.phase_off', 16),
    'sfx_suit_field_on': (field_on, 'suit.field_on', 24),
    'sfx_suit_field_off': (field_off, 'suit.field_off', 16),
    'sfx_suit_field_catch': (field_catch, 'suit.field_catch', 16),
    'sfx_suit_dilation_on': (dilation_on, 'suit.dilation_on', 32),
    'sfx_suit_dilation_off': (dilation_off, 'suit.dilation_off', 24),
    'sfx_suit_volley_order': (volley_order, 'suit.volley_order', 24),
    'sfx_suit_volley_impact': (volley_impact, 'suit.volley_impact', 32),
    'sfx_suit_decoherence': (decoherence, 'suit.decoherence', 16),
    'sfx_suit_visor_boot': (visor_boot, 'suit.visor_boot', 8),
}

if __name__ == '__main__':
    s.SOUNDS = SOUNDS
    s.main()
