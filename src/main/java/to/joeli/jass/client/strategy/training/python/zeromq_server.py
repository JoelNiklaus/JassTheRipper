import json

import zmq
import numpy as np

from util import base_path


def init_socket():
    context = zmq.Context()
    socket = context.socket(zmq.REP)
    socket.bind("tcp://*:5555")
    print("Server listening on port 5555")
    return socket


def listen(socket, cards_estimator, score_estimator):
    while True:
        message = socket.recv()  # Wait for next request from client
        print(message)
        message = json.loads(message)
        print(message)

        if message['command'] == 'SHUTDOWN':
            socket.send_string("success")  # Send back reply
            break

        if message['command'] == 'PREDICT':
            features = np.array(message['features'])
            features = np.expand_dims(features, axis=0)  # needed to make it fit the expected network input
            print(features)
            print(np.shape(features))
            if message['networkType'] == 'CARDS':
                prediction = json.dumps(cards_estimator.predict(features)[0].tolist())
            if message['networkType'] == 'SCORE':
                prediction = score_estimator.predict(features)[0][0]
            response = str(prediction)
            print(response)

        if message['command'] == 'LOAD':
            path = base_path + "self_play/" + message['networkType']
            weights_path = path + "weights.hdf5"
            if message['networkType'] == 'CARDS':
                response = load_weights(cards_estimator, weights_path)
            if message['networkType'] == 'SCORE':
                response = load_weights(score_estimator, weights_path)

        socket.send_string(response)  # Send back reply


def load_weights(estimator, weights_path):
    try:
        estimator.load_weights(weights_path)
        return "success"
    except ImportError as err:
        return "failure: ImportError: " + str(err)
