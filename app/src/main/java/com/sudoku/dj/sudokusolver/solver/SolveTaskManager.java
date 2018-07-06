package com.sudoku.dj.sudokusolver.solver;

import com.sudoku.dj.sudokusolver.tasks.SolveTask;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Deprecated
public class SolveTaskManager {
    private static AllStats allStats = new AllStats();

    public static void clearAllStats() {
        allStats.clear();
    }

    public static SolveTask.SolveStats getSolveStats() {
        return allStats.get();
    }

    public static void addStats(SolveTask.SolveStats stats) {
        allStats.add(stats);
    }

    private static class AllStats {
        private List<SolveTask.SolveStats> solveStatsList = new CopyOnWriteArrayList<>();

        public void clear() {
            solveStatsList.clear();
        }

        public void add(SolveTask.SolveStats stats) {
            solveStatsList.add(stats);
        }

        public SolveTask.SolveStats get() {
            return new CumulativeSolveStats(solveStatsList);
        }
    }

    private static class CumulativeSolveStats implements SolveTask.SolveStats {
        private int attempts, steps;
        private long elapsed;

        public CumulativeSolveStats(List<SolveTask.SolveStats> solveStatsList) {
            for (SolveTask.SolveStats s: solveStatsList) {
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
