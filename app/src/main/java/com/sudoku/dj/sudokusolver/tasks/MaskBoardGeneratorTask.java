package com.sudoku.dj.sudokusolver.tasks;

import com.sudoku.dj.sudokusolver.solver.Cell;
import com.sudoku.dj.sudokusolver.solver.CellModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MaskBoardGeneratorTask implements BackgroundTaskManager.BackgroundTaskWork<CellModel>  {

    private final int filledCells;
    private Random random;

    public MaskBoardGeneratorTask(int filledCells) {
        this.filledCells = filledCells;
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public CellModel doWork(CellModel model) {
        model.resetAllCells();
        String mask = masks.get(random.nextInt(masks.size()));
        Map<Character, Integer> map = buildMap();
        buildUnsolvedBoard(model, mask, map);
        return model;
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

    // HACK: these belong somewhere else, either in a file or database
    private static final List<String> masks;
    static {
        List<String> m = new ArrayList<>();
        m.add("ABCDEFGHIIDGABHCEFHFEICGDBAFIAGHEBCDGEDBICAFHBCHFADIGEEABCFIHDGDHIEGBFACCGFHDAEIB");
        m.add("ABCDEFGHIFDEIGHBCAGIHBACFEDICDHFBAGEEFACDGIBHHGBAIECDFBEGFHIDACDHIGCAEFBCAFEBDHIG");
        m.add("ABCDEFGHIFDEHGIBACHGIBCADEFBEAIFDCGHGIFCHBEDADCHEAGFIBIABGDCHFEEFDABHICGCHGFIEABD");
        m.add("ABCDEFGHIFGDIHAECBHIEGCBDFAGCHBIEFADEFAHGDIBCBDIAFCHEGCHBEDIAGFDAGFBHCIEIEFCAGBDH");
        m.add("ABCDEFGHIGIECHAFBDFDHGBICEACHGEDBIAFDEAFIHBCGBFIACGHDEICFBAEDGHEGBHFDAICHADIGCEFB");
        m.add("ABCDEFGHIIGDHBCFAEEFHAIGCBDHABGDEICFCDIFABEGHGEFCHIADBDCAIFHBEGFHEBGADICBIGECDHFA");
        m.add("ABCDEFGHIHFDGBICEAIEGCAHDFBGCFEIAHBDDHEBFGAICBAIHCDEGFEGBIDCFAHCIAFHEBDGFDHAGBICE");
        m.add("ABCDEFGHIDGFHIBAECHEIACGDBFCIBFGDEAHGAHEBCFIDFDEIAHCGBIHGCFEBDAEFABDIHCGBCDGHAIFE");
        m.add("ABCDEFGHIHDGIACBFEIEFHBGACDDHBEFACIGCIEBGHFDAFGACDIEBHBCIAHEDGFGADFIBHECEFHGCDIAB");
        m.add("ABCDEFGHIEHGCAIBDFFIDGHBCAEHCIFGADEBBFAEDHICGGDEIBCHFADAFHIGEBCCGHBFEAIDIEBACDFGH");
        m.add("ABCDEFGHIEFDIHGACBIGHABCFEDCHAFGIBDEDIGBCEHAFBEFHDACIGFABEIHDGCGDICABEFHHCEGFDIBA");
        m.add("ABCDEFGHIIFHAGCDEBEGDIHBACFGABEFICDHCIFHADBGEDHEBCGFIAFDAGIHEBCBEICDAHFGHCGFBEIAD");
        m.add("ABCDEFGHIDEGHICAFBHIFBGACDEFCDEAGIBHEHICFBDGABGAIHDECFCAHFDEBIGIDEGBHFACGFBACIHED");
        m.add("ABCDEFGHIFDGIBHAECIEHCAGFDBBHAFGCDIEGIEBDACFHDCFHIEBAGCADGHIEBFHGBEFDICAEFIACBHGD");
        m.add("ABCDEFGHIDHEGAIFBCFIGBCHEDAHGIEDBACFCFAHIGBEDBEDAFCHIGGDHCBAIFEICBFGEDAHEAFIHDCGB");
        m.add("ABCDEFGHIHGFIACBDEIEDBHGFCAEAHCGDIBFBCGFIHAEDDFIABECGHCDEGFIHABGIAHDBEFCFHBECADIG");
        m.add("ABCDEFGHIEGHCIBFADDFIAGHCBEHCDEFGBIABEFIHADGCIAGBDCHEFFDAHBIECGCHEGADIFBGIBFCEADH");
        m.add("ABCDEFGHIDFIHCGBEAHEGBIACFDICDEHBFAGEABFGIHDCFGHCADIBEGIFADHECBCHAGBEDIFBDEIFCAGH");
        m.add("ABCDEFGHIEIGHCADFBHDFIBGCEAIABGFDHCECEDAHBFIGGFHCIEABDDCIEAHBGFFHABGIEDCBGEFDCIAH");
        m.add("ABCDEFGHIHEFIBGADCGDICAHBFEFHABCEIGDCGDAFIEBHBIEHGDCAFECGFDBHIAIFBEHADCGDAHGICFEB");
        masks = Collections.unmodifiableList(m);
    }
}
