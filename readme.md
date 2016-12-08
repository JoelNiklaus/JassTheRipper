# jass-challenge-client-java ![Build Status](https://travis-ci.org/webplatformz/jass-challenge-client-java.svg)

This is a Java client for the [Jass challenge server](https://github.com/webplatformz/challenge).
This client allows you to easily develop a bot for the Jass challenge.

## Getting started

Clone this repository and start (`gradle run`) the [Application](src/main/java/com/zuehlke/jasschallenge/client/Application.java) class:

``` java
public class Application {
    public static void main(String[] args) throws Exception {
        final String name = "Your bot name here";
        final Player myLocalPlayer = new Player(name, new RandomMoveJassStrategy());

        final RemoteGame remoteGame = new RemoteGame("ws://jasschallenge.herokuapp.com", myLocalPlayer, SessionType.SINGLE_GAME);
        remoteGame.start();
    }
}
```

## Implement your own bot

To implement your own bot you need to provide an implementation of the
[JassStrategy](src/main/java/com/zuehlke/jasschallenge/client/game/strategy/JassStrategy.java) interface:

``` java
public interface JassStrategy {
    Mode chooseTrumpf(Set<Card> availableCards, GameSession session);
    Card chooseCard(Set<Card> availableCards, GameSession session);

    default void onSessionStarted(GameSession session) {}
    default void onGameStarted(GameSession session) {}
    default void onMoveMade(Move move, GameSession session) {}
    default void onGameFinished() {}
    default void onSessionFinished() {}
}
```

Thanks to [fluescher](https://github.com/fluescher) for creating this skeleton.