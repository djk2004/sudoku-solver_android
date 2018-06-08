package com.sudoku.dj.sudokusolver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.sudoku.dj.sudokusolver.solver.Cell;
import com.sudoku.dj.sudokusolver.solver.CellModel;
import com.sudoku.dj.sudokusolver.solver.CellModelManager;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private CellModel.ChangeListenerRegistration reg;
    private Map<Integer, Integer> cellIDsToBoxIDs;
    private Menu menu;

    private int generateFilledCellsCount() {
        Random r = new Random(System.currentTimeMillis());
        return r.nextInt(15) + 10;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!CellModelManager.isModelInitialized()) {
            // This should only happen when the activity is first created on application start
            cellIDsToBoxIDs = buildCellIDsMap();
            int filledCells = generateFilledCellsCount();
            reg = CellModelManager.buildNewBoard(filledCells, new CellModel.ChangeListener() {
                @Override
                public void onChange(final Cell cell, int oldValue) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Integer id = Integer.valueOf(cell.getID());
                            TextView text = (TextView)findViewById(cellIDsToBoxIDs.get(id));
                            int value = cell.getValue();
                            String textValue = value == 0 ? "" : ""+value;
                            text.setTextColor(cell.isLocked() ?
                                    ContextCompat.getColor(text.getContext(), android.R.color.black) :
                                    ContextCompat.getColor(text.getContext(), R.color.colorPrimaryDark));
                            text.setText(textValue);
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (reg != null) {
            reg.unregister();
        }
        super.onDestroy();
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
        if (CellModelManager.isSolvingBoard()) {
            Toast.makeText(this, "Cannot create a new board while solving the current board", Toast.LENGTH_SHORT).show();
        } else {
            resetSolveButtonIcon();

            int filledCells = generateFilledCellsCount();
            CellModelManager.buildNewBoard(filledCells);
        }
    }

    private void onSolveClick(MenuItem item) {
        try {
            if (CellModelManager.getInstance().isSolved()) {
                Toast.makeText(this, "Puzzle solved!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (CellModelManager.isSolvingBoard()) {
                item.setIcon(android.R.drawable.ic_media_play);
                CellModelManager.cancelSolve();
                return;
            }

            item.setIcon(android.R.drawable.ic_media_pause);
            final MainActivity activity = this;
            CellModelManager.solve(new CellModelManager.SolverListener() {
                @Override
                public void onSolved(CellModelManager.SolveStats stats) {
                    if (activity.isDestroyed() || activity.isFinishing()) {
                        return;
                    }
                    activity.resetSolveButtonIcon();

                    SimpleDateFormat df = new SimpleDateFormat("mm:ss.SSS");
                    StringBuilder builder = new StringBuilder();
                    builder.append("Solved in "+df.format(new Date(stats.getElapsedTime())));
                    builder.append(" in "+stats.getSteps()+" steps");
                    Toast.makeText(activity, builder.toString(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onLongRunningTask(CellModelManager.SolveStats stats) {
                    if (activity.isDestroyed() || activity.isFinishing()) {
                        return;
                    }
                 Toast.makeText(activity, "This puzzle may be unsolvable...", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onResetClick() {
        if (CellModelManager.isSolvingBoard()) {
            Toast.makeText(this, "Cannot reset board while solving", Toast.LENGTH_SHORT).show();
        } else {
            CellModelManager.getInstance().resetCells();
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

    // HACK: probably a better way to do this
    private Map<Integer, Integer> buildCellIDsMap() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(Integer.valueOf(0), R.id.box0);
        map.put(Integer.valueOf(1), R.id.box1);
        map.put(Integer.valueOf(2), R.id.box2);
        map.put(Integer.valueOf(3), R.id.box3);
        map.put(Integer.valueOf(4), R.id.box4);
        map.put(Integer.valueOf(5), R.id.box5);
        map.put(Integer.valueOf(6), R.id.box6);
        map.put(Integer.valueOf(7), R.id.box7);
        map.put(Integer.valueOf(8), R.id.box8);
        map.put(Integer.valueOf(9), R.id.box9);
        map.put(Integer.valueOf(10), R.id.box10);
        map.put(Integer.valueOf(11), R.id.box11);
        map.put(Integer.valueOf(12), R.id.box12);
        map.put(Integer.valueOf(13), R.id.box13);
        map.put(Integer.valueOf(14), R.id.box14);
        map.put(Integer.valueOf(15), R.id.box15);
        map.put(Integer.valueOf(16), R.id.box16);
        map.put(Integer.valueOf(17), R.id.box17);
        map.put(Integer.valueOf(18), R.id.box18);
        map.put(Integer.valueOf(19), R.id.box19);
        map.put(Integer.valueOf(20), R.id.box20);
        map.put(Integer.valueOf(21), R.id.box21);
        map.put(Integer.valueOf(22), R.id.box22);
        map.put(Integer.valueOf(23), R.id.box23);
        map.put(Integer.valueOf(24), R.id.box24);
        map.put(Integer.valueOf(25), R.id.box25);
        map.put(Integer.valueOf(26), R.id.box26);
        map.put(Integer.valueOf(27), R.id.box27);
        map.put(Integer.valueOf(28), R.id.box28);
        map.put(Integer.valueOf(29), R.id.box29);
        map.put(Integer.valueOf(30), R.id.box30);
        map.put(Integer.valueOf(31), R.id.box31);
        map.put(Integer.valueOf(32), R.id.box32);
        map.put(Integer.valueOf(33), R.id.box33);
        map.put(Integer.valueOf(34), R.id.box34);
        map.put(Integer.valueOf(35), R.id.box35);
        map.put(Integer.valueOf(36), R.id.box36);
        map.put(Integer.valueOf(37), R.id.box37);
        map.put(Integer.valueOf(38), R.id.box38);
        map.put(Integer.valueOf(39), R.id.box39);
        map.put(Integer.valueOf(40), R.id.box40);
        map.put(Integer.valueOf(41), R.id.box41);
        map.put(Integer.valueOf(42), R.id.box42);
        map.put(Integer.valueOf(43), R.id.box43);
        map.put(Integer.valueOf(44), R.id.box44);
        map.put(Integer.valueOf(45), R.id.box45);
        map.put(Integer.valueOf(46), R.id.box46);
        map.put(Integer.valueOf(47), R.id.box47);
        map.put(Integer.valueOf(48), R.id.box48);
        map.put(Integer.valueOf(49), R.id.box49);
        map.put(Integer.valueOf(50), R.id.box50);
        map.put(Integer.valueOf(51), R.id.box51);
        map.put(Integer.valueOf(52), R.id.box52);
        map.put(Integer.valueOf(53), R.id.box53);
        map.put(Integer.valueOf(54), R.id.box54);
        map.put(Integer.valueOf(55), R.id.box55);
        map.put(Integer.valueOf(56), R.id.box56);
        map.put(Integer.valueOf(57), R.id.box57);
        map.put(Integer.valueOf(58), R.id.box58);
        map.put(Integer.valueOf(59), R.id.box59);
        map.put(Integer.valueOf(60), R.id.box60);
        map.put(Integer.valueOf(61), R.id.box61);
        map.put(Integer.valueOf(62), R.id.box62);
        map.put(Integer.valueOf(63), R.id.box63);
        map.put(Integer.valueOf(64), R.id.box64);
        map.put(Integer.valueOf(65), R.id.box65);
        map.put(Integer.valueOf(66), R.id.box66);
        map.put(Integer.valueOf(67), R.id.box67);
        map.put(Integer.valueOf(68), R.id.box68);
        map.put(Integer.valueOf(69), R.id.box69);
        map.put(Integer.valueOf(70), R.id.box70);
        map.put(Integer.valueOf(71), R.id.box71);
        map.put(Integer.valueOf(72), R.id.box72);
        map.put(Integer.valueOf(73), R.id.box73);
        map.put(Integer.valueOf(74), R.id.box74);
        map.put(Integer.valueOf(75), R.id.box75);
        map.put(Integer.valueOf(76), R.id.box76);
        map.put(Integer.valueOf(77), R.id.box77);
        map.put(Integer.valueOf(78), R.id.box78);
        map.put(Integer.valueOf(79), R.id.box79);
        map.put(Integer.valueOf(80), R.id.box80);
        return Collections.unmodifiableMap(map);
    }
}
