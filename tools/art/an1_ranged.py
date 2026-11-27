"""Nine upper-body performances for the AN1 wingman, in rig authoring space."""

from dataclasses import replace
from math import pi, sin

from an1_locomotion import Pose, bake, staged


# Turning the chest puts the two shoulders at different depths. This supplies
# a rear and a forward grip without shortening either of the rigid arms.
RIFLE = Pose(torso=(-4, 30, 3), head=(4, -28, 4),
             arms=((92, -46, -8), (89, 14, 9)))
PISTOL = Pose(torso=(-3, -12, 1), head=(3, 11, 1),
              arms=((94, 3, -3), (9, 1, 10)))


def breathing(base, t, pistol=False):
    # Both curves return to the reference pose at zero and at two seconds.
    wave = sin(pi * t)
    drift = sin(2 * pi * t) * .18
    right, left = base.arms
    return replace(base,
                   torso=(base.torso[0] - .75 * wave, base.torso[1], base.torso[2] + drift),
                   head=(base.head[0] + .48 * wave, base.head[1], base.head[2] - drift),
                   arms=((right[0] + .53 * wave, right[1] - drift, right[2]),
                         (left[0] + (.2 if pistol else .48) * wave, left[1], left[2] + drift)))


def recoil(heavy):
    # Keep the arms coupled. The collar, helmet and chest settle at different
    # times. Animation on the root is reserved for the heavy version.
    if not heavy:
        return staged([
            (0, RIFLE),
            (.03, Pose(torso=(.5, 31, 3.5), head=(2, -28, 4),
                       arms=((98, -46, -9), (95, 14, 9)))),
            (.05, Pose(torso=(1, 31, 3.5), head=(5, -28, 4),
                       arms=((97, -46, -9), (94, 14, 9)))),
            (.10, Pose(torso=(-4.6, 30, 3), head=(5.5, -28, 4),
                       arms=((91, -46, -8), (88, 14, 9)))),
            (.15, RIFLE),
        ])
    return staged([
        (0, RIFLE),
        (.04, Pose(shift=(.15, 1.7), torso=(6, 34, 4), head=(-2, -28, 4),
                   arms=((105, -42, -11), (105, 13, 10)))),
        (.08, Pose(shift=(.45, 4.1), torso=(9, 35, 5), head=(4, -30, 3),
                   arms=((106, -42, -11), (105, 14, 11)))),
        (.12, Pose(shift=(.5, 4.4), torso=(8, 35, 5), head=(7, -31, 3),
                   arms=((105, -45, -9), (103, 15, 10)))),
        (.20, Pose(shift=(.22, 2.5), torso=(-1, 32, 3), head=(7, -30, 4),
                   arms=((92, -46, -8), (90, 14, 9)))),
        (.28, Pose(shift=(.05, .55), torso=(-5, 29, 2.8), head=(4, -28, 4),
                   arms=((90, -46, -8), (87.5, 14, 9)))),
        (.35, RIFLE),
    ])


def reload_rifle():
    # The left (cyan) hand travels below the right, drops to the hip, then
    # returns. Keep the right hand raised throughout the exchange.
    port = Pose(torso=(-6, 20, 4), head=(-7, -16, 4),
                arms=((77, -38, -14), (58, 24, 8)))
    insert = Pose(torso=(-7, 22, 4), head=(-9, -16, 4),
                  arms=((76, -37, -14), (64, 23, 8)))
    return staged([
        (0, RIFLE),
        (.12, Pose(torso=(-4, 25, 4), head=(1, -22, 4),
                   arms=((83, -42, -11), (73, 15, 9)))),
        (.24, port),
        (.30, port),
        (.37, replace(port, arms=(port.arms[0], (31, 12, 14)))),
        (.52, Pose(torso=(-8, 13, 5), head=(-13, -14, 2),
                   arms=((74, -34, -14), (-5, 0, 15)))),
        (.62, Pose(torso=(-7, 15, 5), head=(-9, -15, 3),
                   arms=((74, -34, -14), (3, 1, 16)))),
        (.73, replace(insert, arms=(insert.arms[0], (44, 15, 11)))),
        (.80, insert),
        (.85, insert),
        (.91, replace(insert, arms=(insert.arms[0], (50, 18, 10)))),
        (.97, replace(insert, arms=((78, -37, -14), (70, 24, 8)))),
        (1.05, Pose(torso=(-5, 27, 3), head=(.5, -23, 4),
                    arms=((84, -42, -10), (76, 13, 9)))),
        (1.20, RIFLE),
    ])


# The draw opens the shoulders. The left wrist advances as the right moves
# back past the camera-facing rim of the helmet; both elbows remain rigid.
BOW_FULL = Pose(torso=(-2, 68, -2), head=(2, -66, 1),
                arms=((99, -50, -4), (94, -37, 5)))


def bow_draw():
    return staged([
        (0, Pose(arms=((4, 0, -3), (10, 0, 5)))),
        (.10, Pose(torso=(-5, 8, 1), head=(5, -7, 1),
                   arms=((27, -16, -6), (39, 15, 6)))),
        (.23, Pose(torso=(-4, 19, 2), head=(4, -17, 1),
                   arms=((77, -46, -7), (83, 6, 7)))),
        (.30, Pose(torso=(-4, 25, 1), head=(4, -23, 1),
                   arms=((83, -47, -8), (86, -4, 6)))),
        (.49, Pose(torso=(-2, 64, -2), head=(2, -60, 1),
                   arms=((97, -49, -5), (93, -33, 5)))),
        (.56, BOW_FULL),
        (.60, BOW_FULL),
    ])


def bow_release():
    return staged([
        (0, BOW_FULL),
        (.04, Pose(torso=(-1, 72, -3), head=(1, -68, 1),
                   arms=((101, -38, -5), (94, -39, 5)))),
        (.07, Pose(torso=(-1, 72, -3), head=(1, -69, 1),
                   arms=((102, -31, -5), (93, -38, 5)))),
        (.13, Pose(torso=(-1, 57, -1), head=(2, -56, 1),
                   arms=((77, -34, -14), (77, -30, 8)))),
        (.23, Pose(torso=(1, 14, 1), head=(-.5, -17, 0),
                   arms=((19, -10, -8), (28, -14, 8)))),
        (.30, Pose(arms=((4, 0, -3), (10, 0, 5)))),
    ])


def crossbow_reload():
    low = Pose(torso=(-11, 16, 2), head=(-13, -11, 1),
               arms=((61, -34, -10), (66, 17, 6)))
    locked = Pose(torso=(-6, 40, 1), head=(-6, -33, 1),
                  arms=((60, -49, -14), (69, 1, 6)))
    return staged([
        (0, Pose(torso=(-2, 8, 1), head=(2, -7, 0),
                 arms=((36, -15, -6), (45, 15, 7)))),
        (.18, Pose(torso=(-4, 19, 3), head=(-3, -13, 1),
                   arms=((78, -40, -7), (84, 12, 7)))),
        (.34, low),
        (.44, low),
        (.63, Pose(torso=(-10, 24, 2), head=(-12, -20, 1),
                   arms=((60, -37, -11), (67, 12, 6)))),
        (.81, Pose(torso=(-8, 36, 1), head=(-8, -30, 1),
                   arms=((61, -47, -14), (68, 3, 6)))),
        (.90, locked),
        (.95, locked),
        (1.10, RIFLE),
    ])


def spear_throw():
    return staged([
        (0, Pose(arms=((6, 0, -4), (2, 0, 4)))),
        (.09, Pose(shift=(-.4, .45), torso=(3, 12, 2), head=(-1, -6, 0),
                   arms=((54, 15, -21), (23, -4, 13)))),
        (.19, Pose(shift=(-.7, 1.1), torso=(7, 38, 5), head=(-5, -33, -2),
                   arms=((160, -11, 15), (64, -39, 10)))),
        (.23, Pose(shift=(-.7, 1.1), torso=(5, 35, 4), head=(-4, -32, -2),
                   arms=((166, -14, 17), (59, -40, 10)))),
        (.27, Pose(shift=(-.2, -.5), torso=(-4, 5, -1), head=(3, -7, 1),
                   arms=((130, 10, 10), (33, -20, 14)))),
        (.30, Pose(shift=(.25, -2.1), torso=(-11, -21, -4), head=(9, 16, 2),
                   arms=((100, 10, -7), (-4, -6, 17)))),
        (.35, Pose(shift=(.45, -2.5), torso=(-14, -31, -4), head=(10, 24, 2),
                   arms=((72, 6, -5), (-15, 2, 17)))),
        (.44, Pose(shift=(.3, -1.6), torso=(-8, -19, -2), head=(6, 16, 1),
                   arms=((38, -6, -6), (-7, 2, 10)))),
        (.53, Pose(shift=(0, -.4), torso=(.5, -3, 0), head=(.5, 6, 0),
                   arms=((8, -1, -4), (4, 0, 6)))),
        (.60, Pose(arms=((3, 0, -3), (2, 0, 4)))),
    ])


def build_ranged():
    return {
        "aim_rifle": bake(2, lambda t: breathing(RIFLE, t)),
        "aim_pistol": bake(2, lambda t: breathing(PISTOL, t, pistol=True)),
        "recoil_light": recoil(False),
        "recoil_heavy": recoil(True),
        "reload": reload_rifle(),
        "bow_draw": bow_draw(),
        "bow_release": bow_release(),
        "crossbow_reload": crossbow_reload(),
        "spear_throw": spear_throw(),
    }
