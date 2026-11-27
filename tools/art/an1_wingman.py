"""Author AN1's idle, reaction and power clips, and export the wingman runtime JSON.

Authoring space has Y up, the mannequin faces -Z, and positive angles are
right handed. The two animation-axis sign changes are made only at export.
Run an1_rig.py after this script to refresh the editable Blockbench project.
"""

import json
import math
import re
from pathlib import Path

from an1_locomotion import simplify, tracks as locomotion_tracks
from an1_melee import build_melee
from an1_ranged import build_ranged
from an1_tactics import tracks as tactics_tracks


ROOT = Path(__file__).resolve().parents[2]
ZERO = (0, 0, 0)


def keys(*rows):
    """One channel, in seconds and XYZ degrees or model pixels."""
    return {time: (x, y, z) for time, x, y, z in rows}


def rotation(*rows):
    return {"rotation": keys(*rows)}


def idle_and_calibration():
    stand = {
        "root": {"position": keys((0, -.15, 0, 0), (.85, -.39, 0, .04),
                                  (2, .18, 0, 0), (2.85, .4, 0, -.03), (4, -.15, 0, 0))},
        "body": {"position": keys((0, 0, .04, 0), (.95, 0, .26, -.08),
                                  (2.1, 0, .02, 0), (2.95, 0, .15, -.04), (4, 0, .04, 0)),
                 **rotation((0, -1, .7, -.5), (.95, -2, 1.3, -.8),
                            (2.1, -.6, -.6, .3), (3, -1.6, -.9, .7), (4, -1, .7, -.5))},
        "head": rotation((0, .6, -.3, 0), (1.1, 1.2, -.6, .5), (2.2, .4, .5, -.1),
                         (3.1, 1.1, .8, -.4), (4, .6, -.3, 0)),
        "right_arm": rotation((0, 2, 0, -2), (1.15, 3.5, .3, -2.8),
                              (2.3, 1.4, -.3, -1.8), (3.12, 2.6, 0, -2.5), (4, 2, 0, -2)),
        "left_arm": rotation((0, -.5, .7, 2), (1.2, -2.1, .4, 2.7),
                             (2.15, .1, .5, 1.7), (3.13, -1.3, .9, 2.5), (4, -.5, .7, 2)),
    }
    return {
        "calibrate": {
            "root": {"position": keys((0, 0, 0, 0), (.5, 0, 2, 0))},
            "right_arm": rotation((0, 0, 0, 0), (.5, 90, 0, 0)),
            "left_leg": rotation((0, 0, 0, 0), (.5, -30, 0, 0)),
            "head": rotation((0, 0, 0, 0), (.5, 0, -45, 0)),
        },
        "idle_stand": stand,
        "idle_look": {
            "head": rotation((0, 0, 0, 0), (.13, 1, -6, 0), (.32, 2, -33, -1),
                             (.48, 1, -39, -2), (.78, 0, -33, -1), (.94, -2, -22, -.5),
                             (1.20, 0, 31, 2), (1.37, 1, 43, 2), (1.72, 0, 36, 1),
                             (2.03, 0, 25, 1), (2.32, -1, -2, -.3), (2.5, 0, 0, 0)),
            "body": rotation((0, 0, 0, 0), (.22, 0, 0, 0), (.53, 0, -9, -.4),
                             (.86, 0, -11, -.6), (1.09, 0, -8, 0), (1.49, -.5, 10, .6),
                             (1.86, -.4, 12, .5), (2.23, 0, 5, .1), (2.5, 0, 0, 0)),
            "right_arm": rotation((0, 0, 0, 0), (.62, 3, 0, -2), (1.53, -3, 0, -1), (2.5, 0, 0, 0)),
        },
        "idle_check": {
            "right_arm": rotation((0, 0, 0, 0), (.2, -5, 0, -6), (.48, 32, -7, -13),
                                  (.72, 64, -12, -9), (.85, 70, -13, -8), (1.12, 68, -13, -8),
                                  (1.34, 71, -30, -11), (1.57, 68, -32, -11),
                                  (1.81, 70, -9, -7), (2.14, 64, -10, -6),
                                  (2.41, 50, -8, -9), (2.72, -4, 0, -5), (3, 0, 0, 0)),
            "body": rotation((0, 0, 0, 0), (.52, -3, 5, -2), (.95, -7, 7, -2),
                             (1.68, -7, 8, -1), (2.17, -6, 5, -2), (2.66, 1, -1, 0), (3, 0, 0, 0)),
            "head": rotation((0, 0, 0, 0), (.2, -5, 8, 0), (.53, -18, 16, -4),
                             (.85, -22, 19, -4), (1.33, -24, 15, -5),
                             (1.83, -20, 11, -3), (2.35, -17, 10, -2), (2.8, 1, -1, 0), (3, 0, 0, 0)),
            "left_arm": rotation((0, 0, 0, 0), (.66, -7, 0, 5), (1.4, -5, 0, 3),
                                 (2.34, -6, 0, 4), (2.77, 3, 0, 2), (3, 0, 0, 0)),
        },
    }


def hit(front_back=0, side=0):
    """The impact displaces the chest first. Head and loose arms catch it later."""
    direction = -1 if side < 0 else 1
    tracks = {
        "body": rotation((0, 0, 0, 0), (.035, 12*front_back, 3*side, 14*side),
                         (.075, 13*front_back, 4*side, 16*side), (.12, 12*front_back, 4*side, 15*side),
                         (.205, 4*front_back, side, 5*side), (.268, -2*front_back, 0, -2*side), (.35, 0, 0, 0)),
        "head": rotation((0, 0, 0, 0), (.035, -4*front_back, -side, -4*side),
                         (.088, 10*front_back, 3*side, 11*side), (.13, 12*front_back, 5*side, 13*side),
                         (.21, 5*front_back, 2*side, 5*side), (.295, -2*front_back, 0, -2*side), (.35, 0, 0, 0)),
    }
    # The torso pivots at the collar. Translate the shoulder with the blow
    # so the bottom of the chest remains seated over the pelvis.
    tracks["body"]["position"] = keys((0, 0, 0, 0), (.035, -2.4*side, -.2, 2.2*front_back),
                                      (.09, -2.9*side, -.4, 2.4*front_back),
                                      (.205, -.9*side, -.1, .7*front_back),
                                      (.268, .35*side, 0, -.25*front_back), (.35, 0, 0, 0))
    for hand, outside in (("right_arm", -1), ("left_arm", 1)):
        amplitude = 1.0 if not side or direction == outside else .58
        tracks[hand] = rotation((0, 0, 0, 0), (.045, 0, 0, outside*2),
                                (.105, -15*front_back*amplitude + 3*side, 2*side, outside*(6+6*amplitude)),
                                (.165, -16*front_back*amplitude + 3*side, 3*side, outside*(7+6*amplitude)),
                                (.26, 3*front_back*amplitude, 0, outside*3), (.35, 0, 0, 0))
    return tracks


def smooth_legs(rows):
    """Settle the pelvis on the lower of two rigid boot soles every 20 ms.

    Rows are time, right/left hip pitch, right/left leg Y offset, lateral and
    rearward pelvis offset. The sole spans two pixels on either side in Z.
    """
    times = sorted({i / 50 for i in range(round(rows[-1][0] * 50) + 1)} | {r[0] for r in rows})
    result = {"root": {"position": {}}, "right_leg": {"rotation": {}, "position": {}},
              "left_leg": {"rotation": {}, "position": {}}}
    segment = 0
    for t in times:
        while segment+2 < len(rows) and rows[segment+1][0] < t:
            segment += 1
        start, end = rows[segment:segment+2]
        w = (t-start[0]) / (end[0]-start[0])
        w = w*w*(3-2*w)
        right, left, lift_r, lift_l, x, z = (a+(b-a)*w for a,b in zip(start[1:],end[1:]))
        heights = [12-12*math.cos(math.radians(pitch))-2*abs(math.sin(math.radians(pitch)))+lift
                   for pitch,lift in ((right,lift_r),(left,lift_l))]
        result["root"]["position"][t] = (x, -min(heights), z)
        for name,pitch,lift in (("right_leg",right,lift_r),("left_leg",left,lift_l)):
            result[name]["rotation"][t] = (pitch,0,0)
            result[name]["position"][t] = (0,lift,0)
    for channels in result.values():
        for name, curve in channels.items():
            tolerance = .04 if name == "rotation" else .005
            channels[name] = dict(simplify(list(curve.items()), tolerance))
    return result


def recovery_clips():
    # Share the actual disabled stop pose: waking starts where the motors froze.
    rest_legs = (0,0,0,0,0,0)
    # Let the rear shin lie close to the floor, with the lead boot out in
    # front. Small hip offsets stand in for knee flexion on the rigid rig.
    kneel = (66,-85,2.55,-1.05,.8,.3)
    slump = smooth_legs([(0,*rest_legs),(.20,2,-2,0,0,.18,.08),
                         (.39,11,-18,.4,0,.4,.3),(.66,45,-54,1.4,-.45,.7,.6),
                         (.89,68,-87,2.65,-1.15,.85,.35),(1.04,*kneel),(1.20,*kneel)])
    wake = smooth_legs([(0,*kneel),(.18,*kneel),(.27,64,-81,2.3,-.95,.7,.26),
                        (.46,30,-36,1,-.35,.3,.12),(.60,5,-8,0,0,.04,0),
                        (.70,-2,2,0,0,-.04,0),(.8,*rest_legs)])
    stop = {"body": (-18,4,3), "head": (-37,7,5),
            "right_arm": (17,-3,-3), "left_arm": (19,4,6)}
    fall_poses = {
        "body": ((.22,-2,0,1),(.55,-13,3,4),(.84,-22,5,5),(1.02,-18,4,3)),
        "head": ((.12,-9,2,1),(.35,-22,6,3),(.71,-30,8,5),(.94,-40,7,6)),
        "right_arm": ((.27,-3,0,-3),(.55,12,-3,-11),(.78,24,-4,-10),(1.02,19,-3,-4)),
        "left_arm": ((.29,2,0,3),(.56,7,4,15),(.85,26,6,13),(1.03,21,4,8)),
    }
    wake_poses = {
        "head": ((.05,-36,7,5),(.14,-8,3,1),(.19,4,1,0),(.35,7,0,-2),(.59,2,-1,-1),(.7,-2,0,0)),
        "body": ((.17,-18,4,3),(.28,-23,3,3),(.47,-7,1,0),(.60,3,-1,-1),(.71,1,0,0)),
        "right_arm": ((.19,16,-3,-4),(.28,28,-5,-10),(.47,-12,-2,-8),(.63,-6,0,-4),(.73,3,0,-2)),
        "left_arm": ((.19,19,4,6),(.29,25,6,12),(.49,-10,3,8),(.64,-4,0,4),(.74,2,0,2)),
    }
    for bone, pose in stop.items():
        slump[bone] = rotation((0,*ZERO), *fall_poses[bone], (1.12,*pose), (1.2,*pose))
        wake[bone] = rotation((0,*pose), *wake_poses[bone], (.8,*ZERO))
    slump["body"]["position"] = keys((0,0,0,0),(.22,0,0,0),(.55,-.4,-.4,-2.5),
                                     (.84,-.8,-.9,-3.9),(1.12,-.6,-.6,-3.5),(1.2,-.6,-.6,-3.5))
    wake["body"]["position"] = keys((0,-.6,-.6,-3.5),(.17,-.6,-.6,-3.5),(.28,-.6,-1,-4),
                                    (.47,-.2,-.2,-1.3),(.6,0,0,.4),(.8,0,0,0))
    stagger = smooth_legs([(0,*rest_legs),(.08,7,-5,0,0,-.2,.9),
                           (.18,-19,12,2.3,0,-.3,2.2),(.31,-24,16,0,0,-.5,3.6),
                           (.43,-14,-7,0,2.1,.2,4.3),(.57,8,-22,0,0,.55,5.1),
                           (.73,9,-12,0,0,.28,5.05),(.9,2,-3,0,0,0,4.7)])
    stagger.update({
        "body": rotation((0,*ZERO),(.065,19,8,-5),(.18,17,11,-9),(.31,8,6,-5),
                         (.47,-5,-11,7),(.59,-7,-9,5),(.75,3,2,-2),(.9,0,0,0)),
        "head": rotation((0,*ZERO),(.05,-3,0,2),(.12,11,4,-4),(.23,9,7,-6),
                         (.39,-4,-6,0),(.54,-5,-9,5),(.72,3,4,-1),(.9,0,0,0)),
        "right_arm": rotation((0,*ZERO),(.12,-8,9,-32),(.23,-10,14,-63),
                              (.41,28,8,-41),(.61,34,-6,-21),(.79,-4,1,-9),(.9,2,0,-3)),
        "left_arm": rotation((0,*ZERO),(.11,16,-8,26),(.26,42,-13,48),
                             (.45,-4,-6,63),(.62,-16,9,29),(.80,4,0,8),(.9,1,0,2)),
    })
    stagger["body"]["position"] = keys((0, 0, 0, 0), (.065, 1, -.5, 3.2),
                                       (.18, 1.6, -.6, 3), (.31, 1, -.2, 1.4),
                                       (.47, -1.3, -.2, -1), (.59, -.9, -.25, -1.2),
                                       (.75, .4, 0, .5), (.9, 0, 0, 0))
    return {"lowpower_slump": slump, "lowpower_wake": wake, "stagger": stagger}


def encode(tracks):
    result = {}
    for bone, channels in tracks.items():
        result[bone] = {}
        for channel, curve in channels.items():
            signs = (-1, 1, -1) if channel == "rotation" else (1, 1, 1)
            encoded = {}
            for time, vector in sorted(curve.items(), key=lambda entry: float(entry[0])):
                values = [round(value * sign, 4) for value, sign in zip(vector, signs)]
                # The Blockbench project uses a 100 Hz editing grid. Keep
                # these authored poses on it even after a manual re-export.
                tick = math.floor(float(time) * 100 + .5 + 1e-9)
                key = f"{tick / 100:g}"
                if key in encoded:
                    raise ValueError(f"{bone}.{channel}: two authored poses on tick {tick}")
                encoded[key] = [int(n) if n == int(n) else n for n in values]
            # A constant zero channel has exactly the rest transform.
            if any(any(v) for v in encoded.values()):
                result[bone][channel] = encoded
        if not result[bone]:
            del result[bone]
    return result


def main():
    spec = json.loads((ROOT / "docs/art/an1/spec.json").read_bytes())
    clips = {**idle_and_calibration(), **recovery_clips(), **locomotion_tracks(),
             **build_melee(), **build_ranged(), **tactics_tracks()}
    for name, directions in (("front",(1,0)),("back",(-1,0)),("left",(0,1)),("right",(0,-1))):
        clips["hit_"+name] = hit(*directions)
    entries = [a for a in spec["animations"] if a["file"] == "wingman"]
    if set(clips) != {e["name"] for e in entries}:
        raise ValueError("Authored clips differ from the wingman contract")
    file = spec["files"]["wingman"]
    animations = {}
    for entry in entries:
        mode = entry["loop"]
        anim = {"loop": {"loop":True, "play_once":False}.get(mode,mode),
                "animation_length":entry["length"], "bones":encode(clips[entry["name"]])}
        if entry["markers"]:
            anim["timeline"] = {f"{t:g}":name for name,t in entry["markers"].items()}
        animations[file["prefix"]+entry["name"]] = anim
    output = ROOT / file["path"]
    output.parent.mkdir(parents=True, exist_ok=True)
    data = {"format_version":"1.8.0", "animations":animations}
    # Compact numeric vectors, retain line breaks between named channels.
    serialized = re.sub(r'\[\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+)\s*\]',
                        r'[\1, \2, \3]', json.dumps(data, indent=2, allow_nan=False))
    output.write_text(serialized+"\n", encoding="utf8", newline="\n")


if __name__ == "__main__":
    main()
