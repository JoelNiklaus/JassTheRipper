import os
import sys

import numpy
from keras.callbacks import TensorBoard, EarlyStopping, ModelCheckpoint, History
from keras.engine.saving import load_model
from keras.optimizers import SGD, Adam
from tensorflow.contrib.learn.python.learn.estimators._sklearn import train_test_split

from export_model_checkpoint import ExportModelCheckpoint
from neural_networks import define_separate_model
from util import model_path, features_path, targets_path, weights_path, export_path, load_dataset, zero_pad, base_path, \
    shuffle_in_unison


def train(episode_padded, network_type):
    """
    pre_train setting: model is defined newly, trained and saved (overwrites possible older models)
    self_play setting: model is loaded from self_play and if not available there from pre_train.


    Loads an existing model or existing weights if available
    and then trains the model with the data available in the directory based on the train mode and the network type.
    Saves the best model and weights during training based on the validation loss

    :param episode_padded:  0 (pre_train), 1..n (self_play)
    :param network_type:    cards or score
    """

    episode_number = int(episode_padded)
    if episode_number < 0:
        print("\nPlease enter an episode number >= 0!")
        return
    elif episode_number == 0:  # pre_train setting: overwrite possible existing models
        saved_model_path = model_path(episode_padded, network_type)
        if os.path.exists(saved_model_path):
            print("\nNo need to train. Saved model found in " + saved_model_path)
            return
        else:
            model = define_separate_model(network_type)
            # Try also: 'mape', 'kullback_leibler_divergence', 'categorical_crossentropy', 'acc'
            optimizer = SGD(lr=1e-2, momentum=0.9, decay=1e-6, nesterov=True)
            # optimizer = Adam()
            model.compile(loss='mse', optimizer=optimizer,
                          metrics=['mae'])  # Reason for mse: big errors should be punished!
            print("\nCompiled new model")
    else:  # self_play setting: loading existing model of the previous episode and saving to current episode
        saved_model_path = model_path(zero_pad(episode_number - 1), network_type)
        if not os.path.exists(saved_model_path):
            print("\nNo saved model found in " + saved_model_path)
            return
        else:
            model = load_model(saved_model_path)
            print("\nLoaded existing model from " + saved_model_path)

        # if os.path.exists(weights_path(episode_number, network_type)):
        #    model = model.load_weights(weights_path(episode_number, network_type))
        #    print("\nLoaded existing weights from " + weights_path(episode_number, network_type))

    print("Loading data...")

    x_train = load_dataset(episode_number, network_type, features_path)
    y_train = load_dataset(episode_number, network_type, targets_path)
    # First two games of the dataset are used as test set.
    # Can only be used in pre-training, in self-play the data has already been seen in previous epochs during training!
    size = 36 * 24 * 2  # cards x data augmentation multiplier * fair tournament mode multiplier
    x_test = x_train[:size - 1]
    y_test = y_train[:size - 1]
    x_train = x_train[size:]
    y_train = y_train[size:]
    shuffle_in_unison(x_train, y_train)
    shuffle_in_unison(x_test, y_test)

    print("Training...")

    h = History()
    tb = TensorBoard(log_dir='./Graph', write_images=True)
    es = EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)
    mc = ModelCheckpoint(model_path(episode_padded, network_type), save_best_only=True, save_weights_only=False,
                         verbose=1)
    wc = ModelCheckpoint(weights_path(episode_padded, network_type), save_best_only=True, save_weights_only=True,
                         verbose=1)
    emc = ExportModelCheckpoint(export_path(episode_padded, network_type), save_best_only=True, verbose=1)

    history = model.fit(x_train, y_train, epochs=99, batch_size=32, validation_split=0.1,
                        callbacks=[h, tb, es, mc, wc, emc])

    min_val_loss = min(history.history['val_loss'])
    print(min_val_loss, file=open(base_path() + "min_val_loss.txt", "w"))  # This file is then read in the Java code

    test_loss = model.evaluate(x_test, y_test)[0]
    print(test_loss, file=open(base_path() + "test_loss.txt", "w"))  # This file is then read in the Java code

    numpy.set_printoptions(threshold=sys.maxsize)  # so that the print output is not truncated
    print(model.predict(numpy.expand_dims(x_test[0], axis=0)))
    print(y_test[0])


if __name__ == '__main__':
    train(sys.argv[1], sys.argv[2])
