package com.sudoku.dj.sudokusolver.tasks;

import android.content.res.AssetManager;

import com.sudoku.dj.sudokusolver.solver.Cell;
import com.sudoku.dj.sudokusolver.solver.CellModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipInputStream;

public class MaskBoardGeneratorTask implements BackgroundTaskManager.BackgroundTaskWork<CellModel>  {

    private static final int MAX_FILES = 10;
    private static final int MAX_MASKS = 100000;

    private final int filledCells;
    private Random random;
    private AssetManager assets;

    public MaskBoardGeneratorTask(int filledCells, AssetManager assets) {
        this.filledCells = filledCells;
        this.assets = assets;
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public CellModel doWork(CellModel model) {
        model.resetAllCells();
        String mask = getMask();
        Map<Character, Integer> map = buildMap();
        buildUnsolvedBoard(model, mask, map);
        return model;
    }

    private String getMask() {
        int fileID = random.nextInt(MAX_FILES);
        int lines = random.nextInt(MAX_MASKS);
        try (ZipInputStream zip = new ZipInputStream(assets.open("masks."+fileID+".zip"));
             BufferedReader reader = new BufferedReader(new InputStreamReader(zip))) {
            zip.getNextEntry(); // allows the file to be read
            String mask = null;
            for (int i=0; i<=lines; i++) {
                mask = reader.readLine();
            }
            return mask;
        } catch (Exception e) {
            throw new RuntimeException("Unknown error reading file", e);
        }
    }

    private void buildUnsolvedBoard(CellModel model, String mask, Map<Character, Integer> map) {
        int cellCount = 0;
        final int maxAvailablePerGroup = 4;
        while (cellCount < filledCells) {
            int index = random.nextInt(CellModel.MAX_CELLS);
            Cell target = model.getCell(index);
            if (target.getValue() > 0 ||
                    target.getHorizontalGroup().getAvailableValues().size() < maxAvailablePerGroup ||
                    target.getVerticalGroup().getAvailableValues().size() < maxAvailablePerGroup ||
                    target.getCubeGroup().getAvailableValues().size() < maxAvailablePerGroup) {
                continue;
            }
            char c = mask.charAt(index);
            Integer value = map.get(c);
            model.setValue(target, value);
            cellCount++;
        }
    }

    private Map<Character, Integer> buildMap() {
        // build a set containing all available values
        List<Integer> available = new ArrayList<>();
        for (int i=1; i<=CellModel.MAX_CELLS_IN_GROUP; i++) {
            available.add(Integer.valueOf(i));
        }

        Map<Character, Integer> map = new HashMap<>();
        for (char ch = 'A'; ch < 'J'; ch++) {
            Integer value = available.remove(random.nextInt(available.size()));
            map.put(ch, value);
        }
        return map;
    }

    @Override
    public void onUpdate(Integer... progress) {
        // no-op
    }

    @Override
    public void onFinish(CellModel model) {
        model.lockFilledCells();
    }
}
