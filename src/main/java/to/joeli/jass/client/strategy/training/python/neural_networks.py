from keras import Input, Model
from keras.layers import Dense, Reshape, Softmax, Dropout

num_neurons = 128  # TODO Try 64, 128, 256, 512, 1024


def define_score_estimator_model():
    inp = input()
    hid = hidden(inp)
    out = score(hid)

    model = Model(inp, out)

    model.summary()

    return model


def define_cards_estimator_model():
    inp = input()
    hid = hidden(inp)
    out = cards(hid)

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


def score(hid):
    score = Dense(units=1, activation='linear', name='score')(hid)
    return score


def cards(hid):
    cards = Dense(units=36 * 4)(hid)
    cards = Reshape((36, 4))(cards)
    cards = Softmax(name='cards')(cards)
    return cards


def hidden(inp):
    inp = Reshape((73 * 18,))(inp)
    # finetune architecture
    hid = Dense(units=num_neurons, activation='relu')(inp)
    # hid = Dropout(0.2)(hid)
    hid = Dense(units=num_neurons, activation='relu')(hid)
    # hid = Dropout(0.2)(hid)
    hid = Dense(units=num_neurons, activation='relu')(hid)
    # TODO try dropout, regularization, different number of hidden layers etc.
    return hid


def input():
    return Input((73, 18,), name='input')
