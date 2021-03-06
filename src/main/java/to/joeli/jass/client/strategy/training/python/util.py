import os
from os.path import isfile, join
from pathlib import Path

import cbor
import numpy as np

# Determines how many episodes back the training data will be used
# Example: If it is 4: each experience (episode data) will be used 4 times.
# The bigger, the bigger the datasets are, and the longer the training takes
REPLAY_MEMORY_SIZE_FACTOR = 4  # 1, 2, 4, 8


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


def concat(combined_array, array):
    if combined_array is None:
        combined_array = array
    else:
        combined_array = np.concatenate((combined_array, array))
    return combined_array


def zero_pad(episode_number):
    return f'{episode_number:04}'


def load_all_cbor_files(path):
    combined = None
    for filename in get_file_names(path, '.cbor'):
        array = load_cbor(path + filename)
        combined = concat(combined, array)
    return combined


def load_dataset(episode_number, network_type, data_set_type, path_function):
    dataset = None
    for episode in range(max(episode_number - REPLAY_MEMORY_SIZE_FACTOR + 1, 0), episode_number + 1, 1):
        dataset = concat(dataset, load_all_cbor_files(path_function(zero_pad(episode), network_type, data_set_type)))
    return dataset


def shuffle_in_unison(a, b):
    """
    Shuffle so that the validation data is better selected (chosen from the end of the dataset)
    Be careful with future numpy version! This could maybe stop working!
    See https://stackoverflow.com/questions/4601373/better-way-to-shuffle-two-numpy-arrays-in-unison
    :param a:
    :param b:
    :return:
    """
    rng_state = np.random.get_state()
    np.random.shuffle(a)
    np.random.set_state(rng_state)
    np.random.shuffle(b)


"""
Folder structure:
test_loss.txt
min_val_loss.txt
experiment.log
episodes/
    ####/                           --> episode number: 0 -> pre_train, 1..n -> self_play
        cards/                      --> network_type
            train/                  --> data_set_type      
                features/
                targets/
                models/
                    export/
                    keras/
                        model.hdf5
                        weights.hdf5
            test/
                ...
            val/
                ...
        score/                      --> network_type
            ...
"""


def base_path():
    root_path = Path(os.path.realpath(__file__)).parent.parent.parent.parent.parent.parent.parent.parent.parent
    return str(root_path) + "/resources/"


def episode_path(episode_number):
    return base_path() + "episodes/" + episode_number + "/"


def network_type_path(episode_number, network_type):
    return episode_path(episode_number) + network_type


def data_set_type_path(episode_number, network_type, data_set_type):
    return network_type_path(episode_number, network_type) + data_set_type


def features_path(episode_number, network_type, data_set_type):
    return create_if_not_exists(data_set_type_path(episode_number, network_type, data_set_type) + "features/")


def targets_path(episode_number, network_type, data_set_type):
    return create_if_not_exists(data_set_type_path(episode_number, network_type, data_set_type) + "targets/")


def models_path(episode_number, network_type):
    return create_if_not_exists(network_type_path(episode_number, network_type) + "models/")


def export_path(episode_number, network_type):
    return create_if_not_exists(models_path(episode_number, network_type) + "export/")


def keras_path(episode_number, network_type):
    return create_if_not_exists(models_path(episode_number, network_type) + "keras/")


def model_path(episode_number, network_type):
    return keras_path(episode_number, network_type) + "model.hdf5"


def weights_path(episode_number, network_type):
    return keras_path(episode_number, network_type) + "weights.hdf5"
