#!/usr/bin/env python3
"""Additional synthesized SFX (no samples): metal former mode switch, mining-laser shot.
Usage: si_sfx.py <sounds-dir>"""
import os, sys
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from armory_sfx import (SR, rng, t_axis, noise, bandpass, lowpass, highpass, exp_decay, metal, click, place,
                        normalize, soft_clip, write_ogg, sweep_filter)

def mode_switch():
    """Heavy rotary selector: detent click, gear ratchet, latch clunk with a short metal ring."""
    y = np.zeros(int(0.32 * SR))
    place(y, click(0.012, 2800) * 0.9, 0.0)
    for i in range(4):   # ratchet teeth
        place(y, click(0.008, 4200) * (0.55 - 0.08 * i), 0.035 + i * 0.022)
    thud = bandpass(noise(0.06), 90, 420) * exp_decay(int(0.06 * SR), 0.012)
    place(y, thud * 2.2, 0.13)
    place(y, click(0.015, 1800) * 1.1, 0.13)
    ring = metal(0.18, [(1870, 0.5, 1.0), (2960, 0.3, 0.7), (4410, 0.18, 0.5)], 0.035)
    place(y, ring * 0.35, 0.132)
    return normalize(soft_clip(y, 1.2), 0.85)

def laser_shot():
    """Clean, condensed sci-fi bolt: fast downward chirp with a bright transient and a short tail."""
    sec = 0.34
    t = t_axis(sec)
    f = 520 + 2900 * np.exp(-t / 0.045)
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(ph) + 0.35 * np.sin(2 * ph + 0.3) + 0.15 * np.sin(3 * ph)
    env = np.minimum(1, t / 0.002) * np.exp(-t / 0.09)
    zap = tone * env
    fizz = bandpass(noise(sec), 2500, 9000) * np.exp(-t / 0.03) * 0.35
    sub = np.sin(2 * np.pi * 95 * t) * np.exp(-t / 0.05) * 0.5
    y = zap + fizz + sub
    return normalize(soft_clip(y, 1.6), 0.86)

SOUNDS = {'sfx_metal_former_switch': mode_switch, 'sfx_laser_shot': laser_shot}

def _chirp(sec, f0, f1, tau):
    t = t_axis(sec)
    f = f1 + (f0 - f1) * np.exp(-t / tau)
    return t, 2 * np.pi * np.cumsum(f) / SR

def rifle_shot():
    """Crisp energy-rifle crack: bright transient, fast falling tone, short noise tail."""
    t, ph = _chirp(0.28, 4200, 700, 0.03)
    tone = (np.sin(ph) + 0.4 * np.sin(2.01 * ph)) * np.minimum(1, t / 0.001) * np.exp(-t / 0.06)
    crack = highpass(noise(0.28), 1800) * np.exp(-t / 0.012) * 0.9
    body = bandpass(noise(0.28), 300, 1200) * np.exp(-t / 0.05) * 0.5
    return normalize(soft_clip(tone + crack + body, 1.8), 0.85)

def plasma_shot():
    """Plasma bolt: wobbling low tone with a hot hiss."""
    t, ph = _chirp(0.45, 900, 180, 0.08)
    wob = 1 + 0.25 * np.sin(2 * np.pi * 38 * t)
    tone = np.sin(ph * wob) * np.minimum(1, t / 0.004) * np.exp(-t / 0.14)
    hiss = bandpass(noise(0.45), 2000, 7000) * np.exp(-t / 0.08) * 0.4
    sub = np.sin(2 * np.pi * 70 * t) * np.exp(-t / 0.09) * 0.6
    return normalize(soft_clip(tone + hiss + sub, 1.5), 0.86)

def tachyon_shot():
    """Heavy disfission discharge: charge-up zip, detonation, sub-bass drop and crackling tail."""
    sec = 1.0
    t = t_axis(sec)
    y = np.zeros_like(t)
    zt, zph = _chirp(0.09, 800, 5200, 0.05)            # rising pre-zip
    zip_ = np.sin(-zph[::-1]) * np.linspace(0, 1, len(zt)) * 0.35
    place(y, zip_, 0.0)
    _, ph = _chirp(0.9, 2600, 90, 0.07)
    boom = (np.sin(ph) + 0.5 * np.sin(1.5 * ph)) * np.exp(-t_axis(0.9) / 0.22)
    place(y, boom, 0.08)
    sub = np.sin(2 * np.pi * 48 * t_axis(0.8)) * np.exp(-t_axis(0.8) / 0.25) * 0.9
    place(y, sub, 0.08)
    crackle = highpass(noise(0.9), 2500) * (rng.random(int(0.9 * SR)) > 0.985) * np.exp(-t_axis(0.9) / 0.35) * 3.0
    place(y, crackle, 0.1)
    place(y, click(0.02, 1500) * 2.0, 0.08)
    return normalize(soft_clip(y, 2.0), 0.9)

def tachyon_hit():
    """Electric burst on impact (multi-hit crackle)."""
    sec = 0.5
    t = t_axis(sec)
    y = highpass(noise(sec), 2000) * (rng.random(len(t)) > 0.95) * np.exp(-t / 0.12) * 2.5
    _, ph = _chirp(sec, 3000, 600, 0.05)
    y += np.sin(ph) * np.exp(-t / 0.05) * 0.6
    y += bandpass(noise(sec), 200, 900) * np.exp(-t / 0.04) * 0.8
    return normalize(soft_clip(y, 1.6), 0.85)

def energy_hit():
    """Short sizzle where an energy bolt lands."""
    sec = 0.3
    t = t_axis(sec)
    y = bandpass(noise(sec), 1500, 6000) * np.exp(-t / 0.06)
    y += bandpass(noise(sec), 150, 600) * np.exp(-t / 0.025) * 1.2
    return normalize(soft_clip(y, 1.4), 0.8)

def air_cannon_charge():
    """Compressor spin-up whine (one second segment, re-triggered with rising pitch while charging)."""
    sec = 0.6
    t = t_axis(sec)
    f = 300 + 500 * t / sec
    ph = 2 * np.pi * np.cumsum(f) / SR
    whine = (np.sin(ph) + 0.3 * np.sin(3 * ph)) * 0.5
    air = bandpass(noise(sec), 800, 3000) * 0.35
    env = np.minimum(1, t / 0.05) * np.minimum(1, (sec - t) / 0.08)
    return normalize((whine + air) * env, 0.6)

def air_cannon_blast():
    """Plasma air cannon release: pneumatic thump, shock crack and a long rushing air tail."""
    sec = 1.3
    t = t_axis(sec)
    thump = np.sin(2 * np.pi * (55 + 60 * np.exp(-t / 0.05)) * t) * np.exp(-t / 0.18) * 1.2
    crack = highpass(noise(sec), 1200) * np.exp(-t / 0.02) * 1.0
    rush = sweep_filter(noise(sec), 3500, 500) * np.exp(-t / 0.45) * 0.8
    return normalize(soft_clip(thump + crack + rush, 1.7), 0.92)

def air_cannon_hit():
    """Heavy body hit from the air blast."""
    sec = 0.35
    t = t_axis(sec)
    y = np.sin(2 * np.pi * (80 + 120 * np.exp(-t / 0.02)) * t) * np.exp(-t / 0.07) * 1.2
    y += bandpass(noise(sec), 200, 1500) * np.exp(-t / 0.03)
    return normalize(soft_clip(y, 1.5), 0.85)

def rocket_launch():
    """Rocket ignition: pop, roaring burn sweeping away."""
    sec = 1.2
    t = t_axis(sec)
    pop = bandpass(noise(sec), 100, 900) * np.exp(-t / 0.03) * 1.5
    roar = sweep_filter(noise(sec), 600, 2500) * np.minimum(1, t / 0.05) * np.exp(-t / 0.5)
    return normalize(soft_clip(pop + roar, 1.5), 0.88)

SOUNDS.update({
    'sfx_weapon_rifle_shot': rifle_shot, 'sfx_weapon_plasma_shot': plasma_shot,
    'sfx_weapon_tachyon_shot': tachyon_shot, 'sfx_weapon_tachyon_hit': tachyon_hit,
    'sfx_weapon_energy_hit': energy_hit, 'sfx_weapon_air_cannon_charge': air_cannon_charge,
    'sfx_weapon_air_cannon_blast': air_cannon_blast, 'sfx_weapon_air_cannon_hit': air_cannon_hit,
    'sfx_weapon_rocket_launch': rocket_launch,
})

if __name__ == '__main__':
    out = sys.argv[1]
    for name, fn in SOUNDS.items(): print(write_ogg(name, fn(), out))
