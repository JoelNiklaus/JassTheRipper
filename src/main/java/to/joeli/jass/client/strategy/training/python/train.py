import os
import sys

from keras.callbacks import TensorBoard, EarlyStopping, ModelCheckpoint
from keras.engine.saving import load_model

from export_model_checkpoint import ExportModelCheckpoint
from neural_networks import define_separate_model
from util import load_all_cbor_files, model_path, features_path, targets_path, weights_path, export_path


def train(episode_number, network_type):
    """
    pre_train setting: model is defined newly, trained and saved (overwrites possible older models)
    self_play setting: model is loaded from self_play and if not available there from pre_train.


    Loads an existing model or existing weights if available
    and then trains the model with the data available in the directory based on the train mode and the network type.
    Saves the best model and weights during training based on the validation loss

    :param episode_number:  0 (pre_train), 1..n (self_play)
    :param network_type:    cards or score
    """

    if int(episode_number) < 0:
        print("\nPlease enter an episode number >= 0!")
        return
    elif int(episode_number) == 0:  # pre_train setting: overwrite possible existing models
        saved_model_path = model_path(episode_number, network_type)
        if os.path.exists(saved_model_path):
            print("\nNo need to train. Saved model found in " + saved_model_path)
            return
        else:
            model = define_separate_model(network_type)
            # Try also: 'mape', 'kullback_leibler_divergence', 'categorical_crossentropy', 'acc'
            model.compile(loss='mae', optimizer='adam', metrics=['mse'])
            print("\nCompiled new model")
    else:  # self_play setting: loading existing model of the previous episode and saving to current episode
        saved_model_path = model_path(f'{(int(episode_number) - 1):04}', network_type)
        if not os.path.exists(saved_model_path):
            print("\nNo saved model found in " + saved_model_path)
            return
        else:
            model = load_model(saved_model_path)
            print("\nLoaded existing model from " + saved_model_path)

        # if os.path.exists(weights_path(episode_number, network_type)):
        #    model = model.load_weights(weights_path(episode_number, network_type))
        #    print("\nLoaded existing weights from " + weights_path(episode_number, network_type))

    features = load_all_cbor_files(features_path(episode_number, network_type))
    targets = load_all_cbor_files(targets_path(episode_number, network_type))

    tb = TensorBoard(log_dir='./Graph', write_images=True)
    es = EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)
    mc = ModelCheckpoint(model_path(episode_number, network_type), save_best_only=True, save_weights_only=False,
                         verbose=1)
    wc = ModelCheckpoint(weights_path(episode_number, network_type), save_best_only=True, save_weights_only=True,
                         verbose=1)
    emc = ExportModelCheckpoint(export_path(episode_number, network_type), save_best_only=True, verbose=1)

    model.fit(features, targets, epochs=99, batch_size=32, validation_split=0.1, callbacks=[tb, es, mc, wc, emc])


if __name__ == '__main__':
    # train("pre_train/", "score/")
    train(sys.argv[1], sys.argv[2])
