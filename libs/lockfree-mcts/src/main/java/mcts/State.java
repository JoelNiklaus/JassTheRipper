package mcts;

public interface State {

    boolean isTerminal();

    short[] getAvailableActions();

    int getPreviousAgent();

    double getRewardFor(int agent);

    State takeAction(short action);

    State copy();

    void applyAction(short action);

    int getWinner();

}
