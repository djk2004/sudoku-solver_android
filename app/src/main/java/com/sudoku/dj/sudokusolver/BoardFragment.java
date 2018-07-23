package com.sudoku.dj.sudokusolver;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sudoku.dj.sudokusolver.solver.Cell;
import com.sudoku.dj.sudokusolver.solver.CellModel;
import com.sudoku.dj.sudokusolver.solver.CellModelManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BoardFragment extends Fragment {
    public static final int CONTEXT_ID = 1;

    private Map<Integer, Integer> cellIDsToBoxIDs;
    private ScheduledExecutorService executorService;
    private CellModel.ChangeListenerRegistration reg;
    private Set<Cell> changes;

    public BoardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_board, container, false);
        cellIDsToBoxIDs = buildCellIDsMap();
        changes = new HashSet<>();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new UpdateTask(), 5, 100, TimeUnit.MILLISECONDS);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!CellModelManager.isModelInitialized()) {
            // must run after onCreateView() returns
            MainActivity activity = (MainActivity)getActivity();
            activity.showProgressFragment();
            CellModelManager.buildNewBoard(activity);

            reg = CellModelManager.getInstance().addListener(new CellModel.ChangeListener() {
                @Override
                public void onChange(Cell cell, int oldValue) {
                    synchronized (changes) {
                        changes.add(cell);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        View view = getView();
        for (Cell cell: CellModelManager.getInstance().getFilledCells()) {
            updateCellView(cell, view);
        }
    }

    private void updateCellView(Cell cell, View view) {
        Integer id = Integer.valueOf(cell.getID());
        Integer viewID = cellIDsToBoxIDs.get(id);
        TextView text = (TextView)view.findViewById(viewID);
        int value = cell.getValue();
        String textValue = value == 0 ? "" : ""+value;
        text.setTextColor(cell.isLocked() ?
                ContextCompat.getColor(text.getContext(), android.R.color.black) :
                ContextCompat.getColor(text.getContext(), R.color.colorPrimaryDark));
        text.setText(textValue);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (reg != null) {
            reg.unregister();
            reg = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
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

    private class UpdateTask implements Runnable {
        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View view = getView();
                    if (view == null)
                        return;

                    synchronized (changes) {
                        for (Cell cell : changes) {
                            Integer id = Integer.valueOf(cell.getID());
                            Integer viewID = cellIDsToBoxIDs.get(id);
                            TextView text = (TextView)view.findViewById(viewID);
                            int value = cell.getValue();
                            String textValue = value == 0 ? "" : ""+value;
                            text.setTextColor(cell.isLocked() ?
                                    ContextCompat.getColor(text.getContext(), android.R.color.black) :
                                    ContextCompat.getColor(text.getContext(), R.color.colorPrimaryDark));
                            text.setText(textValue);
                        }
                        changes.clear();
                    }
                }
            });
        }
    }
}
