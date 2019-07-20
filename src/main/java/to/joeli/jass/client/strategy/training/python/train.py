import os
import sys

from keras.callbacks import TensorBoard, EarlyStopping, ModelCheckpoint
from keras.engine.saving import load_model

from neural_networks import define_separate_model
from util import load_all_cbor_files, base_path

"""
The trainable model is in the RAM ready to predict and be trained 
The frozen model represents a checkpoint of the trainable model

Folder structure:
    pre_train/          --> train_mode
        cards/          --> network_type
            features/
            targets/
            model.hdf5
            weights.hdf5
        score/          --> network_type
            features/
            targets/
            model.hdf5
            weights.hdf5
    self_play/          --> train_mode
        cards/          --> network_type
            features/
            targets/
            model.hdf5
            weights.hdf5
        score/          --> network_type
            features/
            targets/
            model.hdf5
            weights.hdf5

"""


def train(train_mode, network_type):
    """
    Loads an existing model or existing weights if available
    and then trains the model with the data available in the directory based on the train mode and the network type.
    Saves the best model and weights during training based on the validation loss

    :param train_mode:      pre_train or self_play
    :param network_type:    cards or score
    """

    path = base_path + train_mode + network_type
    features_path = path + "features/"
    targets_path = path + "targets/"
    model_path = path + "model.hdf5"
    weights_path = path + "weights.hdf5"

    if os.path.exists(model_path):
        model = load_model(model_path)
        print("Loaded existing model from " + model_path)
    else:
        model = define_separate_model(network_type)
        # INFO: Set loss_weights accordingly during training
        # Try also: 'mape', 'kullback_leibler_divergence', 'categorical_crossentropy', 'acc'
        # model.compile(loss=['mse', 'mse'], loss_weights=[0, 1], optimizer='adam', metrics=['mae'])
        model.compile(loss='mae', optimizer='adam', metrics=['mse'])
        print("Compiled new model")
        if os.path.exists(weights_path):
            model = model.load_weights(weights_path)
            print("Loaded existing weights from " + weights_path)

    # features_path = dataset_path + train_mode + "features/"
    # targets_path = dataset_path + train_mode + "targets/"
    features = load_all_cbor_files(features_path)
    targets = load_all_cbor_files(targets_path)
    # cards_targets = load_all_cbor_files(targets_path + "cards/")
    # score_targets = load_all_cbor_files(targets_path + "score/")
    # print(features)
    # print(cards_targets)
    # print(score_targets)

    # if type == 'cards':
    #    model.loss_weights = [1, 0]
    # features = load_cbor_files(features_path + "cards")
    # if type == 'score':
    #    model.loss_weights = [0, 1]
    # features = load_cbor_files(features_path + "score")
    if train_mode == "pre_train/":
        epochs = 999
    if train_mode == "self_play/":
        epochs = 99

    print("Loss Weights")
    print(model.loss_weights)
    print(model.to_json())
    # features = numpy.random.random_sample((1, 73, 18))
    # print(numpy.ndarray.flatten(features))

    tb = TensorBoard(log_dir='./Graph', write_images=True)
    es = EarlyStopping(monitor='val_loss', patience=100, restore_best_weights=True)
    mc = ModelCheckpoint(model_path, save_best_only=True, save_weights_only=False, period=1)
    wc = ModelCheckpoint(weights_path, save_best_only=True, save_weights_only=True, period=1)

    # cards_targets = numpy.random.random_sample((1, 36, 4))
    # score_targets = numpy.random.random_sample((1, 1))
    model.fit(features, targets, epochs=epochs, verbose=1, batch_size=32, validation_split=0.1,
              callbacks=[tb, es, mc, wc])

    # cards, score = model.predict(features)
    # print(cards[0])
    # print(cards_targets[0])
    # print(score[0])
    # print(score_targets[0])

    # model.save(estimator_model_path)
    # model.save_weights(estimator_weights_path)


if __name__ == '__main__':
    train(sys.argv[1], sys.argv[2])
