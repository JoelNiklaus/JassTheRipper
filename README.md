# JassTheRipper [![Build Status](https://travis-ci.com/JoelNiklaus/JassTheRipper.svg?token=TyJh5WgmQurKDQkSXyDX&branch=master)](https://travis-ci.com/JoelNiklaus/JassTheRipper)

This is a Java client (bot) for the [Jass challenge server](https://github.com/webplatformz/challenge).
It is a fork of the [challenge java client](https://github.com/webplatformz/challenge-client-java).

We use Monte Carlo Tree Search (MCTS) with determinization and domain knowledge to beat the best machine and humand Jass players in the Schieber variant.

Build the app with 
```bash
./gradlew build
```
Make sure gradle is installed.

Run this app either with docker:
```bash
docker build -t jass-the-ripper . # make sure you are in the root directory of this project

docker run -it --rm -p 80:80 jass-the-ripper # specify the ports if needed (also update the dockerfile)
```
or gradle:
```bash
./gradlew run -Pmyargs=ws://127.0.0.1:3000,1 --no-daemon # change the websocket host and port if needed. The value after the comma is the chosenTeamIndex of the bot to be started
```

Connect to server:
```bash
ssh -i ~/.ssh/diufpc29 joel@diufpc29
```

Run specific test
```bash
./gradlew test --tests to.joeli.jass.client.strategy.training.ArenaTest.train
```

Run experiment
```bash
./gradlew runExperiment
```

Run experiment as a background process
```bash
nohup ./gradlew runExperiment > nohup.out &
```

Train network
```bash
python3 src/main/java/to/joeli/jass/client/strategy/training/python/train.py 0010 cards/
nohup python3 src/main/java/to/joeli/jass/client/strategy/training/python/train.py 0010 cards/ > nohup.out &
```

Connect to digitalocean server
```bash
ssh -i ~/.ssh/digitalocean root@167.99.133.247
```

Start REST endpoint: 
```bash
nohup ./gradlew startServer > nohup.out &
```

JassTheRipper bots:
- http://jasstheripper.joeli.to/random-playout
- http://jasstheripper.joeli.to/light-playout
- http://jasstheripper.joeli.to/heavy-playout
- http://jasstheripper.joeli.to/runs-100000

HSLU opponents:
- http://10.180.39.12:5001/randomsimple
- http://10.180.39.12:5001/soismcts-100000-trump
- http://10.180.39.12:5001/dnn-max-policy-trump
- http://10.180.39.12:5001/dnn-max-value-trump
- http://10.180.39.12:5001/prob-ismcts-trump

Tournament Server available on: https://jass-server.abiz.ch/

This client allows you to easily develop a bot for the Jass challenge.

###Wiki (Server):
https://github.com/webplatformz/challenge/wiki

###JassChallenge2017
If you are an enrolled student in switzerland, you are welcome to participate the **JassChallenge2017** competition in April '17

---------------------- LINK TO OUR REGISTRATION PAGE ----------------------



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

The client needs the challenge server to connect to. Clone the challenge server and run npm start. For more information
go to [Jass challenge server](https://github.com/webplatformz/challenge) repository.

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