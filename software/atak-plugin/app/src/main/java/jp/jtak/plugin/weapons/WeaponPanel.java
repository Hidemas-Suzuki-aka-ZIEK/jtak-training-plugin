package jp.jtak.plugin.weapons;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPoint;
import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.Lang;
import jp.jtak.core.TrainingNotice;
import jp.jtak.core.weapons.DefaultWeaponTable;
import jp.jtak.core.weapons.FriendlyProximity;
import jp.jtak.core.weapons.PointRef;
import jp.jtak.core.weapons.ProximityHit;
import jp.jtak.core.weapons.ProximityResult;
import jp.jtak.core.weapons.WeaponEntry;
import jp.jtak.core.weapons.WeaponEntryForm;
import jp.jtak.core.weapons.WeaponRing;
import jp.jtak.core.weapons.WeaponTable;
import jp.jtak.core.weapons.WeaponTableCodec;
import jp.jtak.plugin.JtakLang;
import jp.jtak.plugin.R;
import jp.jtak.plugin.cas.MapPoint;
import jp.jtak.plugin.cas.MapPointLister;
import jp.jtak.plugin.cas.PanelKit;

/**
 * RED（危険見積距離）の円の画面（04 3 章）。表のどの値も「訓練用サンプル値」で、画面の先頭に必ずその旨を出す（U1）。
 * 円を描いて、範囲の中の味方の点を一覧にする。ATAK の送信やネットワークの API は呼ばない（U8）。
 * <p>
 * Screen for RED (risk estimate distance) circles (04 §3). All table values are “training sample values,” and
 * this is always stated at the top of the screen (U1). Draws circles and lists friendly points within their
 * ranges. Does not call ATAK transmission or network APIs (U8).
 */
public final class WeaponPanel {
    private static final String TAG = "JtakWeapons";
    private static final int RING_SLOTS = WeaponEntryForm.RING_SLOTS;
    private static final int MAX_SHOWN_ERRORS = 5;

    private final MapView mapView;
    private final Context context;
    private final Lang lang;
    private final PanelKit kit;
    private final WeaponTableFile tableFile;
    private final WeaponCircles circles;

    private final TextView tableStatus;
    private final Spinner targetSpinner;
    private final Spinner rowSpinner;
    private final TextView ringSummary;
    private final TextView resultView;
    private final TextView editMessage;
    private final EditText labelField;
    private final EditText[] ringNames = new EditText[RING_SLOTS];
    private final EditText[] ringRadii = new EditText[RING_SLOTS];
    private final EditText noteField;
    private final Button addButton;
    private final Button deleteButton;

    private WeaponTable table;
    /**
     * 編集の欄に出している行の id。
     * <p>
     * ID of the row shown in the editing fields.
     */
    private String editingId;

    public WeaponPanel(MapView mapView, Context context, View content) {
        this.mapView = mapView;
        this.context = context;
        lang = JtakLang.of(context);
        table = DefaultWeaponTable.table(lang);
        kit = new PanelKit(context);
        tableFile = new WeaponTableFile(FileSystemUtils.getItem(WeaponTableFile.RELATIVE_PATH), lang);
        circles = new WeaponCircles(mapView, context);

        LinearLayout root = content.findViewById(R.id.weapons_root);

        // 1. 訓練用サンプル値の表示（外せない。文は core から取る）
        // 1. Training sample values notice (cannot be removed; text comes from core)
        TextView sample = new TextView(context);
        sample.setText(TrainingNotice.weaponSampleText());
        sample.setTextSize(13);
        sample.setTextColor(Color.parseColor("#FFD166"));
        sample.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(sample, matchWrap());

        TextView title = kit.addLabel(root, R.string.weapons_title);
        title.setTextSize(15);

        // 2. 表の状態
        // 2. Table status
        tableStatus = new TextView(context);
        tableStatus.setTextSize(11);
        tableStatus.setTextColor(Color.parseColor("#9AA0A6"));
        root.addView(tableStatus, matchWrap());

        // 3. 目標の点
        // 3. Target point
        kit.addLabel(root, R.string.weapons_target);
        targetSpinner = new Spinner(context);
        root.addView(targetSpinner, matchWrap());
        Button refresh = button(R.string.weapons_refresh);
        refresh.setOnClickListener(v -> refreshPoints());
        root.addView(refresh, matchWrap());

        // 4. 兵器の行
        // 4. Weapon row
        kit.addLabel(root, R.string.weapons_row);
        rowSpinner = new Spinner(context);
        root.addView(rowSpinner, matchWrap());
        ringSummary = new TextView(context);
        ringSummary.setTextSize(13);
        ringSummary.setTextColor(Color.WHITE);
        root.addView(ringSummary, matchWrap());

        // 5. 描く・消す
        // 5. Draw and clear
        LinearLayout drawRow = row();
        Button draw = button(R.string.weapons_draw);
        draw.setOnClickListener(v -> draw());
        Button erase = button(R.string.weapons_erase);
        erase.setOnClickListener(v -> erase());
        addWeighted(drawRow, draw);
        addWeighted(drawRow, erase);
        root.addView(drawRow, matchWrap());

        // 6. 結果
        // 6. Results
        resultView = new TextView(context);
        resultView.setTextSize(13);
        resultView.setTextColor(Color.WHITE);
        resultView.setTextIsSelectable(true);
        resultView.setTypeface(Typeface.MONOSPACE);
        resultView.setPadding(0, kit.dp(8), 0, 0);
        root.addView(resultView, matchWrap());

        // 7. 表の編集
        // 7. Table editing
        TextView editTitle = kit.addLabel(root, R.string.weapons_edit_title);
        editTitle.setTextSize(15);
        labelField = kit.addField(root, R.string.weapons_edit_label);
        labelField.setSingleLine(true);
        int[] nameRes = {R.string.weapons_ring1_name, R.string.weapons_ring2_name, R.string.weapons_ring3_name};
        int[] radiusRes = {R.string.weapons_ring1_radius, R.string.weapons_ring2_radius,
                R.string.weapons_ring3_radius};
        for (int i = 0; i < RING_SLOTS; i++) {
            ringNames[i] = kit.addField(root, nameRes[i]);
            ringNames[i].setSingleLine(true);
            ringRadii[i] = kit.addField(root, radiusRes[i]);
            ringRadii[i].setSingleLine(true);
            ringRadii[i].setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        noteField = kit.addField(root, R.string.weapons_edit_note);

        LinearLayout editRow1 = row();
        Button save = button(R.string.weapons_save_row);
        save.setOnClickListener(v -> saveRow());
        addButton = button(R.string.weapons_add_row);
        addButton.setOnClickListener(v -> addRow());
        deleteButton = button(R.string.weapons_delete_row);
        deleteButton.setOnClickListener(v -> confirmDeleteRow());
        addWeighted(editRow1, save);
        addWeighted(editRow1, addButton);
        addWeighted(editRow1, deleteButton);
        root.addView(editRow1, matchWrap());

        LinearLayout editRow2 = row();
        Button reset = button(R.string.weapons_reset_table);
        reset.setOnClickListener(v -> confirmResetTable());
        Button reload = button(R.string.weapons_reload);
        reload.setOnClickListener(v -> reloadTable(editingId));
        addWeighted(editRow2, reset);
        addWeighted(editRow2, reload);
        root.addView(editRow2, matchWrap());

        editMessage = new TextView(context);
        editMessage.setTextSize(13);
        editMessage.setTextColor(Color.parseColor("#FFD166"));
        root.addView(editMessage, matchWrap());

        rowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onRowSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        reloadTable(null);
        refreshPoints();
    }

    /**
     * 画面を開くたびに、ファイルと点の一覧を読み直す（04 3.2）。
     * <p>
     * Reloads the file and point list whenever the screen opens (04 §3.2).
     */
    public void onShown() {
        reloadTable(editingId);
        refreshPoints();
    }

    /**
     * 描いた円を全部消す。プラグインを閉じるときにも呼ぶ（04 3.4）。
     * <p>
     * Removes all drawn circles. Also called when the plugin closes (04 §3.4).
     */
    public void removeCircles() {
        try {
            circles.clear();
        } catch (RuntimeException e) {
            // 閉じる途中なので画面には出せない。ATAK を落とさないよう、ログだけ残す（04 3.4）
            // The screen is closing, so only log the message to avoid crashing ATAK (04 §3.4).
            Log.e(TAG, "閉じるときに円を消せなかった", e);
        }
    }

    // ---- 表の読み込みと画面への反映 ----
    // ---- Loading the table and updating the screen ----

    private void reloadTable(String keepId) {
        WeaponTableFile.Loaded loaded = tableFile.read();
        table = loaded.table();
        showTableStatus(loaded);
        rebuildRowSpinner(keepId);
        editMessage.setText("");
    }

    private void showTableStatus(WeaponTableFile.Loaded loaded) {
        switch (loaded.source()) {
            case FILE:
                tableStatus.setText(context.getString(R.string.weapons_table_file, table.entries().size()));
                break;
            case INVALID_USING_DEFAULT:
                StringBuilder sb = new StringBuilder(context.getString(R.string.weapons_table_invalid));
                if (loaded.problem() != null) {
                    sb.append('\n').append(describe(loaded.problem()));
                }
                List<String> errors = loaded.errors();
                for (int i = 0; i < errors.size() && i < MAX_SHOWN_ERRORS; i++) {
                    sb.append('\n').append(errors.get(i));
                }
                if (errors.size() > MAX_SHOWN_ERRORS) {
                    sb.append('\n').append(
                            context.getString(R.string.weapons_more_errors, errors.size() - MAX_SHOWN_ERRORS));
                }
                tableStatus.setText(sb.toString());
                break;
            case DEFAULT:
            default:
                tableStatus.setText(R.string.weapons_table_default);
                break;
        }
    }

    private void rebuildRowSpinner(String keepId) {
        List<String> labels = new ArrayList<>();
        int select = 0;
        List<WeaponEntry> entries = table.entries();
        for (int i = 0; i < entries.size(); i++) {
            labels.add(entries.get(i).label());
            if (entries.get(i).id().equals(keepId)) {
                select = i;
            }
        }
        rowSpinner.setAdapter(whiteAdapter(labels));
        if (!entries.isEmpty()) {
            rowSpinner.setSelection(select);
        }
        onRowSelected();
    }

    private void onRowSelected() {
        WeaponEntry entry = selectedEntry();
        if (entry == null) {
            ringSummary.setText(R.string.weapons_no_row);
            editingId = null;
        } else {
            ringSummary.setText(ringSummaryOf(entry));
            fillEditFields(entry);
        }
        addButton.setEnabled(table.entries().size() < WeaponTableCodec.MAX_ENTRIES);
        deleteButton.setEnabled(table.entries().size() > 1);
    }

    private WeaponEntry selectedEntry() {
        int i = rowSpinner.getSelectedItemPosition();
        List<WeaponEntry> entries = table.entries();
        return i < 0 || i >= entries.size() ? null : entries.get(i);
    }

    /**
     * 「内側 100 m / 外側 300 m」の形。
     * <p>
     * In the form “inner 100 m / outer 300 m.”
     */
    private String ringSummaryOf(WeaponEntry entry) {
        StringBuilder sb = new StringBuilder();
        for (WeaponRing r : entry.rings()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(context.getString(R.string.weapons_unit_m, r.name(), r.radiusM()));
        }
        return sb.toString();
    }

    private void fillEditFields(WeaponEntry entry) {
        editingId = entry.id();
        labelField.setText(entry.label());
        List<WeaponRing> rings = entry.rings();
        for (int i = 0; i < RING_SLOTS; i++) {
            if (i < rings.size()) {
                ringNames[i].setText(rings.get(i).name());
                ringRadii[i].setText(String.valueOf(rings.get(i).radiusM()));
            } else {
                ringNames[i].setText("");
                ringRadii[i].setText("");
            }
        }
        noteField.setText(entry.note());
    }

    // ---- 目標の点 ----
    // ---- Target point ----

    /**
     * 点の一覧を読み直してスピナーに入れる。ATAK の API が例外を投げたら、ATAK を落とさず結果の欄に理由を出す（04 3.4）。
     * <p>
     * Reloads the point list into the spinner. If an ATAK API throws, displays the reason in the results area
     * without crashing ATAK (04 §3.4).
     */
    private void refreshPoints() {
        try {
            List<MapPoint> points = MapPointLister.list(mapView);
            Object before = targetSpinner.getSelectedItem();
            String keep = before instanceof MapPoint ? ((MapPoint) before).uid() : null;
            List<Object> items = new ArrayList<>();
            items.add(context.getString(R.string.choice_none));
            items.addAll(points);
            targetSpinner.setAdapter(whiteAdapter(items));
            if (keep != null) {
                for (int i = 0; i < points.size(); i++) {
                    if (keep.equals(points.get(i).uid())) {
                        targetSpinner.setSelection(i + 1);
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "点の一覧を取れなかった", e);
            resultView.setText(headWith(context.getString(R.string.weapons_refresh_failed, reasonOf(e))));
        }
    }

    private MapPoint selectedTarget() {
        Object o = targetSpinner.getSelectedItem();
        return o instanceof MapPoint ? (MapPoint) o : null;
    }

    // ---- 描く・消す ----
    // ---- Draw and clear ----

    private void draw() {
        try {
            drawImpl();
        } catch (RuntimeException e) {
            // 判定は円を描く前に済ませてある。円を描く途中の失敗は、WeaponCircles が
            // この回に足した分だけ取り除いてある。取り除けなかった円は覚えておき、
            // 「消す」で取り除く（04 3.4）
            // Assessment is already complete before drawing. If drawing fails partway through, WeaponCircles has
            // removed only the circles added in this call. It keeps track of circles it could not remove, so
            // “Clear” can remove them (04 §3.4).
            Log.e(TAG, "円を描けなかった", e);
            resultView.setText(headWith(context.getString(R.string.weapons_draw_failed, reasonOf(e))));
        }
    }

    private void drawImpl() {
        MapPoint selected = selectedTarget();
        WeaponEntry entry = selectedEntry();
        if (selected == null) {
            resultView.setText(headWith(context.getString(R.string.weapons_no_target)));
            return;
        }
        if (entry == null) {
            resultView.setText(headWith(context.getString(R.string.weapons_no_row)));
            return;
        }
        // 描く時点の点の一覧を取り直す。開いたあとに動いた点を、古い位置で判定しない（04 3.4・2.8）
        // Retrieve the point list again at drawing time. Do not assess points that moved after opening the screen at their old positions (04 §§3.4, 2.8).
        List<MapPoint> points = MapPointLister.list(mapView);
        MapPoint target = null;
        for (MapPoint p : points) {
            if (selected.uid().equals(p.uid())) {
                target = p;
                break;
            }
        }
        if (target == null) {
            resultView.setText(headWith(context.getString(R.string.weapons_no_target)));
            return;
        }
        // 味方の判定と結果の文は、円を描く前に作る。ここで例外が出ても円は 1 つも描いていない（04 3.4）
        // Assess friendly points and prepare result text before drawing circles. If an exception occurs here, no circles have been drawn (04 §3.4).
        String selfUid = MapView.getDeviceUid();
        PointRef targetRef = toRef(target, selfUid);
        List<PointRef> refs = new ArrayList<>();
        for (MapPoint p : points) {
            refs.add(toRef(p, selfUid));
        }
        ProximityResult result = FriendlyProximity.check(entry, targetRef, refs);

        StringBuilder sb = new StringBuilder(TrainingNotice.text());
        List<ProximityHit> hits = result.hits();
        if (hits.isEmpty()) {
            sb.append('\n').append(context.getString(R.string.weapons_friendly_none));
        } else {
            sb.append('\n').append(context.getString(R.string.weapons_friendly_title));
            for (ProximityHit h : hits) {
                sb.append('\n').append(context.getString(R.string.weapons_friendly_hit, h.name(), h.ringName(),
                        (int) Math.round(h.distanceM())));
            }
        }
        if (result.unknownCount() >= 1) {
            sb.append('\n').append(context.getString(R.string.weapons_unknown, result.unknownCount()));
        }

        // 円は最後に描く。描く途中の失敗は WeaponCircles が戻して投げ直し、draw() の catch が理由を出す
        // Draw circles last. WeaponCircles rolls back drawing failures and rethrows; draw()'s catch displays the reason.
        GeoPoint center = target.point();
        circles.draw(entry, center, target.name());
        resultView.setText(sb.toString());
    }

    private void erase() {
        try {
            int removed = circles.clear();
            resultView.setText(headWith(context.getString(R.string.weapons_erased, removed)));
        } catch (RuntimeException e) {
            Log.e(TAG, "円を消せなかった", e);
            resultView.setText(headWith(context.getString(R.string.weapons_erase_failed, reasonOf(e))));
        }
    }

    /**
     * 例外のメッセージ。メッセージがない例外は、クラス名で代える。
     * <p>
     * Exception message. Uses the class name for exceptions without a message.
     */
    private static String reasonOf(Throwable e) {
        String m = e.getMessage();
        return m != null && !m.isEmpty() ? m : e.getClass().getSimpleName();
    }

    private static PointRef toRef(MapPoint p, String selfUid) {
        String type = p.type() == null ? "" : p.type();
        return new PointRef(p.uid(), p.name(), type, p.point().getLatitude(), p.point().getLongitude(),
                selfUid != null && selfUid.equals(p.uid()));
    }

    private static String headWith(String line) {
        return TrainingNotice.text() + "\n" + line;
    }

    // ---- 表の編集 ----
    // ---- Table editing ----

    private void saveRow() {
        WeaponEntry current = selectedEntry();
        if (current == null) {
            resultView.setText(headWith(context.getString(R.string.weapons_no_row)));
            return;
        }
        String[] names = new String[RING_SLOTS];
        String[] radii = new String[RING_SLOTS];
        for (int i = 0; i < RING_SLOTS; i++) {
            names[i] = PanelKit.text(ringNames[i]);
            radii[i] = PanelKit.text(ringRadii[i]);
        }
        WeaponEntryForm.FormResult form = WeaponEntryForm.toEntry(current.id(), PanelKit.text(labelField), names,
                radii, PanelKit.text(noteField), lang);
        if (!form.ok()) {
            showErrors(form.errors());
            return;
        }
        List<WeaponEntry> list = new ArrayList<>(table.entries());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(current.id())) {
                list.set(i, form.entry());
            }
        }
        writeTable(new WeaponTable(list), current.id());
    }

    private void addRow() {
        List<WeaponEntry> list = new ArrayList<>(table.entries());
        String id = WeaponEntryForm.nextId(table);
        List<WeaponRing> rings = new ArrayList<>();
        rings.add(new WeaponRing(context.getString(R.string.weapons_new_ring_name), 100));
        list.add(new WeaponEntry(id, context.getString(R.string.weapons_new_row_label), rings, ""));
        writeTable(new WeaponTable(list), id);
    }

    private void confirmDeleteRow() {
        final WeaponEntry current = selectedEntry();
        if (current == null || table.entries().size() <= 1) {
            return;
        }
        confirm(context.getString(R.string.weapons_delete_confirm, current.label()), () -> {
            List<WeaponEntry> list = new ArrayList<>(table.entries());
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).id().equals(current.id())) {
                    list.remove(i);
                    break;
                }
            }
            writeTable(new WeaponTable(list), null);
        });
    }

    private void confirmResetTable() {
        confirm(context.getString(R.string.weapons_reset_confirm),
                () -> writeTable(DefaultWeaponTable.table(lang), null));
    }

    private void confirm(String message, final Runnable onOk) {
        new AlertDialog.Builder(mapView.getContext())
                .setMessage(message)
                .setPositiveButton(R.string.weapons_ok, (d, w) -> onOk.run())
                .setNegativeButton(R.string.weapons_cancel, null)
                .show();
    }

    /**
     * 表全体を確かめてから書く。確かめで引っかかったとき・書けなかったときは、古いファイルのまま。
     * <p>
     * Validates the entire table before writing. Keeps the old file if validation or writing fails.
     */
    private void writeTable(WeaponTable next, String selectId) {
        List<String> errors = WeaponTableCodec.validate(next, lang);
        if (!errors.isEmpty()) {
            showErrors(errors);
            return;
        }
        try {
            tableFile.write(next);
        } catch (WeaponTableFileException e) {
            StringBuilder sb = new StringBuilder(context.getString(R.string.weapons_save_failed, describe(e)));
            if (e.keptFileName() != null) {
                sb.append('\n').append(context.getString(R.string.weapons_kept_as, e.keptFileName()));
            }
            editMessage.setText(sb.toString());
            showTableStatus(tableFile.read());
            return;
        } catch (RuntimeException e) {
            Log.e(TAG, "表を書けなかった", e);
            editMessage.setText(context.getString(R.string.weapons_save_failed, reasonOf(e)));
            showTableStatus(tableFile.read());
            return;
        }
        reloadTable(selectId);
        editMessage.setText(R.string.weapons_saved);
    }

    /**
     * 表のファイルの失敗の原因を、strings.xml の文言にする（04 3.6）。
     * <p>
     * Converts the cause of a table file failure to strings.xml text (04 §3.6).
     */
    private String describe(WeaponTableFileException e) {
        String detail = e.detail();
        switch (e.reason()) {
            case FILE_TOO_LARGE:
                return context.getString(R.string.weapons_reason_too_large, detail);
            case MKDIR_FAILED:
                return context.getString(R.string.weapons_reason_mkdir_failed, detail);
            case TMP_WRITE_FAILED:
                return context.getString(R.string.weapons_reason_tmp_write_failed, detail);
            case RENAME_INVALID_FAILED:
                return context.getString(R.string.weapons_reason_rename_invalid_failed, detail);
            case RENAME_FAILED:
                return context.getString(R.string.weapons_reason_rename_failed, detail);
            case READ_FAILED:
            default:
                Throwable cause = e.getCause();
                return context.getString(R.string.weapons_reason_read_failed,
                        cause != null ? reasonOf(cause) : "");
        }
    }

    private void showErrors(List<String> errors) {
        StringBuilder sb = new StringBuilder(context.getString(R.string.weapons_errors_title));
        for (int i = 0; i < errors.size() && i < MAX_SHOWN_ERRORS; i++) {
            sb.append('\n').append(errors.get(i));
        }
        if (errors.size() > MAX_SHOWN_ERRORS) {
            sb.append('\n').append(context.getString(R.string.weapons_more_errors, errors.size() - MAX_SHOWN_ERRORS));
        }
        editMessage.setText(sb.toString());
    }

    // ---- 部品 ----
    // ---- Components ----

    private Button button(int textRes) {
        Button b = new Button(context);
        b.setText(textRes);
        b.setTextSize(12);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(b.getPaddingLeft(), kit.dp(12), b.getPaddingRight(), kit.dp(12));
        return b;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private static void addWeighted(LinearLayout row, View v) {
        row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private <T> ArrayAdapter<T> whiteAdapter(List<T> items) {
        ArrayAdapter<T> adapter = new ArrayAdapter<T>(context, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                ((TextView) v).setTextSize(13);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setTextSize(13);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }
}
