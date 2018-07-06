package com.sudoku.dj.sudokusolver.solver;

import com.sudoku.dj.sudokusolver.tasks.BackgroundTaskManager;
import com.sudoku.dj.sudokusolver.tasks.BoardGeneratorTask;
import com.sudoku.dj.sudokusolver.tasks.MaskBoardGeneratorTask;

public class CellModelManager {
    private static CellModel cellModel;

    /**
     * Instantiates the global cell model
     * @param filledCells The number of initially filled cells
     * @return
     */
    public static CellModel buildNewBoard(int filledCells, CellModel.ChangeListener listener) {
        if (cellModel == null) {
            cellModel = new CellModel();
        } else {
            cellModel.resetAllCells();
        }
//        BoardGeneratorTask task = new BoardGeneratorTask(filledCells, listener);
        MaskBoardGeneratorTask task = new MaskBoardGeneratorTask(filledCells);
        BackgroundTaskManager.getInstance().runTask(task, cellModel);
        CurrentSolverStatsManager.getInstance().clearAllStats();
        return cellModel;
    }

    /**
     * Instantiates the global cell model
     * @return
     */
    public static CellModel.ChangeListenerRegistration initializeModel(CellModel.ChangeListener listener) {
        cellModel = new CellModel();
        CellModel.ChangeListenerRegistration reg = cellModel.addListener(listener);
        return reg;
    }

    /**
     * Returns the current state of the global cell model. This will throw an exception
     * if buildNewBoard() is not called first.
     * @return
     */
    public static CellModel getInstance() {
        if (cellModel == null) {
            throw new NullPointerException("Cell model not initialized");
        }
        return cellModel;
    }

    /**
     * Returns true if the cell model has been initialized
     * @return
     */
    public static boolean isModelInitialized() {
        return cellModel != null;
    }
}
