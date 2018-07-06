package com.sudoku.dj.sudokusolver.tasks;

import com.sudoku.dj.sudokusolver.solver.CellModel;
import com.sudoku.dj.sudokusolver.solver.SolveTaskManager;
import com.sudoku.dj.sudokusolver.solver.Solver;

public class SolveTask implements BackgroundTaskManager.BackgroundTaskWork<SolveTask.SolveStats> {
    private int attempts, steps;
    private long start, elapsed;

    private final SolveTask.SolverListener solverListener;

    public SolveTask(final SolveTask.SolverListener solverListener) {
        this.solverListener = solverListener;
    }

    @Override
    public SolveStats doWork(CellModel model) {
        start = System.currentTimeMillis();
        try {
            solve(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
        elapsed = System.currentTimeMillis() - start;
        SolveStats current = new SolveStats() {
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
        SolveTaskManager.addStats(current);
        SolveStats all = SolveTaskManager.getSolveStats();
        return all;
    }

    private void solve(CellModel model) {
        attempts = 0;
        steps = 0;
        do
        {
            if (++attempts > 1) {
                model.resetCells();
//                if (attempts % 1000 == 0) {
//                    publishProgress(attempts);
//                }
            }
            Solver solver = new Solver(model, Solver.SolverType.BACKTRACKING);
            steps += solver.solve(BackgroundTaskManager.getInstance().isCurrentTaskCancelled());
        } while (!model.isSolveable() && !BackgroundTaskManager.getInstance().isCurrentTaskCancelled().get());
    }

    @Override
    public void onUpdate(Integer... progress) {
//        // this is not meant to be included in the list, instead, it is a
//        // progress update specific to this thread instance
//        SolveTask.SolveStats stats = new SolveTask.SolveStats() {
//            @Override
//            public int getAttempts() {
//                return attempts;
//            }
//
//            @Override
//            public int getSteps() {
//                return steps;
//            }
//
//            @Override
//            public long getElapsedTime() {
//                return System.currentTimeMillis() - start;
//            }
//        };
//        solverListener.onLongRunningTask(stats);
    }

    @Override
    public void onFinish(SolveTask.SolveStats stats) {
        if (steps == 0) {
            return;
        }
        SolveTaskManager.addStats(stats);
        SolveStats all = SolveTaskManager.getSolveStats();
        if (BackgroundTaskManager.getInstance().isCurrentTaskCancelled().get()) {
            solverListener.onPaused(all);
        } else {
            solverListener.onSolved(all);
        }
    }

    public static interface SolveStats {
        int getAttempts();
        int getSteps();
        long getElapsedTime();
    }

    public static interface SolverListener {
        void onSolved(SolveStats stats);
        void onLongRunningTask(SolveStats stats);
        void onPaused(SolveStats stats);
    }
}
