"""Key poses for the wingman's nine AN1 close-combat clips, facing -Z."""
from dataclasses import replace

from an1_locomotion import Pose, staged


def combo():
    # The hand travels from the suit's outside shoulder, across the visor, and
    # down the far side. The end of each cut is the wind-up for its successor.
    ready = Pose(arms=((15, 0, -4), (11, 0, 4)))
    draw = Pose(torso=(4, -23, 4), head=(-2, 17, -3), shift=(-.45, .4),
                arms=((137, 35, 16), (33, 14, 13)))
    slash = Pose(torso=(-11, -4, -6), head=(6, 3, 4), shift=(.4, -.8),
                 arms=((76, -42, 8), (37, 4, 22)))
    across = Pose(torso=(-6, -14, -4), head=(5, 11, 4), shift=(.28, -.35),
                  arms=((50, -48, 7), (20, 7, 13)))
    backhand = Pose(torso=(-7, 25, 4), head=(4, -16, -3), shift=(-.52, -.6),
                    arms=((92, 38, -11), (39, 22, 9)))
    open_side = Pose(torso=(1, 18, 3), head=(0, -14, -2), shift=(-.2, -.2),
                     arms=((99, 49, -9), (24, 13, 8)))
    high = Pose(torso=(8, 43, 5), head=(-4, -28, -4), shift=(-.52, .6),
                arms=((148, 6, 9), (51, 16, 13)))
    finish = Pose(torso=(-16, -15, -7), head=(7, 9, 3), shift=(.55, -1.25),
                  arms=((66, -19, 8), (18, 6, 24)))
    return {
        "melee_1": staged([(0, ready), (.07, draw),
                           (.10, replace(draw, torso=(0, -12, 2), arms=((128, 26, 17), (35, 16, 15)))),
                           (.15, slash), (.21, slash),
                           (.28, replace(across, torso=(-12, -21, -5), arms=((42, -53, 9), (22, 6, 18)))),
                           (.45, across)]),
        "melee_2": staged([(0, across),
                           (.07, replace(across, torso=(-3, -28, -6), arms=((73, -57, 4), (35, 12, 15)))),
                           (.10, replace(across, torso=(-4, -3, -2), arms=((87, -40, 3), (37, 15, 13)))),
                           (.15, backhand), (.21, backhand),
                           (.29, replace(open_side, torso=(-3, 32, 4), arms=((99, 57, -16), (33, 17, 12)))),
                           (.45, open_side)]),
        "melee_3": staged([(0, open_side), (.11, high), (.16, high),
                           (.19, replace(high, torso=(0, 11, 3), arms=((144, 4, 7), (46, 16, 14)))),
                           (.25, finish), (.36, finish),
                           (.44, replace(finish, torso=(-18, -21, -5), arms=((47, -23, 8), (13, 3, 18)))),
                           (.55, replace(ready, torso=(-5, -6, -2), arms=((31, -12, 1), (16, 3, 9)))),
                           (.65, ready)]),
    }


def raised_shoulders(clip, keys):
    # A small shoulder lift and forward slide keeps the raised gloves above
    # the brow and clear of the helmet without adding elbows to the rig.
    for arm in ("right_arm", "left_arm"):
        clip[arm]["position"] = {t: (0, up, forward) for t, up, forward in keys}
    return clip


def heavy():
    # Hands converge in front of the brow, leaving room between the palms for
    # held items with different grip lengths. The weight settles on both boots.
    carry = Pose(legs=(13, -16), leg_roll=(-4, 5), compress=.5,
                 torso=(-4, 0, 0), arms=((40, -13, -3), (47, 17, 4)))
    crown = Pose(legs=(32, -34), leg_roll=(-5, 6), compress=.9, lean=4,
                 torso=(5, 3, 1), head=(-11, -2, 0), arms=((151, -22, -7), (156, 25, 6)),
                 shift=(0, .38))
    drive = Pose(legs=(61, -60), leg_roll=(-5, 6), compress=1.25, lean=-24,
                 torso=(-12, -3, 1), head=(18, 2, -1), arms=((80, -18, 4), (85, 21, -3)),
                 shift=(.13, -1.05))
    clip = staged([
        (0, carry),
        (.13, replace(carry, legs=(24, -27), compress=.78, torso=(-9, -2, 1),
                      arms=((50, -18, 2), (55, 21, -1)))),
        (.27, crown), (.44, crown),
        (.48, replace(crown, legs=(38, -40), lean=-8, torso=(1, 2, 0), head=(-5, -2, 0))),
        (.55, drive), (.67, drive),
        (.76, replace(drive, legs=(51, -52), compress=.8, lean=-21, torso=(-10, -2, 1),
                      arms=((72, -18, 3), (77, 22, -2)))),
        (.88, replace(carry, legs=(24, -26), lean=-5, arms=((54, -15, 0), (60, 19, 1)))),
        (1.0, carry),
    ])
    return raised_shoulders(clip, [(0, 0, 0), (.13, .05, -.05), (.24, 1.25, -1.5),
                                   (.27, 1.55, -1.75), (.44, 1.55, -1.75),
                                   (.48, 1.35, -1.65), (.55, 0, 0), (1., 0, 0)])


def kick():
    # The left boot remains the load-bearing foot. A short suspension of the
    # attacking boot reads as a leg chamber with no knee joint on this rig.
    brace = Pose(legs=(9, -8), arms=((19, -4, -4), (28, 4, 6)))
    wind = Pose(legs=(26, -13), leg_roll=(-3, 2), feet=(2.05, 0), shift=(.72, .35),
                torso=(4, 10, -3), head=(-3, -8, 2), arms=((49, -6, -7), (38, 5, 8)))
    thrust = Pose(legs=(97, -16), leg_roll=(-2, 3), feet=(11.55, 0), shift=(.45, -.55),
                  torso=(11, -5, -3), head=(-7, 4, 2), arms=((-4, -8, -22), (48, 10, 19)))
    return staged([
        (0, brace), (.08, wind),
        (.14, replace(wind, legs=(41, -16), feet=(2.9, 0), torso=(8, 9, -4))),
        (.19, replace(thrust, legs=(61, -17), feet=(4.6, 0), arms=((17, -6, -13), (46, 8, 15)))),
        (.25, thrust), (.31, thrust),
        (.36, replace(thrust, legs=(59, -17), feet=(4.65, 0), torso=(7, -3, -3))),
        (.43, replace(wind, legs=(23, -12), feet=(1.25, 0), torso=(1, 4, -2))),
        (.49, replace(brace, legs=(12, -11), compress=.3, shift=(.2, 0))),
        (.55, brace),
    ])


def dash():
    guard = Pose(torso=(-3, 2, 0), arms=((31, 0, -5), (26, 3, 6)))
    load = Pose(torso=(5, 27, 4), head=(-2, -18, -3), shift=(-.4, 1.2),
                arms=((11, 22, -14), (53, 14, 14)))
    line = Pose(torso=(-15, -12, -2), head=(10, 10, 1), shift=(.25, -8.4),
                arms=((104, 11, -1), (-25, 1, 16)))
    return staged([
        (0, guard), (.10, load),
        (.14, replace(load, torso=(-5, 11, 1), shift=(0, -2), arms=((54, 10, -7), (41, 9, 15)))),
        (.20, line), (.27, line),
        (.32, replace(line, shift=(.25, -8.8), arms=((98, 12, -2), (-29, -2, 20)))),
        (.42, replace(guard, torso=(-7, -5, -1), shift=(.1, -3.2), arms=((69, 5, -4), (4, 1, 11)))),
        (.50, guard),
    ])


def slam():
    # Entity fall supplies the actual height. A nearly still downward guard
    # allows landing at any loop phase; the seam is the exact impact entry.
    suspended = Pose(legs=(8, -9), leg_roll=(-1, 1), lean=-5, torso=(2, -2, 0),
                     head=(-9, 2, 0), arms=((154, -22, -7), (151, 27, 6)))
    top = replace(suspended, legs=(9, -10), feet=(.68, .63), lean=-4,
                  head=(-8, 2, 1), arms=((156, -22, -7), (153, 27, 6)))
    crater = Pose(legs=(69, -72), leg_roll=(-5, 5), compress=1.35, lean=-38,
                  torso=(-13, -3, 1), head=(25, 3, -2), arms=((66, -16, 2), (72, 21, -2)),
                  shift=(.12, -.62))
    rest = Pose(legs=(12, -14), arms=((29, -7, -4), (27, 9, 5)))
    return {
        "slam_fall": raised_shoulders(staged([(0, suspended), (.18, top), (.4, suspended)]),
                                      [(0, 1.6, -1.8), (.18, 1.7, -1.85), (.4, 1.6, -1.8)]),
        "slam_impact": raised_shoulders(staged([
            (0, suspended),
            (.02, replace(crater, legs=(32, -34), compress=.7, lean=-18, torso=(-7, -3, 1),
                          head=(6, 3, -1), arms=((110, -16, 0), (111, 22, 1)))),
            (.05, crater), (.19, crater),
            (.29, replace(crater, legs=(60, -62), compress=.95, lean=-31, head=(26, 3, -2),
                          arms=((61, -16, 2), (67, 20, -2)))),
            (.44, replace(rest, legs=(36, -38), compress=.7, lean=-12, torso=(-6, -1, 0),
                          head=(12, 2, -1), arms=((54, -12, 0), (59, 15, 1)))),
            (.59, replace(rest, legs=(10, -12), lean=2, arms=((22, -6, -5), (20, 7, 6)))),
            (.70, rest),
        ]), [(0, 1.6, -1.8), (.02, .6, -.8), (.05, 0, 0), (.7, 0, 0)]),
    }


def shove():
    stand = Pose(legs=(9, -10), arms=((24, -4, -6), (32, 5, 7)))
    wind = Pose(legs=(26, -27), leg_roll=(-3, 4), compress=.75, shift=(.55, .7),
                torso=(0, 23, 4), head=(1, -14, -2), arms=((24, -1, -6), (46, 13, 13)))
    shoulder = Pose(legs=(31, -33), leg_roll=(-7, 6), compress=.65, lean=-14, shift=(-1.3, -2.2),
                    torso=(-9, -36, 8), head=(10, 22, -6), arms=((4, 7, -8), (27, 22, 18)))
    return staged([
        (0, stand), (.07, wind),
        (.10, replace(wind, lean=-4, shift=(-.2, -.3), torso=(-4, -4, 5))),
        (.15, shoulder), (.21, shoulder),
        (.26, replace(shoulder, lean=-12, shift=(-1.35, -2.38), arms=((-1, 8, -10), (23, 20, 17)))),
        (.34, replace(stand, legs=(17, -18), torso=(0, -8, 3), arms=((11, 2, -8), (26, 9, 11)))),
        (.4, stand),
    ])


def build_melee():
    return {**combo(), "melee_heavy": heavy(), "kick": kick(), "dash_stab": dash(),
            **slam(), "shove": shove()}
