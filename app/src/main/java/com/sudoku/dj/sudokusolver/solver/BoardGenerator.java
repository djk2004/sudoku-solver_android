package com.sudoku.dj.sudokusolver.solver;

import android.os.AsyncTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;

public class BoardGenerator {
    private static GeneratorTask task;
    private static AtomicBoolean isRunning = new AtomicBoolean(false);

    public static void buildSolvedBoard(int filledCells, CellModel unsolved, CellModel.ChangeListener listener) {
        if (isRunning.get()) {
            throw new RuntimeException("Board generation task is currently running");
        }
        task = new GeneratorTask(filledCells, isRunning, listener);
        task.execute(unsolved);
    }

    public static boolean isNewBoardTaskRunning() {
        return isRunning.get();
    }

    private static class GeneratorTask extends AsyncTask<CellModel, Integer, CellModel> {
        private ScheduledExecutorService cancelExecutor = Executors.newSingleThreadScheduledExecutor();
        private ExecutorService jobExecutor = Executors.newSingleThreadExecutor();
        private final int filledCells;
        private final AtomicBoolean isRunning;
        private final CellModel.ChangeListener listener;

        public GeneratorTask(int filledCells, AtomicBoolean isRunning, CellModel.ChangeListener listener) {
            this.filledCells = filledCells;
            this.isRunning = isRunning;
            this.listener = listener;
        }

        @Override
        protected CellModel doInBackground(CellModel... models) {
            isRunning.set(true);
            CellModel unsolved = models[0];
            CellModel.ChangeListenerRegistration reg = null;
            try {
                CellModel solved = new CellModel();
                reg = solved.addListener(listener);
                while (!solved.isSolved()) {
                    getSolvedBoard(solved);
                }
                buildUnsolvedBoard(solved, unsolved, filledCells);
            } catch (InterruptedException e) {
                // do nothing, ignoring unsolvable boards
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reg != null) {
                    reg.unregister();
                }
            }
            return unsolved;
        }

        private void getSolvedBoard(final CellModel model) throws InterruptedException {
            final AtomicBoolean canCancel = new AtomicBoolean(false);
            final Future<?> future = jobExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    int attempts = 0;
                    do
                    {
                        if (++attempts > 1) {
                            model.resetCells();
                        }
                        Solver solver = new Solver(model, Solver.SolverType.BACKTRACKING);
                        solver.solve(canCancel);
                    } while (!model.isSolved());
                }
            });
            cancelExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    canCancel.set(true);
                    future.cancel(true);
                }
            }, 5, TimeUnit.MINUTES);

            try {
                // This should never take the maximum time to complete
                future.get();
            } catch (Exception e) {
                throw new InterruptedException(e.getMessage());
            }
        }

        private void buildUnsolvedBoard(CellModel solved, CellModel unsolved, int filledCells) {
            int cellCount = 0;
            Random r = new Random(System.currentTimeMillis());
            while (cellCount < filledCells) {
                int h = r.nextInt(CellModel.MAX_CELLS_IN_GROUP);
                int v = r.nextInt(CellModel.MAX_CELLS_IN_GROUP);
                Cell solvedCell = solved.getHorizontalGroup(h).getCells().get(v);
                Cell target = unsolved.getHorizontalGroup(h).getCells().get(v);
                if (target.getValue() > 0) {
                    continue;
                }
                unsolved.setValue(target, solvedCell.getValue());
                cellCount++;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // TODO: show progress somehow
        }

        @Override
        protected void onPostExecute(CellModel model) {
            try {
                model.lockFilledCells();
                model.resetCells();
                jobExecutor.shutdownNow();
                cancelExecutor.shutdownNow();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                isRunning.set(false);
            }
        }
    }
}

