# jass-challenge-client-java ![Build Status](https://travis-ci.org/webplatformz/challenge-client-java.svg?branch=master)

This is a Java client for the [Jass challenge server](https://github.com/webplatformz/challenge).
This client allows you to easily develop a bot for the Jass challenge.

## Getting started

Clone this repository and start (`gradlew run`) the [Application](src/main/java/com/zuehlke/jasschallenge/Application.java) class:

``` java
public class Application {
    //CHALLENGE2017: Set your bot name
    private static final String BOT_NAME = "awesomeJavaBot";
    //CHALLENGE2017: Set your own strategy
    private static final RandomJassStrategy STRATEGY = new RandomJassStrategy();

    private static final String LOCAL_URL = "ws://localhost:3000";

    public static void main(String[] args) throws Exception {
        String websocketUrl = parseWebsocketUrlOrDefault(args);

        Player myLocalPlayer = new Player(BOT_NAME, STRATEGY);
        startGame(websocketUrl, myLocalPlayer, SessionType.TOURNAMENT);
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

## Start your own tournament
To test your bot against other bots, such das the random bot, you need to start your own tournament. 

1. start the challenge server:
`npm start`
2. Browse to http://localhosthost:3000
3. Enter some user name: 

![Alt text](doc/images/chooseUsername.PNG?raw=true "Choose a user name")
4. Enter some tournament name and press **Enter** 

![Alt text](doc/images/createTournament.PNG?raw=true "Choose a user name")

5. Join your bots, they should appear on the next page

![Alt text](doc/images/tournamentPage.PNG?raw=true "Choose a user name")



## Contributors ##
Thanks to [fluescher](https://github.com/fluescher) for creating this skeleton.