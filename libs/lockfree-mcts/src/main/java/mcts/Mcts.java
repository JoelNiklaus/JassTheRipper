package mcts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Mcts<S extends State> {
    private static final int NO_ACTION = -1;

    private final AtomicInteger totalIterations = new AtomicInteger();

    private final ExecutorService executor;
    private final long timePerActionMillis;
    private final int maxIterations;
    private final int threads;

    private Node<S> root;
    private int lastAction;

    public Mcts(
        ExecutorService executor,
        int threads,
        long timePerActionMillis,
        int maxIterations) {
        this.executor = executor;
        this.threads = threads;
        this.timePerActionMillis = timePerActionMillis;
        this.maxIterations = maxIterations;
    }

    public int getLastAction() {
        return lastAction;
    }

    public int getTotalIterations() {
        return totalIterations.get();
    }

    public void setRoot(int action, S state) {
        if (root != null) {
            Node<S> child = root.findChildFor(action);
            if (child != null) {
                root = child;
                root.releaseParent();
                return;
            }
        }
        root = new Node<>(null, NO_ACTION, state);
    }

    public void think() {
        if (threads == 1) {
            doThink();
            return;
        }

        Collection<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++)
            tasks.add(() -> {
                doThink();
                return null;
            });

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void doThink() {
        long started = System.currentTimeMillis();
        int i = 0;
        Random random = ThreadLocalRandom.current();
        while (i++ < maxIterations && System.currentTimeMillis() - started < timePerActionMillis
            || !root.isExpanded()) {

            growTree(random);
            totalIterations.incrementAndGet();
        }
    }

    public State takeAction() {
        Node<S> actionNode = root.childToExploit();
        lastAction = actionNode.getAction();
        root = actionNode;
        root.releaseParent();
        return actionNode.getState();
    }

    private void growTree(Random random) {
        Node<S> child = selectOrExpand();
        S terminalState = simulate(child, random);
        backPropagate(child, terminalState);
    }

    private Node<S> selectOrExpand() {
        Node<S> node = root;
        while (!node.isTerminal()) {
            if (!node.isExpanded()) {
                Node<S> expandedNode = node.expand();
                if (expandedNode != null)
                    return expandedNode;
            }
            node = node.childToExplore();
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private S simulate(Node<S> node, Random random) {
        S state = (S) node.getState().copy();
        while (!state.isTerminal()) {
            short[] actions = state.getAvailableActions();
            int randomIdx = random.nextInt(actions.length);
            short action = actions[randomIdx];
            state.applyAction(action);
        }
        return state;
    }

    private void backPropagate(Node<S> node, S terminalState) {
        while (node != null) {
            double reward = terminalState.getRewardFor(node.getPreviousAgent());
            node.updateRewards(reward);
            node = node.getParent();
        }
    }

}
