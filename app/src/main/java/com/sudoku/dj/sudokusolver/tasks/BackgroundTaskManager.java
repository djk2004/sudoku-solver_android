package com.sudoku.dj.sudokusolver.tasks;

import android.os.AsyncTask;
import com.sudoku.dj.sudokusolver.solver.CellModel;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundTaskManager {
    private BackgroundTask<?> task;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean canCancel;

    private static BackgroundTaskManager instance;

    public static BackgroundTaskManager getInstance() {
        if (instance == null) {
            instance = new BackgroundTaskManager();
        }
        return instance;
    }

    private BackgroundTaskManager() {
        this.isRunning = new AtomicBoolean(false);
        this.canCancel = new AtomicBoolean(false);
    }

    public void runTask(BackgroundTaskWork<?> work, CellModel model) {
        if (isRunning.get()) {
            throw new RuntimeException("Task is currently running");
        }
        this.canCancel.set(false);
        this.task = new BackgroundTask(isRunning, canCancel, work);
        this.task.execute(model);
    }

    public boolean isTaskRunning() {
        return this.isRunning.get();
    }

    public void cancelTask() {
        if (isTaskRunning() && !this.canCancel.get()) {
            this.canCancel.set(true);
        }
    }

    // HACK: need a cleaner way of getting the running task type
    @Deprecated
    public boolean isSolvingBoard() {
        if (!isTaskRunning()) {
            return false;
        }
        return this.task.getWork() instanceof SolveTask;
    }

    // HACK: need a cleaner way of getting the running task type
    @Deprecated
    public boolean isCreatingNewBoard() {
        if (!isTaskRunning()) {
            return false;
        }
        BackgroundTaskWork<?> work = this.task.getWork();
        return work instanceof BoardGeneratorTask || work instanceof MaskBoardGeneratorTask;
    }

    // HACK: probably shouldn't expose the AtomicBoolean here
    @Deprecated
    public AtomicBoolean isCurrentTaskCancelled() {
        if (!isTaskRunning()) {
            throw new RuntimeException("Task is currently not running");
        }
        return this.canCancel;
    }

    public static interface BackgroundTaskWork<T> {
        T doWork(CellModel model);
        void onUpdate(Integer... progress);
        void onFinish(T result);
    }

    private static class BackgroundTask<T> extends AsyncTask<CellModel, Integer, T> {
        private final AtomicBoolean isRunning, canCancel;
        private BackgroundTaskWork<T> work;

        public BackgroundTask(AtomicBoolean isRunning, AtomicBoolean canCancel, BackgroundTaskWork<T> work) {
            this.isRunning = isRunning;
            this.canCancel = canCancel;
            this.work = work;
        }

        public BackgroundTaskWork<T> getWork() {
            return work;
        }

        @Override
        protected final T doInBackground(CellModel... models) {
            isRunning.set(true);
            return work.doWork(models[0]);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            work.onUpdate(progress);
        }

        @Override
        protected final void onPostExecute(T result) {
            try {
                work.onFinish(result);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isRunning.set(false);
            }
        }

        public void cancelTask() {
            canCancel.set(true);
        }
    }
}
