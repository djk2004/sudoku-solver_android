package com.sudoku.dj.sudokusolver.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private PriorityQueue<InternalCell> unfilled;
    private Deque<InternalCell> filled;

    public static enum SolverType { BOARD_BUILDER, BACKTRACKING };

    private static Map<SolverType, Comparator<InternalCell>> comparators;

    static {
        Map<SolverType, Comparator<InternalCell>> m = new HashMap<>();
        m.put(SolverType.BOARD_BUILDER, new CellGroupsComparator());
        m.put(SolverType.BACKTRACKING, new DefaultComparator());
        comparators = Collections.unmodifiableMap(m);
    }

    public Solver(CellModel model, SolverType type) {
        this.model = model;
        Set<InternalCell> unfilledCells = buildCellSet(model.getUnfilledCells());
        this.unfilled = new PriorityQueue<>(unfilledCells.size(), comparators.get(type));
        this.unfilled.addAll(unfilledCells);
        this.filled = new ArrayDeque<>();
    }

    private Set<InternalCell> buildCellSet(Collection<Cell> cells) {
        Set<InternalCell> internals = new HashSet<>();
        for (Cell c: cells) {
            internals.add(new InternalCell(c));
        }
        return internals;
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
     * @return The number of steps taken in the attempt to solve the puzzle.
     */
    public int solve(AtomicBoolean canCancel) {
        InternalCell current;
        int steps = 0;
        Random r = new Random(System.currentTimeMillis());
        while (!canCancel.get() && (current = unfilled.poll()) != null) {
            // always add the current cell to the filled stack first
            filled.addFirst(current);
            current.addVisit();
            steps++;

            Set<Integer> originalSet = current.getCell().getAvailableValues();
            if (originalSet.isEmpty()) {
                // to backtrack: choose a random number of cells, in reverse order that values
                // were set, and reset them to 0, then add back to the queue
                doBacktrack(r.nextInt(filled.size()));
            } else {
                // for the current cell, randomly choose any available value
                // then if the puzzle is solveable, add the cell to the filled stack
                // and continue iterating through the queue
                boolean solveableValueFound = false;
                Set<Integer> availableValues = buildRandomizedSet(originalSet);
                for (Integer value: availableValues) {
                    model.setValue(current.getCell(), value);
                    if (model.isSolveable()) {
                        // accept the first legal value
                        solveableValueFound = true;
                        break;
                    }
                }

                if (!solveableValueFound) {
                    doBacktrack(r.nextInt(filled.size()));
                }
            }
        }
        return steps;
    }

    private void doBacktrack(int totalToBacktrack) {
        for (int i=0; i<totalToBacktrack; i++) {
            InternalCell back = filled.removeFirst();
            model.resetValue(back.getCell());
            unfilled.add(back);
        }
    }

    private String buildFilledBoard(String mask) {
        // build a set containing all available values
        List<Integer> available = new ArrayList<>();
        for (int i=1; i<=CellModel.MAX_CELLS_IN_GROUP; i++) {
            available.add(Integer.valueOf(i));
        }

        Random r = new Random(System.currentTimeMillis());
        String board = new String(mask);
        for (char ch = 'A'; ch < 'J'; ch++) {
            Integer value = available.remove(r.nextInt(available.size()));
            board = board.replaceAll(""+ch, value.toString());
        }
        return board;
    }

    private static class DefaultComparator implements Comparator<InternalCell> {
        @Override
        public int compare(InternalCell a, InternalCell b) {
            Integer aSize = Integer.valueOf(a.getCell().getAvailableValues().size() + a.getVisits());
            Integer bSize = Integer.valueOf(b.getCell().getAvailableValues().size() + b.getVisits());
            return aSize.compareTo(bSize);
        }
    }

    /**
     * This comparator ranks cells based on the number of empty cells in the associated groups.
     * Unlike the default comparator, which ranks cells based on the number of possible legal values,
     * this comparator is looking at the number of associated cells in the group that are unfilled.
     * A cell should rank higher using this comparator if it's horizontal, vertical, and cube groups
     * have fewer filled cells.
     */
    private static class CellGroupsComparator implements Comparator<InternalCell> {

        private int buildAvailableValuesCount(Cell c) {
            return (CellModel.MAX_CELLS_IN_GROUP * 3) -
                (c.getHorizontalGroup().getAvailableValues().size() +
                c.getVerticalGroup().getAvailableValues().size() +
                c.getCubeGroup().getAvailableValues().size());
        }

        @Override
        public int compare(InternalCell a, InternalCell b) {
            Integer aSize = Integer.valueOf(buildAvailableValuesCount(a.getCell()) + a.getVisits());
            Integer bSize = Integer.valueOf(buildAvailableValuesCount(b.getCell()) + b.getVisits());
            return aSize.compareTo(bSize);
        }
    }

    private static class InternalCell {
        private final Cell cell;
        private int visits;

        public InternalCell(Cell cell) {
            this.cell = cell;
            visits = 0;
        }

        public void addVisit() {
            visits++;
        }

        public int getVisits() {
            return visits;
        }

        public Cell getCell() {
            return cell;
        }
    }

//    public static class HorizontalGroupComparator implements Comparator<Cell> {
//        @Override
//        public int compare(Cell a, Cell b) {
//            Integer aSize = Integer.valueOf(a.getHorizontalGroup().getAvailableValues().size());
//            Integer bSize = Integer.valueOf(b.getHorizontalGroup().getAvailableValues().size());
//            return aSize.compareTo(bSize);
//        }
//    }
//
//    public static class VerticalGroupComparator implements Comparator<Cell> {
//        @Override
//        public int compare(Cell a, Cell b) {
//            Integer aSize = Integer.valueOf(a.getVerticalGroup().getAvailableValues().size());
//            Integer bSize = Integer.valueOf(b.getVerticalGroup().getAvailableValues().size());
//            return aSize.compareTo(bSize);
//        }
//    }
//
//    public static class CubeGroupComparator implements Comparator<Cell> {
//        @Override
//        public int compare(Cell a, Cell b) {
//            Integer aSize = Integer.valueOf(a.getCubeGroup().getAvailableValues().size());
//            Integer bSize = Integer.valueOf(b.getCubeGroup().getAvailableValues().size());
//            return aSize.compareTo(bSize);
//        }
//    }
//
//    public static class IDComparator implements Comparator<Cell> {
//        @Override
//        public int compare(Cell a, Cell b) {
//            Integer aSize = Integer.valueOf(a.getID());
//            Integer bSize = Integer.valueOf(b.getID());
//            return aSize.compareTo(bSize);
//        }
//    }
//
//    public static class MixedGroupComparator implements Comparator<Cell> {
//
//        private int getValue(Cell cell) {
//            int id = cell.getID();
//            if (id % 3 == 0) {
//                return cell.getCubeGroup().getAvailableValues().size();
//            }
//            if (id % 2 == 0) {
//                return cell.getVerticalGroup().getAvailableValues().size();
//            }
//            return cell.getHorizontalGroup().getAvailableValues().size();
//        }
//
//        @Override
//        public int compare(Cell a, Cell b) {
//            Integer aValue = Integer.valueOf(getValue(a));
//            Integer bValue = Integer.valueOf(getValue(b));
//            return aValue.compareTo(bValue);
//        }
//    }
}