package com.zuehlke.jasschallenge.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zuehlke.jasschallenge.client.websocket.GameHandler;
import com.zuehlke.jasschallenge.messages.responses.Response;
import com.zuehlke.jasschallenge.messages.type.RemoteCard;

import java.util.List;
import java.util.Optional;

public class PlayedCards implements Message {

    private final List<RemoteCard> cards;

    public PlayedCards(@JsonProperty(value = "data",required = true) List<RemoteCard> data) {
        this.cards = data;
    }

    @Override
    public Optional<Response> dispatch(GameHandler handler) {
        handler.onPlayedCards(cards);
        return Optional.empty();
    }
}
