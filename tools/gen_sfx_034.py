#!/usr/bin/env python3
"""
0.1.7.34: procedurally synthesised sound effects (original, no samples from any game).

  maneuver_boom     shock-barrier crack: two N-wave pressure jumps + rolling low rumble
  maneuver_wind     seamless 4 s wind loop (band-passed noise, slow gust modulation)
  maneuver_takeoff  launch: sub thump + rising air rush
  fcs_echo          sonar echo ping with a digital bit-crushed tail (ore scanner)
  fcs_lock          two-tone lock chirp (ballistic computer on target)
  fcs_blast_beep    short warning beep (blast warning)

Usage: gen_sfx_034.py <repo root>   (needs numpy + ffmpeg)
"""
import os, subprocess, sys, wave
import numpy as np

SR = 44100
ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
OUT = os.path.join(ROOT, 'src/main/resources/assets/mio_icif/sounds')
rng = np.random.default_rng(34)


def t(sec):
    return np.arange(int(sec * SR)) / SR


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def onepole_lp_fast(x, cutoff):
    # vectorised enough for our lengths: run the recursion with scipy if present
    try:
        from scipy.signal import lfilter
        a = np.exp(-2 * np.pi * cutoff / SR)
        return lfilter([1 - a], [1, -a], x)
    except ImportError:
        return lowpass(x, cutoff)


def bandpass(x, lo, hi):
    return onepole_lp_fast(x, hi) - onepole_lp_fast(x, lo)


def norm(x, peak=0.89):
    m = np.max(np.abs(x))
    return x if m == 0 else x / m * peak


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    wav = os.path.join('/tmp', name + '.wav')
    data = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())
    ogg = os.path.join(OUT, name + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', ogg], check=True)
    os.remove(wav)
    print(name, len(x) / SR, 's')


def n_wave(length_s, at_s, total_s, width_ms=4.0):
    """An N-shaped pressure pulse: sharp rise, linear fall through zero, sharp return."""
    x = np.zeros(int(total_s * SR))
    start = int(at_s * SR)
    n = int(length_s * SR)
    ramp = np.linspace(1, -1, n)
    x[start:start + n] = ramp[: max(0, min(n, len(x) - start))]
    # soften the edges a little so the codec does not ring
    k = max(1, int(width_ms / 1000 * SR / 8))
    kern = np.hanning(k * 2 + 1)
    return np.convolve(x, kern / kern.sum(), mode='same')


def boom():
    total = 2.6
    x = n_wave(0.11, 0.02, total) * 1.0 + n_wave(0.10, 0.155, total) * 0.85
    # rolling thunder: low-passed noise with a slow decay and some low "tumble"
    tt = t(total)
    rumble = onepole_lp_fast(rng.standard_normal(len(tt)), 180) * 9.0
    env = np.exp(-tt * 1.6) * np.clip(tt * 12, 0, 1)
    tumble = 0.6 + 0.4 * np.sin(2 * np.pi * 3.1 * tt + 1.3) * np.sin(2 * np.pi * 0.7 * tt)
    rumble *= env * tumble
    crack = bandpass(rng.standard_normal(len(tt)), 900, 6000) * np.exp(-tt * 22) * 0.35
    sub = np.sin(2 * np.pi * 38 * tt) * np.exp(-tt * 3.0) * np.clip(tt * 40, 0, 1) * 0.5
    y = x * 0.95 + rumble * 0.55 + crack + sub
    return norm(y, 0.95)


def wind():
    total = 4.0
    tt = t(total + 0.5)
    noise = rng.standard_normal(len(tt))
    body = bandpass(noise, 120, 900)
    hiss = bandpass(rng.standard_normal(len(tt)), 1500, 5000) * 0.25
    gust = 0.65 + 0.35 * np.sin(2 * np.pi * 0.5 * tt) * np.sin(2 * np.pi * 0.25 * tt + 0.7)
    y = (body + hiss) * gust
    # seamless loop: crossfade the extra 0.5 s tail over the head
    n = int(total * SR)
    f = int(0.5 * SR)
    loop = y[:n].copy()
    fade = np.linspace(0, 1, f)
    loop[:f] = loop[:f] * fade + y[n:n + f] * (1 - fade)
    return norm(loop, 0.7)


def takeoff():
    total = 1.4
    tt = t(total)
    thump = np.sin(2 * np.pi * (55 - 25 * tt) * tt) * np.exp(-tt * 9) * 1.2
    rush = bandpass(rng.standard_normal(len(tt)), 200, 2500 + 3000 * tt) if False else bandpass(rng.standard_normal(len(tt)), 250, 3500)
    rush *= np.clip(tt * 3, 0, 1) * np.exp(-np.maximum(0, tt - 0.5) * 3.5)
    debris = bandpass(rng.standard_normal(len(tt)), 2000, 7000) * np.exp(-tt * 14) * 0.4
    return norm(thump + rush * 0.7 + debris, 0.9)


def echo():
    total = 1.8
    tt = t(total)
    ping = np.sin(2 * np.pi * 1250 * tt) * np.exp(-tt * 5.5)
    ping += np.sin(2 * np.pi * 1875 * tt) * np.exp(-tt * 9) * 0.35
    # returning echoes (spaced like a sweep reaching farther terrain)
    y = ping.copy()
    for k, d in enumerate([0.22, 0.41, 0.63, 0.88]):
        s = int(d * SR)
        y[s:] += ping[: len(y) - s] * (0.42 / (k + 1))
    # digital glitch tail: bit-crush and sample-hold in bursts
    crushed = np.round(y * 6) / 6
    hold = crushed.copy()
    step = 9
    for i in range(0, len(hold), step):
        hold[i:i + step] = crushed[i]
    mask = (np.sin(2 * np.pi * 7 * tt) > 0.55) & (tt > 0.15)
    y = np.where(mask, hold * 0.8, y)
    sweep = np.sin(2 * np.pi * (300 + 2400 * tt) * tt) * 0.06 * np.exp(-tt * 3)
    return norm(y + sweep, 0.8)


def lock():
    total = 0.32
    tt = t(total)
    a = np.sin(2 * np.pi * 1760 * tt) * ((tt < 0.09) * 1.0)
    b = np.sin(2 * np.pi * 2349 * tt) * ((tt > 0.12) & (tt < 0.30)) * 1.0
    env = np.minimum(1, np.minimum(tt * 400, (total - tt) * 60))
    return norm((a + b) * env, 0.6)


def beep():
    total = 0.14
    tt = t(total)
    x = np.sign(np.sin(2 * np.pi * 1450 * tt)) * 0.5 + np.sin(2 * np.pi * 2900 * tt) * 0.3
    env = np.minimum(1, np.minimum(tt * 600, (total - tt) * 120))
    return norm(onepole_lp_fast(x, 5000) * env, 0.55)


if __name__ == '__main__':
    save('maneuver_boom', boom())
    save('maneuver_wind', wind())
    save('maneuver_takeoff', takeoff())
    save('fcs_echo', echo())
    save('fcs_lock', lock())
    save('fcs_blast_beep', beep())
