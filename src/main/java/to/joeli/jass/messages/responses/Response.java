package to.joeli.jass.messages.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChooseCard.class, name = "CHOOSE_CARD"),
        @JsonSubTypes.Type(value = ChooseTrumpf.class, name = "CHOOSE_TRUMPF"),
        @JsonSubTypes.Type(value = ChoosePlayerName.class, name = "CHOOSE_PLAYER_NAME"),
        @JsonSubTypes.Type(value = ChooseSession.class, name = "CHOOSE_SESSION"),
})
public interface Response {
}
