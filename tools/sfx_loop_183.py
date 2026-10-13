#!/usr/bin/env python3
"""0.1.8.3: seamless loops for the thruster and flight-wind sounds.

The quantum chestplates (and the advanced quantum chestplate) use sfx_jetpack_ionic_loop. Its end
did not meet its start: a step of 0.21 full scale at the loop point gave a click every 4 s. The
other loops had smaller steps (0.02 - 0.06). This tool rebuilds each loop with an equal-power
crossfade: the last F seconds are blended into the first F seconds and the tail is cut, so the
end flows into the start. Length becomes (L - F).

Usage: sfx_loop_183.py <ffmpeg> <sounds dir> <work dir> <name> ... [--fade 0.3]
Prints the seam step (|last - first| sample, full scale 1) before and after.
"""
import array
import math
import os
import subprocess
import sys
import wave

RATE = 44100


def decode(ffmpeg, src, raw):
    subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', src, '-f', 's16le', '-ac', '1', '-ar', str(RATE), raw], check=True)
    a = array.array('h')
    with open(raw, 'rb') as f:
        a.frombytes(f.read())
    return a


def seam(a):
    return abs(a[-1] - a[0]) / 32768


def crossfade(a, fade_s):
    n = len(a)
    f = int(RATE * fade_s)
    out = array.array('h', a[:n - f])
    for i in range(f):
        t = i / f
        w_in, w_out = math.sin(t * math.pi / 2), math.cos(t * math.pi / 2)
        v = a[i] * w_in + a[n - f + i] * w_out
        out[i] = max(-32768, min(32767, int(round(v))))
    return out


def encode(ffmpeg, samples, wav_path, ogg_path):
    with wave.open(wav_path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(samples.tobytes())
    subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', wav_path, '-c:a', 'libvorbis', '-q:a', '5', '-ac', '1', ogg_path], check=True)


def main():
    args = sys.argv[1:]
    fade = 0.3
    if '--fade' in args:
        i = args.index('--fade')
        fade = float(args[i + 1])
        args = args[:i] + args[i + 2:]
    ffmpeg, sounds, work = args[:3]
    os.makedirs(work, exist_ok=True)
    for name in args[3:]:
        src = os.path.join(sounds, name + '.ogg')
        before = decode(ffmpeg, src, os.path.join(work, name + '.raw'))
        looped = crossfade(before, fade)
        encode(ffmpeg, looped, os.path.join(work, name + '.wav'), src)
        after = decode(ffmpeg, src, os.path.join(work, name + '.after.raw'))
        print('%-28s seam %.3f -> %.3f  length %.2fs -> %.2fs' % (name, seam(before), seam(after), len(before) / RATE, len(after) / RATE))


if __name__ == '__main__':
    main()
