from keras import Input, Model
from keras.layers import Dense, Reshape, Softmax, Dropout, GaussianNoise, Activation, BatchNormalization
from keras.regularizers import l2

num_neurons = 256  # TODO Try 32, 64, 128, 256, 512, 1024
dropout_rate = 0.4
activation = 'selu'  # 'elu' a bit slower in training


# l2 = l2(1e-4)


def input():
    return Input((73, 18,), name='input')


def hidden(inp):
    inp = Reshape((73 * 18,))(inp)
    hid = Dense(units=num_neurons, activation=activation)(inp)
    # hid = GaussianNoise(0.1)(hid)  # seems to have almost no effect
    hid = BatchNormalization()(hid)  # makes training much faster (ca. 3x) but error on test set a bit worse
    # hid = Activation(activation)(hid)
    hid = Dropout(dropout_rate)(hid)  # dropout can make the validation loss smaller than the training loss!
    # hid = Dense(units=num_neurons, activation=activation)(hid)
    # hid = Dropout(dropout_rate)(hid)
    return hid


def cards(hid):
    cards = Dense(units=36 * 4, activation=activation)(hid)
    # cards = Dropout(0.2)(cards)  # no dropout towards end of network
    # cards = BatchNormalization()(cards)
    cards = Reshape((36, 4))(cards)
    cards = Softmax(name='cards')(cards)
    return cards


def score(hid):
    score = Dense(units=1, activation='linear', name='score')(hid)
    return score


def define_cards_estimator_model():
    inp = input()
    hid = hidden(inp)
    out = cards(hid)

    model = Model(inp, out)

    model.summary()

    return model


def define_score_estimator_model():
    inp = input()
    hid = hidden(inp)
    out = score(hid)

    model = Model(inp, out)

    model.summary()

    return model


def define_separate_model(network_type):
    if network_type == "cards/":
        return define_cards_estimator_model()
    if network_type == "score/":
        return define_score_estimator_model()
    else:
        print("Please define a valid network type!")


def define_combined_model():
    # INFO: Set loss_weights accordingly during training
    # model.compile(loss=['mse', 'mse'], loss_weights=[0, 1], optimizer='adam', metrics=['mae'])

    inp = input()
    hid = hidden(inp)

    cards_out = cards(hid)
    score_out = score(hid)

    model = Model(inp, [cards_out, score_out])

    model.summary()

    return model
