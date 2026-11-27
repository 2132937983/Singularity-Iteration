"""Player suit assembly, deployment and two one-handed wingman orders."""
from dataclasses import replace
from math import radians, sin

from an1_locomotion import Pose, staged


def planted(pitch, compression, **upper):
    # Shift the pelvis opposite the straight legs' swing. The feet can roll
    # onto a different sole edge while their centres stay put in depth.
    return Pose(legs=(pitch, pitch), shift=(0, 12 * sin(radians(pitch))),
                compress=compression, **upper)


def suit_up():
    # Shoulders accept the plates in succession. At the helmet marker both
    # rigid gauntlets rotate in their sockets and settle with the collar.
    opened = planted(3, .15, torso=(3, 0, 0), head=(12, 0, 0),
                     arms=((13, -6, -53), (11, 6, 52)))
    latched = planted(-2, .85, torso=(-3, 0, 0), head=(5, 0, 0),
                      arms=((7, 25, -45), (7, -25, 45)))
    return staged([
        (0, Pose()),
        (.14, planted(-2, .45, torso=(-3, 0, 0), head=(2, 0, 0),
                      arms=((-2, 0, -9), (-2, 0, 7)))),
        (.32, planted(2, .20, torso=(2, -4, -2), head=(5, 3, 1),
                      arms=((12, -4, -40), (6, 2, 21)))),
        (.49, planted(3, .15, torso=(4, 3, 1), head=(8, -2, 0),
                      arms=((11, -6, -46), (14, 7, 49)))),
        (.66, opened),
        (.79, replace(opened, torso=(1, 0, 0), head=(13, 0, 0),
                      arms=((10, -5, -49), (9, 6, 48)))),
        (.99, replace(opened, head=(17, 0, 0))),
        (1.05, replace(opened, head=(16, 0, 0))),
        (1.10, latched), (1.18, latched),
        (1.29, planted(2, .18, torso=(2, 0, 0), head=(0, 0, 0),
                       arms=((3, 11, -30), (4, -12, 30)))),
        (1.44, Pose(arms=((-2, 1, -3), (-1, -1, 3)))),
        (1.50, Pose()),
    ])


def eject():
    # The chest moves between two planted feet. Both hands lead the release;
    # while the arms are still out, the unarmoured chest recoils a beat later.
    release = planted(-6, .30, torso=(-9, 0, 0), head=(7, 0, 0),
                      arms=((101, -7, -4), (100, 7, 4)))
    return staged([
        (0, Pose()),
        (.11, planted(3, .15, torso=(3, -3, 0), head=(-2, 2, 0),
                      arms=((-10, -3, -12), (-8, 3, 10)))),
        (.23, planted(6, .65, torso=(7, -3, 0), head=(-4, 3, 0),
                      arms=((24, -8, -20), (19, 7, 19)))),
        (.30, planted(3, .55, torso=(0, -1, 0), head=(1, 1, 0),
                      arms=((48, -9, -16), (41, 8, 16)))),
        (.36, planted(-4, .4, torso=(-7, 0, 0), head=(6, 0, 0),
                      arms=((87, -7, -7), (82, 7, 7)))),
        (.40, release), (.46, release),
        (.54, planted(3, .32, torso=(5, 0, 0), head=(-2, 0, 0),
                      arms=((90, -7, -7), (88, 7, 7)))),
        (.67, planted(4, .12, torso=(3, 1, 0), head=(-3, -1, 0),
                      arms=((58, -5, -12), (51, 5, 13)))),
        (.83, planted(-1, .12, torso=(-1, 0, 0), head=(1, 0, 0),
                      arms=((13, -2, -8), (9, 2, 7)))),
        (.93, Pose(arms=((-3, 0, -3), (-2, 0, 3)))), (1, Pose()),
    ])


def point():
    level = Pose(torso=(-5, -12, 2), head=(5, 12, -2),
                 arms=((95, 12, -3), (-2, 0, 5)))
    return staged([
        (0, Pose()),
        (.06, Pose(torso=(1, 3, 0), head=(-1, -3, 0),
                   arms=((-6, 1, -7), (0, 0, 2)))),
        (.15, Pose(torso=(-3, -8, 2), head=(3, 8, -1),
                   arms=((41, 20, -13), (-2, 0, 4)))),
        (.21, Pose(torso=(-5, -11, 2), head=(4, 11, -2),
                   arms=((80, 15, -7), (-2, 0, 5)))),
        (.25, level), (.36, level),
        (.44, Pose(torso=(-2, -7, 1), head=(2, 7, -1),
                   arms=((60, 11, -5), (1, 0, 4)))),
        (.54, Pose(arms=((5, 1, -4), (2, 0, 2)))), (.60, Pose()),
    ])


def recall():
    # A single sweep back past the right shoulder; the glove stays outside the
    # helmet and its return takes a lower arc than its outward signal.
    gathered = Pose(torso=(3, 13, -4), head=(-2, -10, 3),
                    arms=((-46, 25, -137), (6, 0, 7)))
    return staged([
        (0, Pose()),
        (.06, Pose(torso=(-2, -4, 1), head=(1, 4, 0),
                   arms=((14, 3, -14), (0, 0, 2)))),
        (.15, Pose(torso=(-1, -4, -1), head=(1, 5, 1),
                   arms=((24, 0, -104), (0, 0, 6)))),
        (.22, Pose(torso=(1, 2, -3), head=(-1, -1, 2),
                   arms=((-3, 1, -145), (2, 0, 7)))),
        (.30, gathered), (.39, gathered),
        (.48, Pose(torso=(2, 7, -2), head=(0, -6, 2),
                   arms=((-42, 0, -73), (4, 0, 5)))),
        (.58, Pose(torso=(-1, -2, 1), head=(1, 2, -1),
                   arms=((-14, 0, -19), (2, 0, 3)))),
        (.65, Pose(arms=((3, 0, -3), (1, 0, 1)))), (.70, Pose()),
    ])


def build_gestures():
    return {"suit_up": suit_up(), "eject": eject(),
            "command_point": point(), "command_recall": recall()}
