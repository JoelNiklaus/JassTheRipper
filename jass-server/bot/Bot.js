import nameGenerator from 'docker-namesgenerator';
import * as JassBot from './JassBot';

let numberOfBotsToStartFromCommandLine = process.argv[2];
let defaultNumberOfBotsToStartFromCommandLine = 4;

let nameOfBotToStartFromCommandLine = process.argv[3];

let numberOfBotsToStart = defaultNumberOfBotsToStartFromCommandLine;
if (!isNaN(numberOfBotsToStartFromCommandLine) && numberOfBotsToStartFromCommandLine > 0) {
    numberOfBotsToStart = numberOfBotsToStartFromCommandLine;
}

if (nameOfBotToStartFromCommandLine) {
    nameOfBot = nameOfBotToStartFromCommandLine;
}

for (let i = 1; i <= numberOfBotsToStart; i++) {
    const nameOfBot = nameGenerator();
    JassBot.create(nameOfBot);
    JassBot.create(nameOfBot);
}

