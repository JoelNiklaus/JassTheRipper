

import WebSocket from 'ws';
import * as GameType from '../../../server/game/gameType';
import {GameMode} from '../../../shared/game/gameMode';
import * as Card from '../../../shared/deck/card';
import {CardColor} from '../../../shared/deck/cardColor';
import Validation from '../../../shared/game/validation/validation';
import * as messages from '../../../shared/messages/messages';
import {MessageType} from '../../../shared/messages/messageType';
import {SessionChoice} from '../../../shared/session/sessionChoice';
import {expect} from 'chai';


let SimpleBot = {

    onMessage(messageJson) {
        let message = JSON.parse(messageJson);

        if (message.type === MessageType.REQUEST_PLAYER_NAME.name) {
            this.client.send(JSON.stringify(messages.create(MessageType.CHOOSE_PLAYER_NAME.name, this.name)));
        }

        if (message.type === MessageType.REQUEST_SESSION_CHOICE.name) {
            let sessionName = 'Session 1';
            let sessionConfig = {
                sessionName
            };

            if (this.id === 1) {
                expect(message.data.length).to.equal(0);
                this.client.send(JSON.stringify(messages.create(MessageType.CHOOSE_SESSION.name, SessionChoice.CREATE_NEW, sessionConfig)));
            } else {
                this.client.send(JSON.stringify(messages.create(MessageType.CHOOSE_SESSION.name, SessionChoice.JOIN_EXISTING, sessionConfig)));
            }
        }

        if (message.type === MessageType.DEAL_CARDS.name) {
            this.handcards = this.mapCardsFromJson(message.data);
        }

        if (message.type === MessageType.BROADCAST_WINNER_TEAM.name) {
            this.doneFunction();
        }

        if (message.type === MessageType.REQUEST_CARD.name) {
            let handCard = this.giveValidCardFromHand(this.mapCardsFromJson(message.data), this.handcards);
            this.handcards.splice(this.handcards.indexOf(handCard), 1);
            let chooseCardResonse = messages.create(MessageType.CHOOSE_CARD.name, handCard);
            this.client.send(JSON.stringify(chooseCardResonse));
        }

        if (message.type === MessageType.REQUEST_TRUMPF.name) {
            let chooseTrumpfResponse = messages.create(MessageType.CHOOSE_TRUMPF.name, this.gameType);
            this.client.send(JSON.stringify(chooseTrumpfResponse));
        }

        if (message.type === MessageType.BROADCAST_TRUMPF.name) {
            if (message.data.mode !== GameMode.SCHIEBE) {
                this.gameType = GameType.create(message.data.mode, message.data.trumpfColor);
            }
        }
    },

    mapCardsFromJson(cards) {
        return cards.map((element) => {
            return Card.create(element.number, element.color);
        });
    },

    giveValidCardFromHand(tableCards, handCards) {
        let validation = Validation.create(this.gameType.mode, this.gameType.trumpfColor);

        for (let i = 0; i < handCards.length; i++) {
            let handCard = handCards[i];

            if (validation.validate(tableCards, handCards, handCard)) {
                return handCard;
            }
        }
    }
};

export function create(id, name, doneFunction) {
    let clientBot = Object.create(SimpleBot);
    clientBot.id = id;
    clientBot.handcards = [];
    clientBot.doneFunction = doneFunction;
    clientBot.client = new WebSocket('ws://localhost:10001');
    clientBot.client.on('message', clientBot.onMessage.bind(clientBot));
    clientBot.name = name;
    clientBot.gameType = GameType.create(GameMode.TRUMPF, CardColor.SPADES);
    return clientBot;
}