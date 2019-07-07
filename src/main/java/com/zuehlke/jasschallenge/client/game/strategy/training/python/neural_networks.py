from keras import Sequential
from keras.layers import Dense, Reshape, Softmax

num_cards = 36
num_player_cards = 9
num_players = 4
three_hot_length = 14

def define_score_estimator_model():
    model = Sequential()
    model.add(Dense(units=128, activation='relu', input_dim=(0 + num_cards + num_players * num_player_cards) * three_hot_length))
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
