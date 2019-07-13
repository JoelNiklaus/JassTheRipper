import numpy
from keras import Sequential, Input, Model
from keras.layers import Dense, Reshape, Softmax, Dropout

num_cards = 36
num_player_cards = 9
num_players = 4
three_hot_length = 14

num_neurons = 128  # TODO Try 64, 128, 256, 512, 1024


def define_score_estimator_model():
    model = Sequential()
    model.add(Dense(units=128, activation='relu',
                    input_dim=(0 + num_cards + num_players * num_player_cards) * three_hot_length))
    model.add(Dense(units=128, activation='relu'))
    model.add(Dense(units=128, activation='relu'))
    model.add(Dense(units=1, activation='sigmoid'))
    return model


def define_cards_estimator_model():
    model = Sequential()
    model.add(Dense(units=128, activation='relu', input_dim=(0 + num_cards + num_player_cards) * three_hot_length))
    model.add(Dense(units=128, activation='relu'))
    model.add(Dense(units=128, activation='relu'))
    model.add(Dense(units=36 * 4))
    model.add(Reshape((36, 4)))
    model.add(Softmax())
    return model


def define_model():
    inp = Input((73, 18,))

    # finetune architecture
    hid = Reshape((73 * 18,))(inp)
    hid = Dense(units=num_neurons, activation='relu')(hid)
    #hid = Dropout(0.2)(hid)
    hid = Dense(units=num_neurons, activation='relu')(hid)
    #hid = Dropout(0.2)(hid)
    hid = Dense(units=num_neurons, activation='relu')(hid)
    # TODO try dropout, regularization, different number of hidden layers etc.

    cards_out = Dense(units=36 * 4)(hid)
    cards_out = Reshape((36, 4))(cards_out)
    cards_out = Softmax(name='cards_out')(cards_out)

    score_out = Dense(units=1, activation='linear', name='score_out')(hid)

    model = Model(inp, [cards_out, score_out])

    model.summary()

    return model



