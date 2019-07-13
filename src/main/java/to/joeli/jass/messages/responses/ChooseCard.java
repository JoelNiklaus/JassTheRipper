package to.joeli.jass.messages.responses;

import to.joeli.jass.messages.type.RemoteCard;

public class ChooseCard implements Response {

    private final RemoteCard data;

    public ChooseCard(RemoteCard data) {
        this.data = data;
    }

    public RemoteCard getData() {
        return data;
    }
}
