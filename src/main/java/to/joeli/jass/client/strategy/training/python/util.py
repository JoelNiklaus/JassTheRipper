import os
from os.path import isfile, join
from pathlib import Path

import cbor
import numpy as np


def create_if_not_exists(path):
    # INFO: This somehow causes problems on the server!
    if not os.path.exists(path):
        os.makedirs(path)
    return path


def load_cbor(path):
    with open(path, 'rb') as file:
        return read_cbor(file.read())


def read_cbor(string):
    string = cbor.loads(string)
    return np.array(string)


def get_file_names(path, extension):
    return [f for f in os.listdir(path) if isfile(join(path, f)) and os.path.splitext(f)[1] == extension]


def load_all_cbor_files(path):
    combined_array = None
    for filename in get_file_names(path, '.cbor'):
        array = load_cbor(path + filename)
        if combined_array is None:
            combined_array = array
        else:
            combined_array = np.concatenate((combined_array, array))
    return combined_array


"""
Folder structure:
    ####/                   --> episode number: 0 -> pre_train, 1..n -> self_play
        cards/                      --> network_type
            features/
            targets/
            models/
                export/
                keras/
                    model.hdf5
                    weights.hdf5
        score/                      --> network_type
            features/
            targets/
            models/
                export/
                keras/
                    model.hdf5
                    weights.hdf5
"""


def path(episode_number, network_type):
    root_path = Path(os.path.realpath(__file__)).parent.parent.parent.parent.parent.parent.parent.parent.parent
    base_path = str(root_path) + "/resources/"
    return base_path + episode_number + "/" + network_type


def features_path(episode_number, network_type):
    return create_if_not_exists(path(episode_number, network_type) + "features/")


def targets_path(episode_number, network_type):
    return create_if_not_exists(path(episode_number, network_type) + "targets/")


def models_path(episode_number, network_type):
    return create_if_not_exists(path(episode_number, network_type) + "models/")


def export_path(episode_number, network_type):
    return create_if_not_exists(models_path(episode_number, network_type) + "export/")


def keras_path(episode_number, network_type):
    return create_if_not_exists(models_path(episode_number, network_type) + "keras/")


def model_path(episode_number, network_type):
    return keras_path(episode_number, network_type) + "model.hdf5"


def weights_path(episode_number, network_type):
    return keras_path(episode_number, network_type) + "weights.hdf5"
