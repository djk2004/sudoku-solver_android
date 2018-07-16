package com.sudoku.dj.sudokusolver.tasks;

import com.sudoku.dj.sudokusolver.MainActivity;
import com.sudoku.dj.sudokusolver.solver.Cell;
import com.sudoku.dj.sudokusolver.solver.CellModel;
import com.sudoku.dj.sudokusolver.solver.CurrentSolverStatsManager;
import com.sudoku.dj.sudokusolver.solver.Solver;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BoardGeneratorTask implements BackgroundTaskManager.BackgroundTaskWork<CellModel> {
    private ScheduledExecutorService cancelExecutor = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService jobExecutor = Executors.newSingleThreadExecutor();
    private final int filledCells;
    private final CellModel.ChangeListener listener;
    private MainActivity activity;

    public BoardGeneratorTask(int filledCells, CellModel.ChangeListener listener, MainActivity activity) {
        this.filledCells = filledCells;
        this.listener = listener;
        this.activity = activity;
    }

    @Override
    public CellModel doWork(CellModel unsolved) {
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
        final AtomicBoolean canCancelSolver = new AtomicBoolean(false);
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
                    solver.solve(canCancelSolver);
                } while (!model.isSolved());
            }
        });
        cancelExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                canCancelSolver.set(true);
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
    public void onUpdate(Integer... progress) {
        // no-op
    }

    @Override
    public void onFinish(CellModel model) {
        model.lockFilledCells();
        model.resetCells();
        jobExecutor.shutdownNow();
        cancelExecutor.shutdownNow();
        CurrentSolverStatsManager.getInstance().clearAllStats();
        if (!activity.isFinishing() && !activity.isDestroyed())
            activity.returnToBoardFragment();
    }
}
