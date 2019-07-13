import sys

import numpy
from keras.engine.saving import load_model

from util import estimator_model_path

if __name__ == '__main__':
    features = numpy.random.random_sample((1, 73, 18))
    #features = numpy.ndarray.flatten(features)
    # features = sys.argv[1]
    model = load_model(estimator_model_path)
    cards, score = model.predict(features, batch_size=1)
    print(cards)
    print(score)
