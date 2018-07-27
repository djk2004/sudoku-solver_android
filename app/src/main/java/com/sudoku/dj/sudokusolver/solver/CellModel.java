package com.sudoku.dj.sudokusolver.solver;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The memory representation of the Sudoku board.
 */
public class CellModel {

    public static final int MAX_CELLS_IN_GROUP = 9;
    public static final int MAX_GROUPS = MAX_CELLS_IN_GROUP;
    public static final int MAX_CELLS = MAX_CELLS_IN_GROUP * MAX_CELLS_IN_GROUP;
    public static final int MAX_CELLS_IN_CUBE = Double.valueOf(Math.sqrt(MAX_CELLS_IN_GROUP)).intValue();
    
    private static final int NO_VALUE = 0;
    private static final Set<Integer> LEGAL_VALUES;
    
    static {
        Set<Integer> all = new HashSet<>();
        for (int i=1; i<= MAX_CELLS_IN_GROUP; i++) {
            all.add(i);
        }
        LEGAL_VALUES = Collections.unmodifiableSet(all);
    }

    private static List<Integer> buildEmptyCellModel() {
        List<Integer> list = new ArrayList<>(MAX_CELLS);
        Integer unassigned = Integer.valueOf(NO_VALUE);
        for (int i=0; i<MAX_CELLS; i++) {
            list.add(unassigned);
        }
        return list;
    }

    private final List<CellImpl> cells;
    private final List<GroupImpl> horizontals, verticals, cubes;
    private final List<ChangeListener> listeners;

    public CellModel() {
        this(buildEmptyCellModel());
    }

    public CellModel(List<Integer> initial) {
        if (initial == null || initial.size() != MAX_CELLS) {
            throw new RuntimeException("Invalid puzzle size");
        }
        this.cells = Collections.unmodifiableList(buildCells(initial));
        this.horizontals = Collections.unmodifiableList(buildGroupsList(new HorizontalGroupBuilder()));
        this.verticals = Collections.unmodifiableList(buildGroupsList(new VerticalGroupBuilder()));
        this.cubes = Collections.unmodifiableList(buildGroupsList(new CubeGroupBuilder()));

        this.listeners = new ArrayList<>();
    }

    public ChangeListenerRegistration addListener(final ChangeListener listener) {
        this.listeners.add(listener);
        return new ChangeListenerRegistration() {
            @Override
            public void unregister() {
                listeners.remove(listener);
            }
        };
    }

    /**
     * Gets the horizontal group.
     */
    public Group getHorizontalGroup(int groupID) {
        return horizontals.get(groupID);
    }

    /**
     * Gets the vertical group.
     */
    public Group getVerticalGroup(int groupID) {
        return verticals.get(groupID);
    }

    /**
     * Gets the cube group.
     */
    public Group getCubeGroup(int groupID) {
        return cubes.get(groupID);
    }

    private List<Cell> getFilteredCells(CellFilter filter) {
        List<Cell> out = new ArrayList<>();
        for (Cell cell: cells) {
            if (filter.filter(cell)) {
                out.add(cell);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Gets the list of cells that do not have any available values.  These should also
     * have a legal value set.
     */
    public List<Cell> getFilledCells() {
        CellFilter filter = new CellFilter() {
            @Override
            public boolean filter(Cell cell) {
                return (cell.getAvailableValues().size() == NO_VALUE);
            }
        };
        return getFilteredCells(filter);
    }

    /**
     * Gets the list of cells that have available values.  These are guaranteed to be blank
     * initially before attempting to solve the puzzle.
     */
    public List<Cell> getUnfilledCells() {
        CellFilter filter = new CellFilter() {
            @Override
            public boolean filter(Cell cell) {
                return (cell.getAvailableValues().size() != NO_VALUE);
            }
        };
        return getFilteredCells(filter);
    }

    /**
     * Sets the value of the given cell.
     * NOTE: Mutating the cell values is only allowed at the model level, since changing a 
     * cell value affects all other cells in the shared groups.
     */
    public void setValue(Cell cell, Integer value) {
        int oldValue = cell.getValue();
        ((CellImpl)cell).setValue(value);
        ((CellImpl)cell).resetAvailableCache();
        ((GroupImpl)cell.getHorizontalGroup()).resetAvailableCache();
        ((GroupImpl)cell.getVerticalGroup()).resetAvailableCache();
        ((GroupImpl)cell.getCubeGroup()).resetAvailableCache();
        for (ChangeListener listener: listeners) {
            listener.onChange(cell, oldValue);
        }
    }

    public Cell getCell(int id) {
        return cells.get(id);
    }

    /**
     * Clears the cell value.
     */
    public void resetValue(Cell cell) {
        setValue(cell, NO_VALUE);
    }

    /**
     * Resets the cell values to their initial state.
     */
    public void resetCells() {
        for (CellImpl cell: cells) {
            if (!cell.isLocked()) {
                resetValue(cell);
            }
        }
    }

    /**
     * Resets the cell values to their initial state.
     */
    public void resetAllCells() {
        for (CellImpl cell: cells) {
            cell.unlockCell();
            resetValue(cell);
        }
    }

    /**
     * Returns true if the puzzle is solveable: a puzzle is not solvable if a
     * blank cell can be found that has no available values.
     */
    public boolean isSolveable() {
        for (CellImpl cell: cells) {
            if (!cell.isLocked() &&
                cell.getValue() == NO_VALUE && 
                cell.getAvailableValues().size() == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if all cells have an assigned value and no other available values
     * @return
     */
    public boolean isSolved() {
        for (CellImpl cell: cells) {
            if (cell.getValue() == NO_VALUE || cell.getAvailableValues().size() > 0)
                return false;
        }
        return true;
    }

    /**
     * Returns true if the board is empty, meaning that all cells are 0 and none are immutable.
     */
    public boolean isEmptyBoard() {
        for (CellImpl cell: cells) {
            if (cell.isLocked() || cell.getValue() != NO_VALUE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Locks all cells that contain a value, by marking them as immutable.
     */
    public void lockFilledCells() {
        for (CellImpl cell: cells) {
            if (cell.getValue() != NO_VALUE) {
                cell.lockCell();
                for (ChangeListener listener: listeners) {
                    listener.onChange(cell, NO_VALUE);
                }
            }
        }
    }

    private List<CellImpl> buildCells(List<Integer> initial) {
        List<CellImpl> t = new ArrayList<>(MAX_CELLS);
        int id = 0;
        for (Integer value: initial) {
            t.add(new CellImpl(id++, value));
        }
        return t;
    }

    private List<GroupImpl> buildGroupsList(GroupBuilder builder) {
        List<GroupImpl> t = new ArrayList<>(MAX_CELLS_IN_GROUP);
        for (int i=0; i<MAX_CELLS_IN_GROUP; i++) {
            t.add(builder.buildGroup(i));
        }
        return t;
    }

    // Added for Android due to lack of support for lambdas in Java 1.7
    private interface CellFilter {
        boolean filter(Cell cell);
    }

    private interface GroupBuilder {
        GroupImpl buildGroup(int groupID);
    }

    private class HorizontalGroupBuilder implements GroupBuilder {

        @Override
        public GroupImpl buildGroup(int groupID) {
            List<Cell> group = new ArrayList<>();
            int hIndex = MAX_CELLS_IN_GROUP * groupID;
            for (int i=0; i<MAX_CELLS_IN_GROUP; i++) {
                int index = hIndex + i;
                group.add(cells.get(index));
            }
            return new GroupImpl(groupID, group);
        }
    }

    private class VerticalGroupBuilder implements GroupBuilder {

        @Override
        public GroupImpl buildGroup(int groupID) {
            List<Cell> group = new ArrayList<>();
            for (int i=0; i<MAX_CELLS_IN_GROUP; i++) {
                int index = (MAX_CELLS_IN_GROUP * i) + groupID;
                group.add(cells.get(index));
            }
            return new GroupImpl(groupID, group);
        }
    }

    private class CubeGroupBuilder implements GroupBuilder {

        @Override
        public GroupImpl buildGroup(int groupID) {
            List<Cell> group = new ArrayList<>();
            for (int v=0; v<MAX_CELLS_IN_CUBE; v++) {
                // The following formula will work because integer division will
                // drop the remainder (ex: 4/3 = 1, then 1 * 3 = 3).
                int vIndex = ((groupID / MAX_CELLS_IN_CUBE) * MAX_CELLS_IN_CUBE) + v;
                for (int h=0; h<MAX_CELLS_IN_CUBE; h++) {
                    int hIndex = ((groupID % MAX_CELLS_IN_CUBE) * MAX_CELLS_IN_CUBE) + h;
                    int index = (vIndex * MAX_CELLS_IN_GROUP) + hIndex;
                    group.add(cells.get(index));
                }
            }
            return new GroupImpl(groupID, group);
        }
    }

    private class CellImpl implements Cell {
        private final int id, horizontalID, verticalID, cubeID;
        private AtomicBoolean isImmutable;
        private AtomicInteger value;
        private Set<Integer> availableCache;

        public CellImpl(int id, int value) {
            this.id = id;
            this.value = new AtomicInteger(value);
            this.isImmutable = new AtomicBoolean(value > NO_VALUE);
            this.horizontalID = id / MAX_CELLS_IN_GROUP;
            this.verticalID = id % MAX_CELLS_IN_GROUP;
            this.cubeID = ((horizontalID / MAX_CELLS_IN_CUBE) * MAX_CELLS_IN_CUBE) + (verticalID / MAX_CELLS_IN_CUBE);
        }

        public void lockCell() {
            isImmutable.set(true);
        }

        public void unlockCell() {
            isImmutable.set(false);
        }

        /**
         * Sets the cell value if the cell can be modified.
         */
        public void setValue(int value) {
            if (this.isImmutable.get()) {
                throw new UnsupportedOperationException("Attempted to alter immutable cell value ["+this.value+"]");
            }
            this.value.set(value);
        }

        @Override
        public boolean isLocked() {
            return isImmutable.get();
        }

        @Override
        public int getValue() {
            return value.get();
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public Group getHorizontalGroup() {
            return horizontals.get(horizontalID);
        }

        @Override
        public Group getVerticalGroup() {
            return verticals.get(verticalID);
        }

        @Override
        public Group getCubeGroup() {
            return cubes.get(cubeID);
        }

        @Override
        public boolean isEmpty() {
            return value.get() == NO_VALUE;
        }

        @Override
        public Set<Integer> getAvailableValues() {
            if (isImmutable.get())
                return Collections.emptySet();

            if (availableCache != null)
                return availableCache;

            int h = ((GroupImpl)getHorizontalGroup()).getAvailableValues();
            int v = ((GroupImpl)getVerticalGroup()).getAvailableValues();
            int c = ((GroupImpl)getCubeGroup()).getAvailableValues();
            int a = h & v & c;

            Set<Integer> available = new HashSet<>();
            for (Integer test : LEGAL_VALUES) {
                int value = new Double(Math.pow(2, test.intValue() - 1)).intValue();
                int t = a & value;
                if (t > 0)
                    available.add(test);
            }
            
            availableCache = Collections.unmodifiableSet(available);
            return availableCache;
        }

        public void resetAvailableCache() {
            availableCache = null;
        }
    }

    private class GroupImpl implements Group {
        private final int id;
        private final List<Cell> cells;
        private Integer availableCache;

        public GroupImpl(int id, List<Cell> cells) {
            this.id = id;
            this.cells = Collections.unmodifiableList(cells);
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public List<Cell> getCells() {
            return cells;
        }

        public int getAvailableValues() {
            if (availableCache != null)
                return availableCache;

            int all = new Double(Math.pow(2, MAX_CELLS_IN_GROUP)).intValue() - 1;
            int row = 0;
            for (Cell cell: cells) {
                int value = cell.getValue();
                if (value != NO_VALUE)
                    row += new Double(Math.pow(2, value - 1)).intValue();
            }
            availableCache = all ^ row;
            return availableCache;
        }

        public void resetAvailableCache() {
            availableCache = null;
            for (Cell cell: cells)
                ((CellImpl)cell).resetAvailableCache();
        }
    }

    public static interface ChangeListener {
        void onChange(Cell cell, int oldValue);
    }

    public static interface ChangeListenerRegistration {
        void unregister();
    }
}