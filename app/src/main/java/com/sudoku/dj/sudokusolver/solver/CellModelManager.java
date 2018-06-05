package com.sudoku.dj.sudokusolver.solver;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class CellModelManager {
    private static CellModel cellModel;
    private static SolveTask task;

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
        Solver solver = new Solver(cellModel);
        solver.buildNewBoard(filledCells);
        return cellModel;
    }

    /**
     * Instantiates the global cell model
     * @param filledCells The number of initially filled cells
     * @return
     */
    public static CellModel.ChangeListenerRegistration buildNewBoard(int filledCells, CellModel.ChangeListener listener) {
        cellModel = new CellModel();
        CellModel.ChangeListenerRegistration reg = cellModel.addListener(listener);
        Solver solver = new Solver(cellModel);
        solver.buildNewBoard(filledCells);
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
     * Cancels the running solve task.
     */
    public static void cancelSolve() {
        if (task != null) {
            task.canCancelSolve();
//            task.cancel(true);
        }
    }

    /**
     * Returns true if the cell model has been initialized
     * @return
     */
    public static boolean isModelInitialized() {
        return cellModel != null;
    }

    public static boolean isSolvingBoard() {
        return task != null && task.isRunning();
    }

    public static void solve(Activity activity) {
        if (task != null && task.isCancelled()) {
            throw new RuntimeException("Task currently running!");
        }
        task = new SolveTask(activity);
        task.execute(getInstance());
    }

    public static interface SolveStats {
        int getAttempts();
        int getSteps();
        long getElapsedTime();
    }

    private static class SolveTask extends AsyncTask<CellModel, Integer, SolveStats> {
        private int attempts, steps;
        private long start, elapsed;
        private boolean canCancel, isRunning = false;

        private final Activity activity;

        public SolveTask(final Activity activity) {
            this.activity = activity;
        }

        @Override
        protected SolveStats doInBackground(CellModel... models) {
            isRunning = true;
            start = System.currentTimeMillis();
            try {
                CellModel model = models[0];
                solve(model);
            } catch (Exception e) {
                System.err.println("Exception caught ["+e.getMessage()+"], probably due to cancelled task");
            }
            elapsed = System.currentTimeMillis() - start;
            isRunning = false;
            return new SolveStats() {
                @Override
                public int getAttempts() {
                    return attempts;
                }

                @Override
                public int getSteps() {
                    return steps;
                }

                @Override
                public long getElapsedTime() {
                    return elapsed;
                }
            };
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void canCancelSolve() {
            canCancel = true;
        }

        private void solve(CellModel model) {
            attempts = 0;
            steps = 0;
            canCancel = false;
            Random r = new Random(start);
            do
            {
                if (++attempts > 1) {
                    model.resetCells();
                }
                Solver solver = new Solver(model);
                boolean backtrack = r.nextBoolean();
                steps += solver.solve(backtrack);
            } while (!model.isSolveable() && !canCancel);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

        }

        @Override
        protected void onPostExecute(SolveStats stats) {
            if (activity.isDestroyed() || activity.isFinishing() || canCancel || steps == 0)
            {
                return;
            }
            SimpleDateFormat df = new SimpleDateFormat("mm:ss.SSS");
            StringBuilder builder = new StringBuilder();
            builder.append("Solved in "+df.format(new Date(stats.getElapsedTime())));
            builder.append(" in "+stats.getSteps()+" steps");
            builder.append(" with "+stats.getAttempts()+" attempts");
            Toast.makeText(activity, builder.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
