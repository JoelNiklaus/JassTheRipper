import json
import os
from os.path import isfile, join
from pathlib import Path

from keras.callbacks import TensorBoard, EarlyStopping
from keras.models import Sequential
from keras.layers import Dense
import numpy as np
from tensorflow.contrib.learn.python.learn.estimators._sklearn import train_test_split


def createIfNotExists(path):
    if not os.path.exists(path):
        os.makedirs(path)


def load_json(path):
    global labels
    with open(path, 'r') as file:
        return np.array(json.loads(file.read())['data'])


def get_file_names(path, extension):
    return [f for f in os.listdir(path) if isfile(join(path, f)) and os.path.splitext(f)[1] == extension]


def load_all_npy_files(path):
    combined_array = None
    for filename in get_file_names(path, '.npy'):
        array = np.load(path + filename)
        if combined_array is None:
            combined_array = array
        else:
            combined_array = np.concatenate((combined_array, array))
    return combined_array


rootPath = Path(os.path.realpath(__file__)).parent.parent.parent.parent.parent.parent.parent.parent.parent.parent
basepath = str(rootPath) + "/resources/"
datasetPath = basepath + "datasets/"
featuresPath = datasetPath + "features/"
labelsPath = datasetPath + "labels/"
# createIfNotExists(datasetPath)
modelsPath = basepath + "models/"
# createIfNotExists(modelsPath)
# labels = load_json(datasetPath + "labels.txt")
# features = load_json(datasetPath + "features.txt")
features = load_all_npy_files(featuresPath)
labels = load_all_npy_files(labelsPath)

x_train, x_test, y_train, y_test = train_test_split(features, labels, test_size=0.33)

model = Sequential()
model.add(Dense(units=128, activation='relu', input_dim=72 * 14))
model.add(Dense(units=128, activation='relu'))
model.add(Dense(units=128, activation='relu'))
model.add(Dense(units=1, activation='sigmoid'))
model.compile(loss='mae', optimizer='adam', metrics=['mae', 'mse', 'mape'])

loss_and_metrics = model.evaluate(x_test, y_test, batch_size=128)
print(loss_and_metrics)

tb = TensorBoard(log_dir='./Graph', histogram_freq=0, write_graph=True, write_images=True)
es = EarlyStopping(monitor='val_loss', patience=50)
model.fit(x_train, y_train, epochs=1000, verbose=1, batch_size=32, validation_split=0.1, callbacks=[tb, es])

loss_and_metrics = model.evaluate(x_test, y_test, batch_size=128)
print(loss_and_metrics)

classes = model.predict(x_test, batch_size=128)
print(classes[:5])
print(y_test)

model.save(modelsPath + 'score_estimator.h5')
