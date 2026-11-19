#!/usr/bin/env python3
"""Original nuclear pressure crack + bass roar + settling rumble; no audio samples.
Requires numpy and soundfile (OGG/Vorbis encoder). Optional --encoder-path for a local install.
Example: python tools/nuclear_sfx.py --encoder-path build/nuclear-audio-tools
"""
import argparse
import sys
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--encoder-path')
parser.add_argument('--output', default='src/main/resources/assets/mio_icif/sounds/nuclear_explosion.ogg')
args = parser.parse_args()
if args.encoder_path:
    sys.path.insert(0, args.encoder_path)
import numpy as np
import soundfile as sf

rate = 44100
seconds = 9
count = rate * seconds
t = np.arange(count) / rate
random = np.random.default_rng(20261004)
frequency = np.fft.rfftfreq(count, 1 / rate)

def turbulence(low, high):
    signal = random.normal(size=count)
    spectrum = np.fft.rfft(signal)
    band = (frequency / np.maximum(frequency + low, 1)) ** 2 / (1 + (frequency / high) ** 4)
    filtered = np.fft.irfft(spectrum * band, n=count)
    return filtered / max(1e-9, np.std(filtered))

attack = np.minimum(1, t / .002)
phase = 2 * np.pi * np.cumsum(28 + 100 * np.exp(-t / .1)) / rate
sub = np.sin(phase) * np.exp(-t / 1.4) * .48
crack = turbulence(500, 11000) * np.exp(-t / .055) * .65
roar = turbulence(25, 700) * np.exp(-t / 2.2) * (.75 + .2 * np.sin(t * 11)) * .40
hiss = turbulence(300, 2400) * np.exp(-t / .9) * .16
rumble = turbulence(15, 160) * np.minimum(1, t / .2) * np.exp(-t / 3.1) * .34
signal = (sub + crack + roar + hiss + rumble) * attack
# Reflections trail the direct front instead of replaying an unrelated thunder sample.
for delay, strength in ((.28, .24), (.72, .12)):
    offset = int(delay * rate)
    signal[offset:] += roar[:-offset] * strength
signal *= np.minimum(1, np.maximum(0, (seconds - t) / .6))
signal = np.tanh(signal * 1.3)
signal *= .70 / max(1e-9, np.max(np.abs(signal)))
output = Path(args.output)
output.parent.mkdir(parents=True, exist_ok=True)
sf.write(output, signal.astype('float32'), rate, format='OGG', subtype='VORBIS')
print(f'{output}: {seconds}s mono, {rate} Hz, peak {np.max(np.abs(signal)):.2f}')
