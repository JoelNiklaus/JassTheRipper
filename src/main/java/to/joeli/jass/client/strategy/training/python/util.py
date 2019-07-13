import json
import os
import sys
from os.path import isfile, join
from pathlib import Path

import cbor
import numpy as np


def create_if_not_exists(path):
    if not os.path.exists(path):
        os.makedirs(path)


def load_json(path):
    with open(path, 'r') as file:
        return np.array(json.loads(file.read()))


def load_cbor(path):
    with open(path, 'rb') as file:
        return np.array(cbor.loads(file.read()))


def load_npy(path):
    return np.load(path)


def get_file_names(path, extension):
    return [f for f in os.listdir(path) if isfile(join(path, f)) and os.path.splitext(f)[1] == extension]


def load_all_npy_files(path):
    combined_array = None
    for filename in get_file_names(path, '.npy'):
        array = load_npy(path + filename)
        if combined_array is None:
            combined_array = array
        else:
            combined_array = np.concatenate((combined_array, array))
    return combined_array


def load_all_cbor_files(path):
    combined_array = None
    for filename in get_file_names(path, '.cbor'):
        array = load_cbor(path + filename)
        if combined_array is None:
            combined_array = array
        else:
            combined_array = np.concatenate((combined_array, array))
    return combined_array


root_path = Path(os.path.realpath(__file__)).parent.parent.parent.parent.parent.parent.parent.parent.parent

base_path = str(root_path) + "/resources/"

dataset_path = base_path + "datasets/"
# create_if_not_exists(dataset_path)

score_features_path = dataset_path + "score_features/"
cards_features_path = dataset_path + "cards_features/"
score_labels_path = dataset_path + "score_labels/"
cards_labels_path = dataset_path + "cards_labels/"

models_path = base_path + "models/"
estimator_model_path = models_path + "estimator_model.hdf5"
estimator_weights_path = models_path + "estimator_weights.hdf5"

score_estimator_path = models_path + 'score_estimator.hdf5'
cards_estimator_path = models_path + 'cards_estimator.hdf5'
# create_if_not_exists(models_path)

# labels = load_json(dataset_path + "labels.json")
# features = load_json(dataset_path + "features.json")
