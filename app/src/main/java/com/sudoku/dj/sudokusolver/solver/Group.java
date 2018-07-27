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
}