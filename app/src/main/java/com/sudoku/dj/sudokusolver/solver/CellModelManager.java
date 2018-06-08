package com.sudoku.dj.sudokusolver.solver;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.sudoku.dj.sudokusolver.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

public class CellModelManager {
    private static CellModel cellModel;
    private static SolveTask task;
    private static SolverListener solverListener;

    public static SolverListenerRegistration addSolverListener(SolverListener listener) {
        solverListener = listener;
        return new SolverListenerRegistration() {
            @Override
            public void unregister() {
                solverListener = null;
            }
        };
    }

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
        if (solverListener != null) {
            solverListener.onCreateNewBoard();
        }
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
        if (solverListener != null) {
            solverListener.onCreateNewBoard();
        }
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

    public static void solve() {
        if (task != null && task.isCancelled()) {
            throw new RuntimeException("Task currently running!");
        }
        task = new SolveTask(solverListener);
        task.execute(getInstance());
    }

    public static interface SolveStats {
        int getAttempts();
        int getSteps();
        long getElapsedTime();
    }

    public static interface SolverListener {
        void onCreateNewBoard();
        void onSolved(SolveStats stats);
        void onLongRunningTask(SolveStats stats);
        void onPaused(SolveStats stats);
    }

    public static interface SolverListenerRegistration {
        void unregister();
    }

    private static class SolveTask extends AsyncTask<CellModel, Integer, SolveStats> {
        private int attempts, steps;
        private long start, elapsed;
        private boolean canCancel, isRunning = false;

        private final SolverListener solverListener;

        public SolveTask(final SolverListener solverListener) {
            this.solverListener = solverListener;
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
                    if (attempts % 800 == 0) {
                        publishProgress(attempts);
                    }
                }
                Solver solver = new Solver(model, buildComparator());
                boolean backtrack = r.nextBoolean();
                steps += solver.solve(backtrack);
            } while (!model.isSolveable() && !canCancel);
        }

        private Comparator<Cell> buildComparator() {
            int strategyID = new Random(System.currentTimeMillis()).nextInt(10);
            if (strategyID == 0) {
                return new Solver.CubeGroupComparator();
            }
            if (strategyID == 1) {
                return new Solver.HorizontalGroupComparator();
            }
            if (strategyID == 2) {
                return new Solver.VerticalGroupComparator();
            }
            if (strategyID == 3) {
                return new Solver.IDComparator();
            }
            if (strategyID == 4) {
                return new Solver.MixedGroupComparator();
            }
            // the default comparator should be more heavily weighted than the others
            return new Solver.DefaultComparator();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            solverListener.onLongRunningTask(new SolveStats() {
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
                    return System.currentTimeMillis() - start;
                }
            });
        }

        @Override
        protected void onPostExecute(SolveStats stats) {
            if (steps == 0) {
                return;
            }

            if (canCancel) {
                solverListener.onPaused(stats);
            } else {
                solverListener.onSolved(stats);
            }
        }
    }
}
