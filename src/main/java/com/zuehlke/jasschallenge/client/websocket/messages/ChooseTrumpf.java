package com.zuehlke.jasschallenge.client.websocket.messages;

import com.zuehlke.jasschallenge.client.websocket.messages.type.Trumpf;
import com.zuehlke.jasschallenge.client.websocket.messages.type.TrumpfChoice;

public class ChooseTrumpf extends Message {

    private final TrumpfChoice data;

    public ChooseTrumpf(Trumpf trumpf) {
        super(Type.CHOOSE_TRUMPF);
        data = new TrumpfChoice(trumpf, null);
    }

    public TrumpfChoice getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChooseTrumpf that = (ChooseTrumpf) o;

        return !(data != null ? !data.equals(that.data) : that.data != null);

    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }
}