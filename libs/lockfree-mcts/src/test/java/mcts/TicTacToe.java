package mcts;

public class TicTacToe implements State {

    public static TicTacToe start(int dim) {
        return new TicTacToe((byte) dim, (byte) dim);
    }

    public static TicTacToe start(int dim, int needed) {
        return new TicTacToe((byte) dim, (byte) needed);
    }

    private static final byte DRAW = 0;
    private static final byte NOT_OVER_YET = (byte) 99;

    private final byte[] board;
    private final byte needed;
    private final byte dim;

    private byte agent;
    private byte winner;
    private int round;

    public TicTacToe(byte dims, byte needed) {
        assert dims >= needed;
        this.needed = needed;
        this.agent = 1;
        this.board = new byte[dims * dims];
        this.dim = dims;
        this.winner = NOT_OVER_YET;
    }

    public TicTacToe(TicTacToe o) {
        agent = o.agent;
        board = o.board.clone();
        needed = o.needed;
        round = o.round;
        winner = o.winner;
        dim = o.dim;
    }

    public TicTacToe(TicTacToe o, short action) {
        agent = (byte) (3 - o.agent);
        board = o.board.clone();
        needed = o.needed;
        round = o.round;
        dim = o.dim;
        winner = updateWith(action);
    }

    @Override
    public State copy() {
        return new TicTacToe(this);
    }

    @Override
    public boolean isTerminal() {
        return winner < NOT_OVER_YET;
    }

    @Override
    public short[] getAvailableActions() {
        int remaining = dim * dim - round;
        short[] actions = new short[remaining];
        int idx = 0;
        for (byte i = 0; i < board.length; i++) {
            if (board[i] == 0) {
                actions[idx++] = i;
            }
        }
        assert idx == remaining;
        return actions;
    }

    @Override
    public int getPreviousAgent() {
        return 3 - agent;
    }

    @Override
    public double getRewardFor(int agent) {
        assert winner < NOT_OVER_YET;
        if (winner == DRAW)
            return .5;

        return winner == agent
            ? 1.
            : 0;
    }

    private byte at(int row, int col) {
        int idx = row * dim + col;
        return board[idx];
    }

    private byte updateWith(short action) {
        byte prevAgent = (byte) (3 - agent);
        int row = action / dim;
        int col = action % dim;
        round++;
        board[action] = prevAgent;

        int contiguous = 0;
        for (int r = 0; r < dim; r++) {
            if (at(r, col) != prevAgent) {
                contiguous = 0;
            } else {
                if (++contiguous == needed)
                    return prevAgent;
            }
        }

        contiguous = 0;
        for (int c = 0; c < dim; c++) {
            if (at(row, c) != prevAgent) {
                contiguous = 0;
            } else {
                if (++contiguous == needed)
                    return prevAgent;
            }
        }

        if (row == col) {
            contiguous = 0;
            for (int x = 0; x < dim; x++) {
                if (at(x, x) != prevAgent) {
                    contiguous = 0;
                } else {
                    if (++contiguous == needed)
                        return prevAgent;
                }
            }
        }

        if (row == dim - 1 - col) {
            contiguous = 0;
            for (int x = 0; x < dim; x++) {
                if (at(x, dim - 1 - x) != prevAgent) {
                    contiguous = 0;
                } else {
                    if (++contiguous == needed)
                        return prevAgent;
                }
            }
        }

        return round == dim * dim
            ? DRAW
            : NOT_OVER_YET;
    }

    @Override
    public State takeAction(short action) {
        return new TicTacToe(this, action);
    }

    @Override
    public void applyAction(short action) {
        agent = (byte) (3 - agent);
        winner = updateWith(action);
    }

    @Override
    public int getWinner() {
        return winner;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("State=\n");
        for (int r = 0; r < dim; r++) {
            for (int c = 0; c < dim; c++) {
                switch (at(r, c)) {
                    case 0:
                        b.append(".");
                        break;
                    case 1:
                        b.append("X");
                        break;
                    case 2:
                        b.append("O");
                        break;
                }
            }
            b.append("\n");
        }
        return b.toString();
    }

}
