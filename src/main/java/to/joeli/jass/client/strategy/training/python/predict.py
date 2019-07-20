import os
import sys

from keras.engine.saving import load_model

from util import base_path
from zeromq_server import init_socket, listen


def load_with_fallback(network_type):
    """
    Tries to load a model from the self_play directory.
    If it cannot find one there it tries to load one from the pre_train directory
    If it cannot find one there either it returns False. At this point a trained model should be available!

    :param network_type:
    :return:
    """
    self_play = load("self_play", network_type)
    if self_play:
        return self_play
    pre_train = load("pre_train", network_type)
    if pre_train:
        return pre_train
    return False


def load(train_mode, network_type):
    path = base_path + train_mode + "/" + network_type + "/"
    model_path = path + "model.hdf5"

    if os.path.exists(model_path):
        model = load_model(model_path)
        print("Loaded existing model from " + model_path)
        return model
    print("Could not load model from " + model_path)
    return False


if __name__ == '__main__':
    cards_estimator = load_with_fallback("cards")
    score_estimator = load_with_fallback("score")
    if not cards_estimator or not score_estimator:
        print("Could not load models", file=sys.stderr)
        exit(1)
    socket = init_socket()

    listen(socket, cards_estimator, score_estimator)
