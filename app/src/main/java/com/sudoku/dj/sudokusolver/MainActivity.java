package com.sudoku.dj.sudokusolver;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.sudoku.dj.sudokusolver.solver.CellModelManager;
import com.sudoku.dj.sudokusolver.solver.CurrentSolverStatsManager;
import com.sudoku.dj.sudokusolver.tasks.BackgroundTaskManager;
import com.sudoku.dj.sudokusolver.tasks.SolveTask;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout frame = new FrameLayout(this);
        frame.setId(BoardFragment.CONTEXT_ID);
        setContentView(frame, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (savedInstanceState == null) {
            Fragment fragment = new BoardFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(BoardFragment.CONTEXT_ID, fragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void resetSolveButtonIcon() {
        menu.getItem(1).setIcon(android.R.drawable.ic_media_play);
    }

    private void onBuildNewBoardClick() {
        if (BackgroundTaskManager.getInstance().isSolvingBoard()) {
            Toast.makeText(this, "Cannot create a new board while solving the current board", Toast.LENGTH_SHORT).show();
        } else if (BackgroundTaskManager.getInstance().isCreatingNewBoard()) {
            Toast.makeText(this, "New board task is currently running", Toast.LENGTH_SHORT).show();
        } else {
            resetSolveButtonIcon();
            CellModelManager.buildNewBoard(getAssets());
        }
    }

    private void onSolveClick(MenuItem item) {
        if (BackgroundTaskManager.getInstance().isCreatingNewBoard()) {
            // do nothing
            return;
        }

        try {
            if (CellModelManager.getInstance().isSolved()) {
                String message = buildSolvedMessage(CurrentSolverStatsManager.getInstance().getSolveStats());
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                return;
            }

            if (BackgroundTaskManager.getInstance().isSolvingBoard()) {
                item.setIcon(android.R.drawable.ic_media_play);
                BackgroundTaskManager.getInstance().cancelTask();
                return;
            }

            item.setIcon(android.R.drawable.ic_media_pause);
            SolveTask task = new SolveTask(new SolveListenerImpl(this));
            BackgroundTaskManager.getInstance().runTask(task, CellModelManager.getInstance());
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String buildSolvedMessage(SolveTask.SolveStats stats) {
        SimpleDateFormat df = new SimpleDateFormat("mm:ss.SSS");
        return new StringBuilder()
            .append("Solved in "+df.format(new Date(stats.getElapsedTime())))
            .append(" in "+stats.getSteps()+" steps")
                .toString();
    }

    private void onResetClick() {
        if (BackgroundTaskManager.getInstance().isCreatingNewBoard()) {
            // do nothing
            return;
        }

        if (BackgroundTaskManager.getInstance().isSolvingBoard()) {
            Toast.makeText(this, "Cannot reset board while solving", Toast.LENGTH_SHORT).show();
        } else {
            CellModelManager.getInstance().resetCells();
            CurrentSolverStatsManager.getInstance().clearAllStats();
        }
    }

    private void onAboutClick() {
        String message = new StringBuilder()
                .append("by DJ Kwiatkowski, June 2018\n\n")
                .append("Image Sources: \n")
                .append("Material Icons, Apache 2.0\n")
                .append("Oxygen Icon Team, LGPL\n")
                .append("Pixabay, CC0 Creative Commons")
                .toString();
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("About Sudoku Solver");
        dialog.setMessage(message);
        dialog.setIcon(R.mipmap.ksudoku_icon);
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.new_board_button) {
            onBuildNewBoardClick();
        } else if (id == R.id.solve_board_button) {
            onSolveClick(item);
        } else if (id == R.id.reset_board_button) {
            onResetClick();
        } else if (id == R.id.about_app) {
            onAboutClick();
        }
        return super.onOptionsItemSelected(item);
    }

    private static class SolveListenerImpl implements SolveTask.SolverListener {
        private final MainActivity activity;

        public SolveListenerImpl(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onPaused(SolveTask.SolveStats stats) {
            // no-op
        }

        @Override
        public void onSolved(SolveTask.SolveStats stats) {
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
            activity.resetSolveButtonIcon();
            String message = activity.buildSolvedMessage(stats);
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLongRunningTask(SolveTask.SolveStats stats) {
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
            SimpleDateFormat df = new SimpleDateFormat("mm:ss.SSS");
            String message = new StringBuilder()
                    .append("Run time: "+df.format(new Date(stats.getElapsedTime())))
                    .toString();
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }
}
