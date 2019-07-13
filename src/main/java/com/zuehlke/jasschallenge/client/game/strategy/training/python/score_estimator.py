import os
import sys

from keras.callbacks import TensorBoard, EarlyStopping, ModelCheckpoint
from keras.engine.saving import load_model
from tensorflow.contrib.learn.python.learn.estimators._sklearn import train_test_split

from neural_networks import define_score_estimator_model
from util import load_all_npy_files, score_labels_path, score_features_path, models_path, score_estimator_path, \
    load_cbor_files

#features = load_all_npy_files(score_features_path)
#labels = load_all_npy_files(score_labels_path)


# x_train, x_test, y_train, y_test = train_test_split(features, labels, test_size=0.33)

def train(dataset_path, epochs):
    if os.path.exists(score_estimator_path):
        model = load_model(score_estimator_path)
    else:
        model = define_score_estimator_model()
        model.compile(loss='mse', optimizer='adam', metrics=['mae', 'mape', 'accuracy'])

    features = load_cbor_files(dataset_path + "features")
    labels = load_cbor_files(dataset_path + "labels")

    tb = TensorBoard(log_dir='./Graph', write_images=True)
    es = EarlyStopping(monitor='val_loss', patience=50, restore_best_weights=True)
    # TODO consider only saving one file. always replace it...
    mc = ModelCheckpoint(models_path + "score_estimator.{epoch:03d}-{val_loss:.5f}.hdf5", save_best_only=True, period=10)

    model.fit(features, labels, epochs=epochs, verbose=1, batch_size=128, validation_split=0.1, callbacks=[tb, es, mc])

    model.save(score_estimator_path)


# loss_and_metrics = model.evaluate(x_test, y_test, batch_size=128)
# print(loss_and_metrics)


def predict(features):
    model = load_model(score_estimator_path)
    classes = model.predict(features, batch_size=128)
    print(classes[:1])


# TODO how to invoke specific python method on command line?


if __name__ == '__main__':
    globals()[sys.argv[1]](sys.argv[2])
