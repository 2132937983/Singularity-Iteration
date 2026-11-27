"""Build the quantum and nano player's shared suit animation JSON."""
from an1_suit_flight import build_flight
from an1_suit_gestures import build_gestures
from an1_wingman import write_runtime


if __name__ == "__main__":
    write_runtime("suit", {**build_flight(), **build_gestures()})
