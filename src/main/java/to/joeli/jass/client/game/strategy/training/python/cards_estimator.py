from keras.callbacks import TensorBoard, EarlyStopping, ModelCheckpoint
from tensorflow.contrib.learn.python.learn.estimators._sklearn import train_test_split

from neural_networks import define_cards_estimator_model
from util import load_all_npy_files, cards_labels_path, cards_features_path, models_path, cards_estimator_path

features = load_all_npy_files(cards_features_path)
labels = load_all_npy_files(cards_labels_path)

x_train, x_test, y_train, y_test = train_test_split(features, labels, test_size=0.33)

model = define_cards_estimator_model()
model.compile(loss='kld', optimizer='adam',
              metrics=['mse', 'mae', 'categorical_crossentropy', 'accuracy'])

loss_and_metrics = model.evaluate(x_test, y_test, batch_size=128)
print(loss_and_metrics)

tb = TensorBoard(log_dir='./Graph', write_images=True)
es = EarlyStopping(monitor='val_loss', patience=50, restore_best_weights=True)
mc = ModelCheckpoint(models_path + "cards_estimator.{epoch:03d}-{val_loss:.5f}.hdf5", save_best_only=True, period=10)

model.fit(x_train, y_train, epochs=999, verbose=1, batch_size=128, validation_split=0.1, callbacks=[tb, es, mc])

loss_and_metrics = model.evaluate(x_test, y_test, batch_size=128)
print(loss_and_metrics)

classes = model.predict(x_test, batch_size=128)
print(classes[:1])
print(y_test[:1])

model.save(cards_estimator_path)
