package com.sudoku.dj.sudokusolver.solver;

public class CellModelManager {
    private static CellModel cellModel;

    /**
     * Instantiates the global cell model
     * @param filledCells The number of initially filled cells
     * @return
     */
    public static CellModel buildNewBoard(int filledCells) {
        if (cellModel == null) {
            cellModel = new CellModel();
        } else {
            cellModel.resetAllCells();
        }
        BoardGenerator.buildSolvedBoard(filledCells, cellModel);
        SolveTaskManager.clearAllStats();
        return cellModel;
    }

    public static boolean isNewBoardTaskRunning() {
        return BoardGenerator.isNewBoardTaskRunning();
    }

    /**
     * Instantiates the global cell model
     * @param filledCells The number of initially filled cells
     * @return
     */
    public static CellModel.ChangeListenerRegistration buildNewBoard(int filledCells, CellModel.ChangeListener listener) {
        cellModel = new CellModel();
        CellModel.ChangeListenerRegistration reg = cellModel.addListener(listener);
        BoardGenerator.buildSolvedBoard(filledCells, cellModel);
        SolveTaskManager.clearAllStats();
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
