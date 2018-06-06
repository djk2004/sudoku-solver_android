package com.sudoku.dj.sudokusolver.solver;

import java.util.PriorityQueue;
import java.util.Random;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Deque;
import java.util.ArrayDeque;

/**
 * The Solver contains logic to produce a solution to the provided Sudoku cell model.  For
 * an initial Sudoku puzzle, the number of available values per cell is determined, and each
 * cell is added to a priority queue, where the cells with the lowest number of possible values
 * are at the top of the queue.  Additionally, the cells are added to the queue in random order,
 * because adding randomness can sometimes allow the algorithm to produce puzzle solutions faster
 * (of course, sometimes the randomness produces solutions slower too).
 * 
 * A single call to solve() is intended to be an attempt to traverse through the model one time,
 * where the result will be that the puzzle is either solved, or that the algorithm hit a dead end.
 */
public class Solver {
    private CellModel model;
    private PriorityQueue<Cell> unfilled;
    private Deque<Cell> filled;

    public Solver(CellModel model) {
        this(model, new DefaultComparator());
    }

    public Solver(CellModel model, Comparator<Cell> comparator) {
        this.model = model;
        Set<Cell> unfilledCells = buildRandomizedSet(model.getUnfilledCells());
        this.unfilled = new PriorityQueue<>(unfilledCells.size(), comparator);
        this.unfilled.addAll(unfilledCells);
        this.filled = new ArrayDeque<>();
    }

    private <T> Set<T> buildRandomizedSet(Collection<T> original) {
        Set<T> values = new TreeSet<>(new Comparator<T>() {
            private Random r = new Random(System.currentTimeMillis());

            @Override
            public int compare(T a, T b) {    
                // store available values in a random order by manipulating the comparator
                return r.nextBoolean() ? 1 : -1;
            }
        });
        values.addAll(original);
        return values;
    }

    /**
     * This will attempt to solve the puzzle by doing the following:
     *   - Build a randomized set of available values.
     *   - Grab one of the available values and set the cell, then check if the puzzle is
     *     still solveable.
     *   - If the puzzle is still solveable, then extract the Cell and check the next one.
     *     Otherwise, attempt to use another available random value and repeat until either
     *     one of the values makes the puzzle solveable or all values are exhausted.
     *   - If a puzzle is still solveable, but the current cell is at a dead end, then
     *     when allowed, the algorithm will randomly backtrack a few steps and try again.
     *   - The loop will end when either:
     *       - The queue runs out of cells.  If this happens, the puzzle is complete, or, 
     *       - The puzzle is not solveable with the current cell values.  The algorithm has
     *         hit a dead end.
     * @param allowBacktracking When true, the algorithm will attempt to backtrack when necessary.
     *                          Backtracking does not necessarily allow the puzzle to be solved faster.
     * @return The number of steps taken in the attempt to solve the puzzle.
     */
    public int solve(boolean allowBacktracking) {
        Cell current;
        boolean runLoop = true;
        int steps = 0;
        Random r = new Random(System.currentTimeMillis());
        while (runLoop && (current = unfilled.poll()) != null) {
            Set<Integer> originalSet = current.getAvailableValues();
            if (allowBacktracking && originalSet.isEmpty()) {
                if (filled.isEmpty()) {
                    // the puzzle is in a state where it cannot be solved, so end the loop
                    runLoop = false;
                } else {
                    // to backtrack: choose a random number of cells, in reverse order that values
                    // were set, and reset them to 0, then add back to the queue
                    int totalToBacktrack = r.nextInt(filled.size() - 1) + 1;
                    for (int i=0; i<totalToBacktrack; i++) {
                        Cell back = filled.removeFirst();
                        model.resetValue(back);
                        unfilled.add(back);
                    }
                }
            } else {
                // for the current cell, randomly choose any available value
                // then if the puzzle is solveable, add the cell to the filled stack
                // and continue iterating through the queue
                Set<Integer> availableValues = buildRandomizedSet(originalSet);
                for (Integer value: availableValues) {
                    model.setValue(current, value);
                    if (model.isSolveable()) {
                        steps++;
                        filled.addFirst(current);
                        break;
                    }
                }
            }

            // final check to ensure the model is still solveable, if this fails then end the loop
            if (!model.isSolveable()) {
                runLoop = false;
            }
        }
        return steps;
    }

    /**
     * Creates a board that fills no more than the specified number of cells.  If a board becomes
     * unsolveable, then this will backtrack only one step to return the board with the last solveable
     * state.
     * @param filledCells
     */
    public void buildNewBoard(int filledCells) {
        if (!model.isEmptyBoard()) {
            throw new RuntimeException("Cell model must be empty to generate a new board");
        }

        Cell current;
        int cellCount = 0;
        while (cellCount < filledCells && (current = unfilled.poll()) != null) {
            Set<Integer> availableValues = buildRandomizedSet(current.getAvailableValues());
            for (Integer value: availableValues) {
                model.setValue(current, value);
                if (model.isSolveable()) {
                    filled.addFirst(current);
                    cellCount++;
                    break;
                } else {
                    model.resetValue(current);
                    unfilled.add(current);

                    // Forces the loop to end with the current board, which should be solveable
                    // at this point.  This is important because the board creation process
                    // should be very fast, otherwise this process must move into a background task.
                    // Ideally, this loop should iterate no more than about 30-35 times, once for
                    // each filled cell in the board.
                    cellCount = filledCells;
                }
            }
        }
        model.lockFilledCells();
    }

    public static class DefaultComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell a, Cell b) {
            Integer aSize = Integer.valueOf(a.getAvailableValues().size());
            Integer bSize = Integer.valueOf(b.getAvailableValues().size());
            return aSize.compareTo(bSize);
        }
    }

    public static class HorizontalGroupComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell a, Cell b) {
            Integer aSize = Integer.valueOf(a.getHorizontalGroup().getAvailableValues().size());
            Integer bSize = Integer.valueOf(b.getHorizontalGroup().getAvailableValues().size());
            return aSize.compareTo(bSize);
        }
    }

    public static class VerticalGroupComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell a, Cell b) {
            Integer aSize = Integer.valueOf(a.getVerticalGroup().getAvailableValues().size());
            Integer bSize = Integer.valueOf(b.getVerticalGroup().getAvailableValues().size());
            return aSize.compareTo(bSize);
        }
    }

    public static class CubeGroupComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell a, Cell b) {
            Integer aSize = Integer.valueOf(a.getCubeGroup().getAvailableValues().size());
            Integer bSize = Integer.valueOf(b.getCubeGroup().getAvailableValues().size());
            return aSize.compareTo(bSize);
        }
    }

    public static class IDComparator implements Comparator<Cell> {
        @Override
        public int compare(Cell a, Cell b) {
            Integer aSize = Integer.valueOf(a.getID());
            Integer bSize = Integer.valueOf(b.getID());
            return aSize.compareTo(bSize);
        }
    }
}