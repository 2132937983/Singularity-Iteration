"""Target acquisition, cover movement and gestures for the wingman's rigid rig."""
from dataclasses import replace

from an1_locomotion import Pose, cover, staged


def peek(side):
    # +X is the actor's left. Keep the existing fore/aft stance and roll the
    # straight legs back against the short sideways hip shift to plant the feet.
    start = cover(0)
    def leaning(travel, torso, head, arms):
        return replace(start, shift=(start.shift[0] + 1.45*side*travel, start.shift[1]),
                       leg_roll=tuple(a - 12*side*travel for a in start.leg_roll),
                       torso=torso, head=head, arms=arms)

    aim = ((100, -12, -3), (96, 38, 4))
    clip = staged([
        (0, start),
        (.06, leaning(.05, (-12, -10, 2 - .5*side), (8, 8 - 7*side, -1 - 4*side), start.arms)),
        (.16, leaning(.34, (-12, -9, 2 - 6*side), (8, 8 - 10*side, -1 - 8*side),
                      ((83, -7, -3), (86, 24, 6)))),
        (.28, leaning(.84, (-11, -8, 1 - 13*side), (9, 7 - 5*side, 4*side),
                      ((95, -10, -3), (93, 34, 5)))),
        (.35, leaning(1, (-10, -8, -16*side), (9, 7, 8*side), aim)),
        (.4, leaning(1, (-10, -8, -16*side), (9, 8, 9*side), aim)),
    ])
    # The support glove reaches past the trigger glove. The small shoulder
    # travel keeps the hand spacing visible in profile on the rigid-arm rig.
    for arm, offset in (("right_arm", (.65, 0, 1.25)), ("left_arm", (-.65, 0, -2))):
        clip[arm]["position"] = {t: tuple(c * amount for c in offset)
                                 for t, amount in ((0, 0), (.06, 0), (.16, .3), (.28, .85),
                                                   (.35, 1), (.4, 1))}
    return clip


def tracks():
    rest = Pose()
    lock = Pose(torso=(-2, 0, 0), head=(0, 0, 0), arms=((4, 0, -3), (4, 0, 3)))
    glare = staged([
        (0, Pose(torso=(-1, 8, 1), head=(1, -28, -3))),
        (.04, Pose(torso=(-1, 8, 1), head=(0, -30, -3))),
        (.09, Pose(torso=(-2, 7, 1), head=(-2, -5, 1))),
        (.13, Pose(torso=(-3, 3, 0), head=(-2, -2, 0), arms=((3, 0, -3), (2, 0, 3)))),
        (.19, lock), (.30, lock),
    ])
    intercept_hold = Pose(torso=(-4, -8, 4), head=(8, 8, -3),
                          arms=((116, -9, -6), (-6, 1, 11)))
    intercept = staged([
        (0, rest),
        (.03, Pose(torso=(-1, 2, 0), head=(6, -4, 0), arms=((-5, 0, -6), (0, 0, 3)))),
        (.07, Pose(torso=(-3, -5, 3), head=(8, 5, -2), arms=((71, -7, -8), (-4, 0, 7)))),
        (.10, intercept_hold), (.14, intercept_hold),
        (.18, Pose(torso=(-1, -6, 3), head=(7, 6, -2), arms=((122, -7, -6), (-7, 1, 10)))),
        (.24, Pose(torso=(1, -3, 1), head=(4, 3, -1), arms=((55, -4, -6), (-3, 0, 6)))),
        (.30, rest),
    ])
    carry = Pose(torso=(-2, 5, 0), head=(1, -5, 0), arms=((21, -9, -10), (3, 0, 4)))
    finish = Pose(torso=(-3, -8, 0), head=(1, 7, 0), arms=((14, 15, -29), (0, 0, 5)))
    flourish = staged([
        (0, rest), (.17, carry),
        (.28, Pose(torso=(-3, 8, 1), head=(1, -7, -1), arms=((35, -19, -13), (3, 0, 5)))),
        (.38, finish), (.45, finish),
        (.60, Pose(torso=(-2, -2, 0), head=(0, 2, 0), arms=((16, 8, -16), (2, 0, 4)))),
        (.74, Pose(torso=(-1, 0, 0), head=(-10, 1, 0), arms=((9, 2, -7), (1, 0, 3)))),
        (.84, Pose(torso=(-1, 0, 0), head=(-10, 0, 0), arms=((7, 1, -5), (1, 0, 3)))),
        (1.02, Pose(head=(1, 0, 0), arms=((3, 0, -3), (0, 0, 2)))), (1.20, rest),
    ])
    return {"glare": glare, "peek_left": peek(1), "peek_right": peek(-1),
            "intercept": intercept, "kill_flourish": flourish}
