package com.sudoku.dj.sudokusolver.solver;

import com.sudoku.dj.sudokusolver.tasks.SolveTask;

import java.util.ArrayList;
import java.util.List;

public class CurrentSolverStatsManager {
    private static CurrentSolverStatsManager instance;

    private final List<SolveTask.SolveStats> allStats;

    private CurrentSolverStatsManager() {
        this.allStats = new ArrayList<>();
    }

    public static CurrentSolverStatsManager getInstance() {
        if (instance == null) {
            instance = new CurrentSolverStatsManager();
        }
        return instance;
    }

    public synchronized void clearAllStats() {
        allStats.clear();
    }

    public synchronized void addStats(SolveTask.SolveStats stats) {
        allStats.add(stats);
    }

    public synchronized SolveTask.SolveStats getSolveStats() {
        return new CumulativeSolveStats(allStats);
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
