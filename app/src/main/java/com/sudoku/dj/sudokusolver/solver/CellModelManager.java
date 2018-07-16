package com.sudoku.dj.sudokusolver.solver;

import android.content.res.AssetManager;

import com.sudoku.dj.sudokusolver.MainActivity;
import com.sudoku.dj.sudokusolver.tasks.BackgroundTaskManager;
import com.sudoku.dj.sudokusolver.tasks.BoardGeneratorTask;
import com.sudoku.dj.sudokusolver.tasks.MaskBoardGeneratorTask;

import java.util.Random;

public class CellModelManager {
    private static CellModel cellModel;

    /**
     * Instantiates the global cell model
     * @return
     */
    public static CellModel buildNewBoard(MainActivity activity) {
        if (cellModel == null) {
            cellModel = new CellModel();
        } else {
            cellModel.resetAllCells();
        }
        Random r = new Random(System.currentTimeMillis());
        int filledCells = r.nextInt(15) + 17;
        MaskBoardGeneratorTask task = new MaskBoardGeneratorTask(filledCells, activity);
        BackgroundTaskManager.getInstance().runTask(task, cellModel);
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
