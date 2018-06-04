package com.sudoku.dj.sudokusolver.solver;

import java.util.List;
import java.util.Set;

/**
 * The group is defined as a set of 9 cells, where in a completed Sudoku board, all nine 
 * cells have unique values between 1 and 9.
 */
public interface Group {
    /**
     * The group ID.
     */
    int getID();

    /**
     * Gets the list of cells associated with this group.
     */
    List<Cell> getCells();

    /**
     * Gets the list of all values that have not been assigned to a cell in the group.
     */
    Set<Integer> getAvailableValues();
}