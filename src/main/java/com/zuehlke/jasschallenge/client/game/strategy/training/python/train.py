import os
import sys

import numpy
from keras.callbacks import TensorBoard, EarlyStopping, ModelCheckpoint
from keras.engine.saving import load_model

from neural_networks import define_model
from util import estimator_model_path, estimator_weights_path, dataset_path, load_all_cbor_files

"""
Folder structure:
datasets/
    pre_train/
        features/
            cards/
            score/
        targets/
            cards/
            score/
    self_play/
        features/
            cards/
            score/
        targets/
            cards/
            score/
models/
    cards/
    score/


"""


def train(mode, type):
    """

    :param mode:    pre_train or self_play
    :param type:    cards or score
    """

    if os.path.exists(estimator_model_path):
        model = load_model(estimator_model_path)
        print("Loaded existing model from " + estimator_model_path)
    # if os.path.exists(estimator_weights_path):
    #    model = load_model(estimator_weights_path)
    else:
        model = define_model()
        # INFO: Set loss_weights accordingly during training
        model.compile(loss=['mse', 'mse'], loss_weights=[0, 1], optimizer='adam',
                      metrics=['mae']) # 'mape', 'kullback_leibler_divergence', 'categorical_crossentropy', 'acc'
        print("Compiled new model")

    # TODO do we really have the same features for card and score estimation?
    features_path = dataset_path + mode + "features/"
    targets_path = dataset_path + mode + "targets/"
    features = load_all_cbor_files(features_path)
    cards_targets = load_all_cbor_files(targets_path + "cards/")
    score_targets = load_all_cbor_files(targets_path + "score/")
    # print(features)
    # print(cards_targets)
    # print(score_targets)

    # TODO score loss not improving

    #if type == 'cards':
    #    model.loss_weights = [1, 0]
        # features = load_cbor_files(features_path + "cards")
    #if type == 'score':
    #    model.loss_weights = [0, 1]
        # features = load_cbor_files(features_path + "score")
    if mode == "pre_train/":
        epochs = 999
    if mode == "self_play/":
        epochs = 99

    print("Loss Weights")
    print(model.loss_weights)
    print(model.to_json())
    # features = numpy.random.random_sample((1, 73, 18))
    # print(numpy.ndarray.flatten(features))

    tb = TensorBoard(log_dir='./Graph', write_images=True)
    es = EarlyStopping(monitor='val_loss', patience=500, restore_best_weights=True)
    mc = ModelCheckpoint(estimator_model_path, save_best_only=True, period=1)

    # cards_targets = numpy.random.random_sample((1, 36, 4))
    # score_targets = numpy.random.random_sample((1, 1))
    model.fit(features, [cards_targets, score_targets], epochs=epochs, verbose=1, batch_size=32, validation_split=0.1,
              callbacks=[tb, es, mc])

    cards, score = model.predict(features)
    print(cards[0])
    print(cards_targets[0])
    print(score[0])
    print(score_targets[0])

    # model.save(estimator_model_path)
    # model.save_weights(estimator_weights_path)


if __name__ == '__main__':
    train(sys.argv[1], sys.argv[2])
