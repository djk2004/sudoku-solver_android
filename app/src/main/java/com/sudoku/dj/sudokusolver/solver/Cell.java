package com.sudoku.dj.sudokusolver.solver;

import java.util.Set;

/**
 * The Cell is a single position on the Sudoku board.  Cells have a set value, and 
 * relationships with other cell groups, defined by the horizontal, vertical, and cube groups.
 * Each group has 9 cells (although this could probably be adapted to handle an arbitrary
 * sized board).  
 */
public interface Cell {

    /**
     * Returns the cell ID.
     */
    int getID();

    /**
     * The cell value, which when set is between 1 and 9.  A 0 value is used to show no data.
     */
    int getValue();

    /**
     * Returns the horizontal group.
     */
    Group getHorizontalGroup();

    /**
     * Returns the vertical group.
     */
    Group getVerticalGroup();

    /**
     * Returns the cube group.
     */
    Group getCubeGroup();

    /**
     * Returns the set of possible values available to the cell.  When the cell contains a 
     * non-zero value, this returns an empty set.  However, when the cell contains a 0,
     * this will return a set of possible legal values, such that each value returned
     * is not used by any other cells in the horizontal, vertical, or cube groups.
     */
    Set<Integer> getAvailableValues();
}