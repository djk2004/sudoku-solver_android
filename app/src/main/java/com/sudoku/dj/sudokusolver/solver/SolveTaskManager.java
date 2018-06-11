package com.sudoku.dj.sudokusolver.solver;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class SolveTaskManager {
    private static SolveTask task;
    private static AllStats allStats = new AllStats();
//    private static final Comparator<Cell> defaultComparator = new Solver.DefaultComparator();
//    private static final List<Comparator<Cell>> comparators;

//    static {
//        List<Comparator<Cell>> c = new ArrayList<>();
//        c.add(new Solver.CubeGroupComparator());
//        c.add(new Solver.HorizontalGroupComparator());
//        c.add(new Solver.VerticalGroupComparator());
//        c.add(new Solver.IDComparator());
//        c.add(new Solver.MixedGroupComparator());
//        c.add(new Solver.CellGroupsComparator());
//        comparators = Collections.unmodifiableList(c);
//    }

    /**
     * Cancels the running solve task.
     */
    public static void cancelSolve() {
        if (task != null) {
            task.canCancelSolve();
        }
    }

    public static boolean isSolvingBoard() {
        return task != null && task.isRunning();
    }

    public static void solve(SolverListener solverListener) {
        if (task != null && task.isCancelled()) {
            throw new RuntimeException("Task currently running!");
        }
        task = new SolveTask(solverListener);
        task.execute(CellModelManager.getInstance());
    }

    public static void clearAllStats() {
        allStats.clear();
    }

    public static SolveStats getSolveStats() {
        return allStats.get();
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

    private static class SolveTask extends AsyncTask<CellModel, Integer, SolveStats> {
        private int attempts, steps;
        private long start, elapsed;
        private AtomicBoolean isRunning = new AtomicBoolean(false);
        private AtomicBoolean canCancel = new AtomicBoolean(false);

        private final SolverListener solverListener;

        public SolveTask(final SolverListener solverListener) {
            this.solverListener = solverListener;
        }

        @Override
        protected SolveStats doInBackground(CellModel... models) {
            isRunning.set(true);
            start = System.currentTimeMillis();
            try {
                CellModel model = models[0];
                solve(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
            elapsed = System.currentTimeMillis() - start;
            isRunning.set(false);
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
            allStats.add(current);
            SolveStats all = allStats.get();
            return all;
        }

        public boolean isRunning() {
            return isRunning.get();
        }

        public void canCancelSolve() {
            canCancel.set(true);
        }

        private void solve(CellModel model) {
            attempts = 0;
            steps = 0;
            canCancel.set(false);
            do
            {
                if (++attempts > 1) {
                    model.resetCells();
                    if (attempts % 1000 == 0) {
                        publishProgress(attempts);
                    }
                }
                Solver solver = new Solver(model, Solver.SolverType.BACKTRACKING);
                steps += solver.solve(canCancel);
            } while (!model.isSolveable() && !canCancel.get());
        }

//        private Comparator<Cell> buildComparator() {
//            int totalComparators = comparators.size();
//            int strategyID = new Random(System.currentTimeMillis()).nextInt(totalComparators * 2);
//
//            if (strategyID < totalComparators) {
//                return comparators.get(strategyID);
//            }
//
//            // the default comparator should be more heavily weighted than the others
//            return defaultComparator;
//        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // this is not meant to be included in the list, instead, it is a
            // progress update specific to this thread instance
            SolveStats stats = new SolveStats() {
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
            };
            solverListener.onLongRunningTask(stats);
        }

        @Override
        protected void onPostExecute(SolveStats stats) {
            if (steps == 0) {
                return;
            }
            allStats.add(stats);
            SolveStats all = allStats.get();
            if (canCancel.get()) {
                solverListener.onPaused(all);
            } else {
                solverListener.onSolved(all);
            }
        }
    }

    private static class AllStats {
        private List<SolveStats> solveStatsList = new ArrayList<>();

        public void clear() {
            solveStatsList.clear();
        }

        public void add(SolveStats stats) {
            solveStatsList.add(stats);
        }

        public SolveStats get() {
            return new CumulativeSolveStats(solveStatsList);
        }
    }

    private static class CumulativeSolveStats implements SolveStats {
        private int attempts, steps;
        private long elapsed;

        public CumulativeSolveStats(List<SolveStats> solveStatsList) {
            for (SolveStats s: solveStatsList) {
                attempts += s.getAttempts();
                steps += s.getSteps();
                elapsed += s.getElapsedTime();
            }
        }

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
    }
}
