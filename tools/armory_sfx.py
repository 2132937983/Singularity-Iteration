#!/usr/bin/env python3
"""
Original synthesized sound effects for the Armory suit-up sequence.

Everything is generated from noise and oscillators (no samples), written as 16-bit mono
WAV and encoded to Ogg Vorbis with ffmpeg. Usage: armory_sfx.py <sounds-dir>
"""
import os, subprocess, sys, tempfile, wave
import numpy as np
from scipy import signal

SR = 44100
rng = np.random.default_rng(20261002)


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def noise(sec):
    return rng.standard_normal(int(SR * sec))


def bandpass(x, lo, hi, order=4):
    sos = signal.butter(order, [lo, hi], btype='band', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def lowpass(x, f, order=4):
    return signal.sosfilt(signal.butter(order, f, btype='low', fs=SR, output='sos'), x)


def highpass(x, f, order=4):
    return signal.sosfilt(signal.butter(order, f, btype='high', fs=SR, output='sos'), x)


def sweep_filter(x, f0, f1, q=4.0, steps=48):
    """Time-varying band-pass: centre frequency glides f0 -> f1 (log)."""
    out = np.zeros_like(x)
    n = len(x)
    seg = n // steps + 1
    win = np.hanning(2 * seg)
    for i in range(steps):
        a = max(0, i * seg - seg // 2)
        b = min(n, a + 2 * seg)
        f = f0 * (f1 / f0) ** (i / max(1, steps - 1))
        bw = f / q
        y = bandpass(x[a:b], max(20, f - bw), min(SR / 2 - 100, f + bw), 2)
        out[a:b] += y * win[:b - a]
    return out


def env_adsr(n, a, d, s, r, sustain=0.6):
    e = np.ones(n) * sustain
    A, D, R = int(a * SR), int(d * SR), int(r * SR)
    e[:A] = np.linspace(0, 1, A, endpoint=False)
    e[A:A + D] = np.linspace(1, sustain, D, endpoint=False)
    if R > 0:
        e[-R:] *= np.linspace(1, 0, R)
    return e


def exp_decay(n, tau):
    return np.exp(-np.arange(n) / (SR * tau))


def metal(sec, partials, tau):
    """Inharmonic struck-metal resonance."""
    t = t_axis(sec)
    y = np.zeros_like(t)
    for f, amp, k in partials:
        y += amp * np.sin(2 * np.pi * f * t) * np.exp(-t / (tau * k))
    # 2 ms attack and 20 ms release so a resonance never starts or stops with a click
    a, r = min(len(y), int(0.002 * SR)), min(len(y), int(0.02 * SR))
    y[:a] *= np.linspace(0, 1, a)
    y[-r:] *= np.linspace(1, 0, r)
    return y


def click(sec=0.01, f=3500):
    n = noise(sec)
    return highpass(n, f) * exp_decay(len(n), 0.002)


def place(dst, src, at):
    i = int(at * SR)
    j = min(len(dst), i + len(src))
    dst[i:j] += src[:j - i]
    return dst


def normalize(x, peak=0.89):
    m = np.max(np.abs(x)) or 1
    return x * (peak / m)


def soft_clip(x, drive=1.4):
    return np.tanh(x * drive) / np.tanh(drive)


# ---------------------------------------------------------------------------- sounds
def jet_loop():
    """Seamless 2 s thruster: rumble + roar + whine. Loops by crossfading its ends."""
    sec, fade = 2.0, 0.25
    n = noise(sec + fade)
    t = t_axis(sec + fade)
    rumble = lowpass(n, 140) * 2.8
    roar = bandpass(n, 500, 2600) * (0.9 + 0.1 * np.sin(2 * np.pi * 7 * t))
    hiss = highpass(n, 5000) * 0.25
    whine = 0.12 * np.sin(2 * np.pi * (1850 + 25 * np.sin(2 * np.pi * 0.5 * t)) * t)
    x = rumble + roar + hiss + whine
    F = int(fade * SR)
    body = x[:int(sec * SR)].copy()
    tail = x[int(sec * SR):]
    w = np.linspace(0, 1, F)
    body[:F] = body[:F] * w + tail * (1 - w)
    return normalize(soft_clip(normalize(body)), 0.8)


def launch():
    """Ignition: thump, then a roar that sweeps up and away."""
    sec = 1.3
    n = noise(sec)
    t = t_axis(sec)
    thump = lowpass(noise(0.25), 120) * exp_decay(int(0.25 * SR), 0.06) * 5
    roar = sweep_filter(n, 300, 2400, q=2.5) * env_adsr(len(n), 0.04, 0.3, 0.55, 0.6)
    crackle = highpass(n, 3000) * (rng.random(len(n)) > 0.985) * 3 * exp_decay(len(n), 0.5)
    x = roar * 1.4 + crackle
    place(x, thump, 0)
    return normalize(soft_clip(normalize(x)))


def flyby():
    """Incoming piece: air tear rising in pitch and loudness, peaking at the end (doppler)."""
    sec = 1.6
    n = noise(sec)
    t = t_axis(sec)
    sweep = sweep_filter(n, 250, 3200, q=3.0)
    env = (t / sec) ** 2.2
    env[-int(0.18 * SR):] *= np.linspace(1, 0.0, int(0.18 * SR))
    whistle = 0.25 * np.sin(2 * np.pi * np.cumsum(900 + 1400 * (t / sec) ** 2) / SR)
    x = (sweep * 1.3 + whistle * env) * env
    return normalize(soft_clip(normalize(x)))


def latch():
    """Heavy armour plate seating: impact + metal ring + short rattle."""
    sec = 0.7
    x = np.zeros(int(SR * sec))
    impact = lowpass(noise(0.08), 900) * exp_decay(int(0.08 * SR), 0.012) * 3
    ring = metal(0.7, [(410, 1.0, 1.0), (1093, 0.6, 0.6), (1987, 0.4, 0.35), (3220, 0.25, 0.2), (5105, 0.15, 0.12)], 0.18)
    place(x, impact, 0)
    x += ring * 0.7
    place(x, click(0.012, 2500) * 1.2, 0.0)
    place(x, metal(0.15, [(2600, 0.5, 1), (3900, 0.3, 0.6)], 0.03), 0.055)
    return normalize(soft_clip(normalize(x), 1.6))


def lock():
    """Mechanical lock: servo whir, two bolts shooting home."""
    sec = 0.45
    x = np.zeros(int(SR * sec))
    t = t_axis(0.16)
    whir = np.sign(np.sin(2 * np.pi * np.cumsum(260 + 900 * t / 0.16) / SR)) * 0.15
    whir = lowpass(whir, 3000) * env_adsr(len(t), 0.02, 0.05, 0.8, 0.04)
    place(x, whir, 0)
    for at in (0.17, 0.235):
        place(x, click(0.015, 1800) * 1.4, at)
        place(x, metal(0.12, [(1650, 0.7, 1), (2730, 0.4, 0.6), (4400, 0.25, 0.4)], 0.025), at)
    return normalize(x)


def hiss():
    """Pressurising seal: bright hiss that falls in pitch and fades, with a low thunk."""
    sec = 1.1
    n = noise(sec)
    h = sweep_filter(n, 7000, 2600, q=1.6) * env_adsr(len(n), 0.015, 0.25, 0.45, 0.55)
    thunk = lowpass(noise(0.1), 200) * exp_decay(int(0.1 * SR), 0.02) * 2.5
    x = h * 1.2
    place(x, thunk, 0)
    return normalize(x, 0.8)


def unlock():
    """Release: bolts withdraw, seal vents, plate pops loose."""
    sec = 0.8
    x = np.zeros(int(SR * sec))
    for at in (0.0, 0.05):
        place(x, click(0.012, 2200), at)
        place(x, metal(0.1, [(1450, 0.6, 1), (2390, 0.35, 0.6)], 0.02), at)
    n = noise(0.55)
    vent = sweep_filter(n, 1800, 6500, q=1.5) * env_adsr(len(n), 0.03, 0.1, 0.7, 0.35)
    place(x, vent * 0.9, 0.09)
    place(x, lowpass(noise(0.06), 600) * exp_decay(int(0.06 * SR), 0.01) * 2.2, 0.12)
    return normalize(x)


def purge():
    """Detach thrusters: short ignition burst sweeping upward."""
    sec = 0.9
    n = noise(sec)
    roar = sweep_filter(n, 600, 3600, q=2.2) * env_adsr(len(n), 0.01, 0.15, 0.6, 0.55)
    pop = lowpass(noise(0.05), 400) * exp_decay(int(0.05 * SR), 0.01) * 3
    x = roar * 1.3
    place(x, pop, 0)
    return normalize(soft_clip(normalize(x)))


def complete():
    """Maintenance complete chime: soft two-note bell."""
    sec = 1.2
    x = np.zeros(int(SR * sec))
    for at, f in ((0.0, 880.0), (0.14, 1318.5)):
        bell = metal(1.0, [(f, 1.0, 1.0), (f * 2.01, 0.35, 0.5), (f * 3.02, 0.15, 0.3)], 0.32)
        place(x, bell, at)
    return normalize(x, 0.7)


def turbine_loop():
    """Seamless turbine whine ("kiiin"): stacked high partials with slow beating, airy band."""
    sec, fade = 2.0, 0.25
    t = t_axis(sec + fade)
    x = np.zeros_like(t)
    for f, a in ((3150, 0.5), (4720, 0.32), (6310, 0.2), (1575, 0.18), (9460, 0.08)):
        x += a * np.sin(2 * np.pi * f * t + 0.4 * np.sin(2 * np.pi * 0.7 * t))
    x *= 0.85 + 0.15 * np.sin(2 * np.pi * 3.5 * t)
    x += bandpass(noise(sec + fade), 2500, 7000) * 0.35
    F = int(fade * SR)
    body = x[:int(sec * SR)].copy()
    tail = x[int(sec * SR):]
    w = np.linspace(0, 1, F)
    body[:F] = body[:F] * w + tail * (1 - w)
    return normalize(body, 0.7)


def alarm():
    """Launch warning: three two-tone buzzer pulses."""
    sec = 1.25
    x = np.zeros(int(SR * sec))
    for k in range(3):
        for j, f in enumerate((740.0, 990.0)):
            n = int(0.16 * SR)
            tt = np.arange(n) / SR
            tone = signal.square(2 * np.pi * f * tt, duty=0.5) * 0.5 + np.sin(2 * np.pi * f * 2 * tt) * 0.2
            tone = lowpass(tone, 4000) * env_adsr(n, 0.005, 0.02, 0.85, 0.02)
            place(x, tone, k * 0.4 + j * 0.17)
    return normalize(x, 0.75)


def hatch():
    """Launch hatch: latch clunk, hydraulic slide and venting hiss."""
    sec = 1.4
    x = np.zeros(int(SR * sec))
    place(x, lowpass(noise(0.1), 500) * exp_decay(int(0.1 * SR), 0.02) * 3, 0)
    place(x, metal(0.4, [(320, 1.0, 1.0), (870, 0.5, 0.6), (1530, 0.3, 0.4)], 0.09), 0.0)
    n = int(0.8 * SR)
    tt = np.arange(n) / SR
    motor = signal.sawtooth(2 * np.pi * (90 + 60 * tt / 0.8) * tt) * 0.25
    motor = lowpass(motor, 900) * env_adsr(n, 0.05, 0.1, 0.8, 0.15)
    place(x, motor, 0.12)
    vent = sweep_filter(noise(0.7), 6000, 2000, q=1.5) * env_adsr(int(0.7 * SR), 0.02, 0.2, 0.5, 0.35)
    place(x, vent * 0.8, 0.55)
    place(x, metal(0.3, [(510, 0.8, 1), (1320, 0.4, 0.6)], 0.06), 0.95)
    return normalize(soft_clip(normalize(x)))


def brake():
    """Air brake: retro-thrust blast, a hard roar swelling then cut, with a pressure crack."""
    sec = 0.8
    n = noise(sec)
    roar = sweep_filter(n, 2600, 700, q=2.0) * env_adsr(len(n), 0.008, 0.12, 0.7, 0.4)
    crack = highpass(noise(0.03), 1500) * exp_decay(int(0.03 * SR), 0.004) * 4
    thump = lowpass(noise(0.15), 160) * exp_decay(int(0.15 * SR), 0.04) * 4
    x = roar * 1.4
    place(x, crack, 0)
    place(x, thump, 0)
    return normalize(soft_clip(normalize(x), 1.6))


def pulse():
    """Attitude-control pulse jet: short sharp pop with a puff."""
    sec = 0.22
    x = np.zeros(int(SR * sec))
    place(x, highpass(noise(0.02), 1200) * exp_decay(int(0.02 * SR), 0.003) * 3, 0)
    place(x, bandpass(noise(0.18), 900, 5000) * exp_decay(int(0.18 * SR), 0.035), 0.004)
    place(x, lowpass(noise(0.05), 250) * exp_decay(int(0.05 * SR), 0.012) * 2, 0)
    return normalize(x, 0.8)


SOUNDS = {
    'sfx_armory_jet_loop': jet_loop,
    'sfx_armory_launch': launch,
    'sfx_armory_flyby': flyby,
    'sfx_armory_latch': latch,
    'sfx_armory_lock': lock,
    'sfx_armory_hiss': hiss,
    'sfx_armory_unlock': unlock,
    'sfx_armory_purge': purge,
    'sfx_armory_complete': complete,
    'sfx_armory_turbine_loop': turbine_loop,
    'sfx_armory_alarm': alarm,
    'sfx_armory_hatch': hatch,
    'sfx_armory_brake': brake,
    'sfx_armory_pulse': pulse,
}


def write_ogg(name, data, out):
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        path = tmp.name
    with wave.open(path, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    dst = os.path.join(out, name + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', path, '-c:a', 'libvorbis', '-q:a', '4', dst], check=True)
    os.unlink(path)
    return dst


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, fn in SOUNDS.items():
        print(write_ogg(name, fn(), out))
