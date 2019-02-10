import * as ClientApi from '../communication/clientApi';
import * as SessionFactory from './sessionFactory';
import {SessionChoice} from '../../shared/session/sessionChoice';
import {SessionType} from '../../shared/session/sessionType';
import nameGenerator from 'docker-namesgenerator';
import {MessageType} from '../../shared/messages/messageType';
import Registry from '../registry/registry';
import {Logger} from '../logger';

let clientApi = ClientApi.create();

let messageListeners = [];

function findOrCreateSessionWithSpace(sessions, sessionChoiceResponse) {
    let filteredSessions = sessions.filter((session) => {
        return !session.started;
    });

    if (filteredSessions.length === 0) {
        return createSession(sessions, {
            sessionName: sessionChoiceResponse.sessionName || nameGenerator(),
            sessionType: sessionChoiceResponse.sessionType || SessionType.SINGLE_GAME
        });
    }

    return filteredSessions[0];
}

function createSession(sessions, sessionChoiceResponse) {
    let session = SessionFactory.create(sessionChoiceResponse.sessionName, sessionChoiceResponse.sessionType);
    sessions.push(session);
    return session;
}

function findSession(sessions, sessionChoiceResponse) {
    let filteredSessions = sessions.filter((session) => {
        return session.name === sessionChoiceResponse.sessionName;
    });

    if (filteredSessions.length === 0) {
        return createSession(sessions, sessionChoiceResponse);
    }

    return filteredSessions[0];
}

function createAndReturnSession(sessions, sessionChoiceResponse) {
    switch (sessionChoiceResponse.sessionChoice) {
        case SessionChoice.CREATE_NEW:
            return createSession(sessions, sessionChoiceResponse);
        case SessionChoice.SPECTATOR:
        case SessionChoice.JOIN_EXISTING:
            return findSession(sessions, sessionChoiceResponse);
        default:
            return findOrCreateSessionWithSpace(sessions, sessionChoiceResponse);
    }
}

function keepSessionAlive(webSocket, intervall) {
    if (webSocket.readyState === 1) {
        webSocket.ping();
        setTimeout(keepSessionAlive.bind(null, webSocket, intervall), intervall);
    }
}

function handleTournamentStart(SessionHandler, webSocket, session) {
    if (!session.started && session.isComplete()) {
        SessionHandler.startSession(session);
    } else {
        clientApi.waitForTournamentStart(webSocket).then(handleTournamentStart);
    }
}


const SessionHandler = {

    sessions: [],

    getAvailableSessionNames() {
        return this.sessions.filter((session) => {
            return !session.started;
        }).map((session) => {
            return session.name;
        });
    },

    handleClientConnection(ws) {
        keepSessionAlive(ws, 10000);

        messageListeners.push(clientApi.subscribeMessage(ws, MessageType.REQUEST_REGISTRY_BOTS, () => {
            Registry.getRegisteredBots()
                .then(bots => clientApi.sendRegistryBots(ws, bots))
                .catch(error => Logger.debug(`Can't reach Jassbot registry, got ${error}`));
        }));

        messageListeners.push(clientApi.subscribeMessage(ws, MessageType.ADD_BOT_FROM_REGISTRY, (message) => {
            const bot = message.data.bot;
            const sessionName = message.data.sessionName;
            Registry.addBot(bot, SessionType.TOURNAMENT, sessionName);
        }));

        return clientApi.requestPlayerName(ws).then((playerName) => {
            return clientApi.requestSessionChoice(ws, this.getAvailableSessionNames()).then((sessionChoiceResponse) => {
                const session = createAndReturnSession(this.sessions, sessionChoiceResponse);

                if (sessionChoiceResponse.sessionChoice === SessionChoice.SPECTATOR || sessionChoiceResponse.asSpectator) {
                    session.addSpectator(ws);

                    if (session.type === SessionType.TOURNAMENT) {
                        clientApi.waitForTournamentStart(ws).then(handleTournamentStart.bind(null, this, ws, session));
                    }
                } else {
                    session.addPlayer(ws, playerName, sessionChoiceResponse.chosenTeamIndex);
                    if (session.type === SessionType.SINGLE_GAME && session.isComplete()) {
                        this.startSession(session);
                    }
                }
            });
        });
    },

    startSession(session) {
        messageListeners = messageListeners.filter(unbindListener => {
            unbindListener();
            return false;
        });

        session.start().then(
            this.finishSession.bind(this, session),
            this.finishSession.bind(this, session));
    },

    finishSession(session) {
        session.close('Game Finished');
        this.removeSession(session);
    },

    removeSession(session) {
        let index = this.sessions.indexOf(session);
        this.sessions.splice(index, 1);
    },

    resetInstance() {
        this.sessions = [];
    }

};

export default SessionHandler;
