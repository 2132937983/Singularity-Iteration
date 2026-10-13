#!/usr/bin/env python3
"""
0.1.8.5: synthesized sound effects of the nano suit units (no samples, pure Python).

  sfx_nano_dash      Kinetic Dash: air-cut whoosh with a kinetic thump and a falling zip
  sfx_nano_emp       EMP: short charge whine, sub boom, falling discharge sweep, crackle, hiss tail
  sfx_nano_camo_on   optical camouflage on: rising detuned shimmer, glassy ping
  sfx_nano_camo_off  optical camouflage off: stuttering falling glitch chord, digital crackle
  sfx_nano_overheat  overheat: two-tone alarm and a steam vent
  sfx_nano_denied    refused burst: short low buzzer double pulse
  sfx_nano_hf_sync   HF sync: rising high-frequency whine settling on a ringing tone

Usage: sfx_185.py <ffmpeg> <sounds dir> <work dir>
Also adds the sound events to sounds.json (next to the sounds dir) if they are missing.
"""
import array
import json
import math
import os
import random
import subprocess
import sys
import wave

RATE = 44100
rnd = random.Random(1850)


def n(sec):
    return int(sec * RATE)


def zeros(sec):
    return [0.0] * n(sec)


def place(buf, src, at, gain=1.0):
    i0 = int(at * RATE)
    for i, v in enumerate(src):
        j = i0 + i
        if 0 <= j < len(buf):
            buf[j] += v * gain


def noise(sec):
    return [rnd.uniform(-1, 1) for _ in range(n(sec))]


def lowpass(x, cutoff):
    a = 1 - math.exp(-2 * math.pi * cutoff / RATE)
    y, s = [], 0.0
    for v in x:
        s += a * (v - s)
        y.append(s)
    return y


def highpass(x, cutoff):
    lp = lowpass(x, cutoff)
    return [a - b for a, b in zip(x, lp)]


def bandpass(x, lo, hi):
    return lowpass(highpass(x, lo), hi)


def sweep_lowpass(x, f0, f1):
    """Low-pass whose cutoff moves exponentially from f0 to f1 over the buffer."""
    y, s = [], 0.0
    N = len(x)
    for i, v in enumerate(x):
        f = f0 * (f1 / f0) ** (i / max(1, N - 1))
        a = 1 - math.exp(-2 * math.pi * f / RATE)
        s += a * (v - s)
        y.append(s)
    return y


def tone(sec, f0, f1=None, shape='sin', curve=0.06):
    """Sine (or other) with frequency gliding exponentially from f0 to f1 (time constant curve)."""
    f1 = f0 if f1 is None else f1
    out, ph = [], 0.0
    for i in range(n(sec)):
        t = i / RATE
        f = f1 + (f0 - f1) * math.exp(-t / curve) if curve > 0 else f0
        ph += 2 * math.pi * f / RATE
        if shape == 'sin':
            v = math.sin(ph)
        elif shape == 'square':
            v = 1.0 if math.sin(ph) >= 0 else -1.0
        else:  # saw
            v = ((ph / (2 * math.pi)) % 1.0) * 2 - 1
        out.append(v)
    return out


def glide(sec, f0, f1):
    """Linear-in-log glide from f0 to f1 over the whole length."""
    out, ph = [], 0.0
    N = n(sec)
    for i in range(N):
        f = f0 * (f1 / f0) ** (i / max(1, N - 1))
        ph += 2 * math.pi * f / RATE
        out.append(math.sin(ph))
    return out


def env(x, attack, decay_tau, hold=0.0):
    out = []
    for i, v in enumerate(x):
        t = i / RATE
        if t < attack:
            g = t / attack
        elif t < attack + hold:
            g = 1.0
        else:
            g = math.exp(-(t - attack - hold) / decay_tau)
        out.append(v * g)
    return out


def fade_out(x, sec):
    m = n(sec)
    N = len(x)
    return [v * (min(1.0, (N - i) / m) if N - i < m else 1.0) for i, v in enumerate(x)]


def mul(x, g):
    return [v * g for v in x]


def soft_clip(x, drive):
    return [math.tanh(v * drive) / math.tanh(drive) for v in x]


def normalize(x, peak):
    m = max(1e-9, max(abs(v) for v in x))
    return [v * peak / m for v in x]


# ---------------------------------------------------------------- sounds

def dash():
    sec = 0.55
    y = zeros(sec)
    # air cut: noise through a falling low-pass, swelling then dying
    w = sweep_lowpass(noise(0.5), 9000, 600)
    w = [v * math.sin(math.pi * min(1, i / n(0.5))) ** 0.6 * math.exp(-i / n(0.22)) for i, v in enumerate(w)]
    place(y, w, 0.0, 1.6)
    # kinetic thump
    place(y, env(tone(0.18, 140, 45, curve=0.03), 0.002, 0.05), 0.0, 1.1)
    # falling zip (myofiber release)
    place(y, env(glide(0.22, 2600, 380), 0.003, 0.07), 0.01, 0.35)
    # tiny electric tick at the start
    place(y, env(highpass(noise(0.02), 3000), 0.0005, 0.004), 0.0, 0.8)
    return normalize(soft_clip(fade_out(y, 0.05), 1.4), 0.88)


def emp():
    sec = 1.6
    y = zeros(sec)
    # charge whine (rising)
    place(y, [v * (i / n(0.14)) for i, v in enumerate(glide(0.14, 600, 4200))], 0.0, 0.25)
    t0 = 0.14
    # sub boom
    place(y, env(tone(1.0, 90, 38, curve=0.12), 0.003, 0.32), t0, 1.4)
    # discharge sweep (the "pulse")
    sw = env(tone(0.6, 2400, 70, curve=0.07), 0.001, 0.16)
    place(y, sw, t0, 0.55)
    # broadband crack
    place(y, env(bandpass(noise(0.3), 800, 9000), 0.0005, 0.03), t0, 1.2)
    # crackle: sparse impulses through a high-pass, fading
    crack = [0.0] * n(1.2)
    for i in range(len(crack)):
        if rnd.random() < 0.012 * math.exp(-i / n(0.45)):
            crack[i] = rnd.uniform(-1, 1)
    crack = highpass(lowpass(crack, 7000), 1500)
    place(y, crack, t0 + 0.02, 6.0)
    # hiss tail
    hiss = bandpass(noise(1.3), 2500, 9000)
    place(y, env(hiss, 0.02, 0.35), t0 + 0.05, 0.22)
    return normalize(soft_clip(fade_out(y, 0.1), 1.8), 0.9)


def camo_on():
    sec = 0.8
    y = zeros(sec)
    # detuned rising shimmer with tremolo
    for k, f in enumerate((520, 780, 1170, 1560)):
        g = glide(0.62, f * 0.6, f * 1.35)
        g = [v * (0.5 + 0.5 * math.sin(2 * math.pi * (13 + k * 3) * i / RATE)) * math.sin(math.pi * i / len(g)) for i, v in enumerate(g)]
        place(y, g, 0.0, 0.22)
    # soft fizz
    fz = bandpass(noise(0.55), 3000, 11000)
    place(y, [v * math.sin(math.pi * i / len(fz)) for i, v in enumerate(fz)], 0.02, 0.18)
    # glassy ping at the end
    ping = env([math.sin(2 * math.pi * 2637 * i / RATE) + 0.4 * math.sin(2 * math.pi * 3951 * i / RATE) for i in range(n(0.3))], 0.002, 0.08)
    place(y, ping, 0.48, 0.45)
    return normalize(fade_out(y, 0.06), 0.8)


def camo_off():
    sec = 0.6
    y = zeros(sec)
    chord = [0.0] * n(0.5)
    for f in (1400, 1050, 700):
        g = glide(0.5, f, f * 0.45)
        chord = [a + b for a, b in zip(chord, g)]
    # stutter gate (glitch)
    gate = []
    for i in range(len(chord)):
        slot = int(i / n(0.028))
        gate.append(1.0 if (slot * 7919) % 5 != 0 else 0.0)
    chord = [v * gt * math.exp(-i / n(0.22)) for i, (v, gt) in enumerate(zip(chord, gate))]
    place(y, chord, 0.0, 0.3)
    # digital crackle (sample-and-hold noise)
    sh, cur = [], 0.0
    for i in range(n(0.35)):
        if i % 37 == 0:
            cur = rnd.uniform(-1, 1)
        sh.append(cur * math.exp(-i / n(0.1)))
    place(y, highpass(sh, 900), 0.0, 0.35)
    return normalize(fade_out(y, 0.05), 0.78)


def overheat():
    sec = 1.1
    y = zeros(sec)
    for k, (f, at) in enumerate(((880, 0.0), (660, 0.16), (880, 0.32), (660, 0.48))):
        b = tone(0.13, f, shape='square')
        b = lowpass(b, 3500)
        place(y, env(b, 0.004, 0.05, hold=0.06), at, 0.28)
    steam = bandpass(noise(0.9), 1500, 8000)
    steam = [v * min(1, i / n(0.05)) * math.exp(-i / n(0.35)) for i, v in enumerate(steam)]
    place(y, steam, 0.12, 0.5)
    return normalize(fade_out(y, 0.08), 0.8)


def denied():
    sec = 0.22
    y = zeros(sec)
    for at in (0.0, 0.1):
        b = lowpass(tone(0.08, 175, shape='square'), 1800)
        place(y, env(b, 0.002, 0.03, hold=0.03), at, 0.5)
    return normalize(fade_out(y, 0.02), 0.7)


def hf_sync():
    sec = 0.6
    y = zeros(sec)
    w = glide(0.3, 900, 3200)
    w = [v * (i / len(w)) for i, v in enumerate(w)]
    place(y, w, 0.0, 0.25)
    ring = [math.sin(2 * math.pi * 3200 * i / RATE) * (0.6 + 0.4 * math.sin(2 * math.pi * 47 * i / RATE)) for i in range(n(0.3))]
    place(y, env(ring, 0.004, 0.12), 0.28, 0.3)
    place(y, env(highpass(noise(0.015), 2500), 0.0005, 0.004), 0.28, 0.6)
    return normalize(fade_out(y, 0.05), 0.65)


SOUNDS = {
    'sfx_nano_dash': (dash, 'nano.dash', 24),
    'sfx_nano_emp': (emp, 'nano.emp', 32),
    'sfx_nano_camo_on': (camo_on, 'nano.camo_on', 16),
    'sfx_nano_camo_off': (camo_off, 'nano.camo_off', 16),
    'sfx_nano_overheat': (overheat, 'nano.overheat', 16),
    'sfx_nano_denied': (denied, 'nano.denied', 8),
    'sfx_nano_hf_sync': (hf_sync, 'nano.hf_sync', 12),
}


def write_wav(path, x):
    a = array.array('h', (max(-32767, min(32767, int(v * 32767))) for v in x))
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(a.tobytes())


def main():
    ffmpeg, sounds_dir, work = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(work, exist_ok=True)
    for name, (fn, event, dist) in SOUNDS.items():
        wav = os.path.join(work, name + '.wav')
        write_wav(wav, fn())
        out = os.path.join(sounds_dir, name + '.ogg')
        subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', out], check=True)
        print('wrote', out, os.path.getsize(out))
    sj = os.path.join(os.path.dirname(sounds_dir.rstrip('/\\')), 'sounds.json')
    with open(sj, encoding='utf-8-sig') as f:
        data = json.load(f)
    added = 0
    for name, (fn, event, dist) in SOUNDS.items():
        if event in data:
            continue
        data[event] = {'subtitle': 'mio_icif.subtitle.' + event, 'sounds': [{'name': 'mio_icif:' + name, 'attenuation_distance': dist}]}
        added += 1
    with open(sj, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('sounds.json: added', added)


if __name__ == '__main__':
    main()
