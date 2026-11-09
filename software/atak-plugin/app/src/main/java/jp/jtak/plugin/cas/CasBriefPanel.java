package jp.jtak.plugin.cas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jp.jtak.core.cas.AttackPointType;
import jp.jtak.core.cas.CasBriefFormatter;
import jp.jtak.core.cas.CasBriefResult;
import jp.jtak.core.cas.CasRemarks;
import jp.jtak.core.cas.CasRestrictions;
import jp.jtak.core.cas.NineLine;
import jp.jtak.core.cas.NineLineCalculator;
import jp.jtak.core.cas.TargetDescriptionDraft;
import jp.jtak.core.cas.TotType;
import jp.jtak.core.export.ExportKind;
import jp.jtak.core.export.ExportText;
import jp.jtak.core.importer.CasBriefImport;
import jp.jtak.core.importer.ScenarioImportResult;
import jp.jtak.core.importer.ScenarioKind;
import jp.jtak.core.lrf.GeoPosition;
import jp.jtak.plugin.R;
import jp.jtak.plugin.export.ExportShare;
import jp.jtak.plugin.export.ExportException;
import jp.jtak.plugin.export.ExportWriter;
import jp.jtak.plugin.export.PdfExporter;

/**
 * 9-line と Remarks の画面（02 3.1〜3.3）。画面とファイルに出し、ATAK の送信（CoT・チャット）や
 * ネットワークの API は呼ばない（U8）。訓練用の表示は親の画面（見出しと末尾）と、作った本文の
 * 先頭と末尾に必ず入る（U1）。
 * <p>
 * Screen for the 9-line and Remarks (02 §§3.1–3.3). Outputs to the screen and files, without calling ATAK
 * transmission (CoT or chat) or network APIs (U8). The training notice is always included in the parent screen
 * (header and footer) and at the beginning and end of the generated text (U1).
 */
public final class CasBriefPanel {
    private static final int FIELD_COUNT = 9;
    private static final TotType[] TOT_TYPES = TotType.values();

    private final MapView mapView;
    private final Context context;
    private final Spinner ipSpinner;
    private final Spinner targetSpinner;
    private final Spinner friendlySpinner;
    private final RadioButton typeBp;
    private final TextView fillStatus;
    private final TextView missingView;
    private final TextView resultView;
    private final View exportTextButton;
    private final View exportPdfButton;
    private String lastResultText;
    private final EditText[] lineFields = new EditText[FIELD_COUNT];
    private final EditText offsetField;
    private final EditText ltlPtl;
    private final EditText ordnance;
    private final EditText threat;
    private final EditText additionalRemarks;
    private final EditText additionalCalls;
    private final CheckBox dangerClose;
    private final EditText dangerCloseInitials;
    private final EditText acas;
    private final EditText fahs;
    private final EditText separation;
    private final EditText rwOutOfBp;
    private final EditText pla;
    private final Spinner totType;
    private final EditText totTime;
    private final PanelKit kit;
    private final ImportFlow importFlow;
    private List<MapPoint> points = Collections.emptyList();

    public CasBriefPanel(MapView mapView, Context context, View content) {
        this.mapView = mapView;
        this.context = context;
        kit = new PanelKit(context);
        ipSpinner = content.findViewById(R.id.cas_spinner_ip);
        targetSpinner = content.findViewById(R.id.cas_spinner_target);
        friendlySpinner = content.findViewById(R.id.cas_spinner_friendly);
        typeBp = content.findViewById(R.id.cas_type_bp);
        fillStatus = content.findViewById(R.id.cas_fill_status);
        missingView = content.findViewById(R.id.cas_missing);
        resultView = content.findViewById(R.id.cas_result);
        exportTextButton = content.findViewById(R.id.cas_export_text);
        exportPdfButton = content.findViewById(R.id.cas_export_pdf);
        setExportResult(null);
        exportTextButton.setOnClickListener(v -> export(false));
        exportPdfButton.setOnClickListener(v -> export(true));

        LinearLayout fields = content.findViewById(R.id.cas_fields);
        int[] lineLabels = {R.string.cas_line1, R.string.cas_line2, R.string.cas_line3,
                R.string.cas_line4, R.string.cas_line5, R.string.cas_line6, R.string.cas_line7,
                R.string.cas_line8, R.string.cas_line9};
        EditText offset = null;
        for (int i = 0; i < FIELD_COUNT; i++) {
            lineFields[i] = addField(fields, lineLabels[i]);
            if (i == 1) {
                // Offset は 2 行目（Heading）の直後に置く。
                // Place Offset immediately after line 2 (Heading).
                offset = addField(fields, R.string.cas_offset);
            }
        }
        offsetField = offset;
        addLabel(fields, R.string.cas_remarks_title).setPadding(0, dp(12), 0, 0);
        ltlPtl = addField(fields, R.string.cas_ltl_ptl);
        ordnance = addField(fields, R.string.cas_ordnance);
        threat = addField(fields, R.string.cas_threat);
        additionalRemarks = addField(fields, R.string.cas_additional_remarks);
        additionalCalls = addField(fields, R.string.cas_additional_calls);
        addLabel(fields, R.string.restrictions_title).setPadding(0, dp(12), 0, 0);
        dangerClose = kit.addCheck(fields, R.string.restrictions_danger_close);
        dangerCloseInitials = addField(fields, R.string.restrictions_initials);
        acas = addField(fields, R.string.restrictions_acas);
        fahs = addField(fields, R.string.restrictions_fahs);
        separation = addField(fields, R.string.restrictions_separation);
        rwOutOfBp = addField(fields, R.string.restrictions_rw_out_of_bp);
        pla = addField(fields, R.string.restrictions_pla);
        totType = kit.addChoice(fields, R.string.restrictions_tot, R.string.choice_none,
                R.string.restrictions_tot_time_option, R.string.restrictions_tot_standby,
                R.string.restrictions_tot_after_correlation, R.string.restrictions_tot_push);
        totTime = addField(fields, R.string.restrictions_tot_time);

        importFlow = new ImportFlow(mapView, context, ScenarioKind.CAS_BRIEF, new ImportScreen());
        kit.addImportButton(fields, importFlow::start);

        content.findViewById(R.id.cas_refresh).setOnClickListener(v -> refreshPoints());
        content.findViewById(R.id.cas_fill).setOnClickListener(v -> fillFromMap());
        content.findViewById(R.id.cas_make).setOnClickListener(v -> make());
        refreshPoints();
    }

    /**
     * 画面を閉じるとき。読み込みの途中のスレッドを止める。
     * <p>
     * When closing the screen, stops the thread performing an import.
     */
    public void close() {
        importFlow.close();
    }

    /**
     * ファイルから読み込む（設計 06 の 3 章）。地図の点の選ぶ欄（IP・目標・味方）は戻さず、変えない。
     * <p>
     * Imports from a file (design 06 §3). Does not reset or change the map point selectors (IP, target, and
     * friendly).
     */
    private final class ImportScreen implements ImportFlow.Screen {
        @Override
        public boolean hasInput() {
            return PanelKit.anyText(lineFields) || PanelKit.anyText(offsetField, ltlPtl, ordnance,
                    threat, additionalRemarks, additionalCalls, dangerCloseInitials, acas, fahs,
                    separation, rwOutOfBp, pla, totTime)
                    || dangerClose.isChecked()
                    || totType.getSelectedItemPosition() != 0;
        }

        @Override
        public void resetInput() {
            PanelKit.clear(lineFields);
            PanelKit.clear(offsetField, ltlPtl, ordnance, threat, additionalRemarks,
                    additionalCalls, dangerCloseInitials, acas, fahs, separation, rwOutOfBp, pla,
                    totTime);
            dangerClose.setChecked(false);
            totType.setSelection(0);
        }

        @Override
        public void fill(ScenarioImportResult result) {
            CasBriefImport c = result.casBrief();
            if (c == null) {
                return;
            }
            String[] lines = {c.line1(), c.line2(), c.line3(), c.line4(), c.line5(), c.line6(),
                    c.line7(), c.line8(), c.line9()};
            for (int i = 0; i < FIELD_COUNT; i++) {
                PanelKit.set(lineFields[i], lines[i]);
            }
            PanelKit.set(offsetField, c.offset());
            PanelKit.set(ltlPtl, c.ltlPtl());
            PanelKit.set(ordnance, c.remarksOrdnance());
            PanelKit.set(threat, c.threat());
            PanelKit.set(additionalRemarks, c.additionalRemarks());
            PanelKit.set(additionalCalls, c.additionalCalls());
            PanelKit.set(dangerClose, c.dangerClose());
            PanelKit.set(dangerCloseInitials, c.dangerCloseInitials());
            PanelKit.set(acas, c.acas());
            PanelKit.set(fahs, c.fahs());
            PanelKit.set(separation, c.separation());
            PanelKit.set(rwOutOfBp, c.rwOutOfBp());
            PanelKit.set(pla, c.pla());
            PanelKit.select(totType, TOT_TYPES, c.tot());
            PanelKit.set(totTime, c.totTime());
        }

        @Override
        public void regenerate() {
            make();
        }

        @Override
        public TextView missingView() {
            return missingView;
        }
    }

    /**
     * 画面を開くたびに一覧を読み直す。
     * <p>
     * Reloads the list whenever the screen opens.
     */
    public void onShown() {
        refreshPoints();
    }

    private int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    private TextView addLabel(LinearLayout parent, int textRes) {
        TextView t = new TextView(context);
        t.setTextSize(13);
        t.setText(textRes);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, dp(4), 0, 0);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private EditText addField(LinearLayout parent, int labelRes) {
        addLabel(parent, labelRes);
        EditText e = new EditText(context);
        e.setTextSize(13);
        e.setTextColor(Color.WHITE);
        e.setSingleLine(false);
        parent.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private void refreshPoints() {
        points = MapPointLister.list(mapView);
        fillSpinner(ipSpinner);
        fillSpinner(targetSpinner);
        fillSpinner(friendlySpinner);
        fillStatus.setText(context.getString(R.string.cas_points_count, points.size()));
    }

    private void fillSpinner(Spinner spinner) {
        // 選んでいた点は、同じ UID の点があれば選び直す（ラベルは重なることがある）。
        // Restore the selected point if a point with the same UID exists (labels may be duplicated).
        Object before = spinner.getSelectedItem();
        String keep = before instanceof MapPoint ? ((MapPoint) before).uid() : null;
        List<Object> items = new ArrayList<>();
        items.add(context.getString(R.string.cas_none));
        items.addAll(points);
        ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(context,
                android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (keep != null) {
            for (int i = 0; i < points.size(); i++) {
                if (keep.equals(points.get(i).uid())) {
                    spinner.setSelection(i + 1);
                    break;
                }
            }
        }
    }

    private MapPoint selected(Spinner spinner) {
        Object o = spinner.getSelectedItem();
        return o instanceof MapPoint ? (MapPoint) o : null;
    }

    /**
     * 「地図の点から入れる」: 1・2・3・4・5・6・8 行目を入れる。取れない値は空にして、行を知らせる。
     * <p>
     * “Fill from map points”: fills lines 1, 2, 3, 4, 5, 6, and 8. Leaves unavailable values empty and identifies
     * their lines.
     */
    private void fillFromMap() {
        MapPoint ip = selected(ipSpinner);
        MapPoint target = selected(targetSpinner);
        MapPoint friendly = selected(friendlySpinner);
        AttackPointType type = typeBp.isChecked() ? AttackPointType.BP : AttackPointType.IP;

        String l1 = ip == null ? null : ip.name();
        String l2 = null;
        String l3 = null;
        if (ip != null && target != null) {
            GeoPosition from = CasValueReader.position(ip.point());
            GeoPosition to = CasValueReader.position(target.point());
            l2 = NineLineCalculator.heading(from, to, CasValueReader.declination(ip.point()));
            l3 = NineLineCalculator.distance(from, to, type);
        }
        String l4 = null;
        String l5 = null;
        String l6 = null;
        String l8 = null;
        if (target != null) {
            GeoPoint t = target.point();
            l4 = CasValueReader.elevationText(t);
            // 5 行目は敵の点の種類から下書きを入れる。味方・中立・不明や表にない種類は null で、欄を空にする（02 2.3.1・3.1）。
            // Draft line 5 from the hostile point type. Friendly, neutral, unknown, and unlisted types yield null, leaving the field empty (02 §§2.3.1, 3.1).
            l5 = TargetDescriptionDraft.fromCotType(target.type());
            l6 = CasValueReader.mgrs(t);
            if (friendly != null) {
                List<GeoPosition> list = Collections.singletonList(CasValueReader.position(friendly.point()));
                l8 = NineLineCalculator.friendlies(CasValueReader.position(t), list);
            }
        }
        String[] values = {l1, l2, l3, l4, l5, l6, null, l8, null};
        int[] filled = {0, 1, 2, 3, 4, 5, 7};
        List<String> notFilled = new ArrayList<>();
        for (int i : filled) {
            lineFields[i].setText(values[i] == null ? "" : values[i]);
            if (values[i] == null) {
                notFilled.add(String.valueOf(i + 1));
            }
        }
        fillStatus.setText(notFilled.isEmpty()
                ? context.getString(R.string.cas_fill_done)
                : context.getString(R.string.cas_fill_missing, join(notFilled)));
    }

    private void make() {
        NineLine.Builder b = new NineLine.Builder();
        b.line1(text(lineFields[0])).line2(text(lineFields[1])).line3(text(lineFields[2]))
                .line4(text(lineFields[3])).line5(text(lineFields[4])).line6(text(lineFields[5]))
                .line7(text(lineFields[6])).line8(text(lineFields[7])).line9(text(lineFields[8]))
                .offset(text(offsetField));
        CasRemarks remarks = new CasRemarks.Builder()
                .ltlPtl(text(ltlPtl)).ordnance(text(ordnance)).threat(text(threat))
                .additionalRemarks(text(additionalRemarks)).additionalCalls(text(additionalCalls))
                .build();
        CasRestrictions restrictions;
        try {
            restrictions = new CasRestrictions.Builder()
                    .dangerClose(dangerClose.isChecked())
                    .dangerCloseInitials(text(dangerCloseInitials))
                    .acas(text(acas)).fahs(text(fahs)).separation(text(separation))
                    .rwOutOfBp(text(rwOutOfBp)).pla(text(pla))
                    .tot(PanelKit.picked(totType, TOT_TYPES)).totTime(text(totTime))
                    .build();
        } catch (IllegalArgumentException e) {
            missingView.setText(context.getString(R.string.restrictions_conflict));
            resultView.setText("");
            setExportResult(null);
            return;
        }
        CasBriefResult result = CasBriefFormatter.format(b.build(), remarks, restrictions);
        List<String> messages = new ArrayList<>();
        if (!result.missingLines().isEmpty()) {
            List<String> nums = new ArrayList<>();
            for (Integer n : result.missingLines()) {
                nums.add(String.valueOf(n));
            }
            messages.add(context.getString(R.string.cas_missing_lines, join(nums)));
        }
        if (!result.missingRestrictions().isEmpty()) {
            messages.add(context.getString(R.string.restrictions_missing,
                    join(result.missingRestrictions())));
        }
        missingView.setText(String.join("\n", messages));
        resultView.setTypeface(Typeface.MONOSPACE);
        resultView.setText(result.text());
        setExportResult(result.text());
    }

    private static String text(EditText e) {
        return e.getText().toString();
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p);
        }
        return sb.toString();
    }

    private void setExportResult(String text) {
        lastResultText = text;
        exportTextButton.setEnabled(text != null);
        exportPdfButton.setEnabled(text != null);
    }

    private void export(boolean pdf) {
        if (lastResultText == null) {
            return;
        }
        try {
            LocalDateTime time = LocalDateTime.now();
            File file = pdf
                    ? new PdfExporter().writePdf(ExportKind.CAS_BRIEF, lastResultText, time)
                    : new ExportWriter().writeText(ExportKind.CAS_BRIEF, lastResultText, time);
            Toast.makeText(mapView.getContext(), context.getString(R.string.export_saved,
                    file.getAbsolutePath()), Toast.LENGTH_SHORT).show();
            new ExportShare(context).share(mapView, file,
                    pdf ? "application/pdf" : "text/plain", ExportText.fileContent(lastResultText));
        } catch (IOException | RuntimeException e) {
            Log.e("JtakExport", "書き出せなかった", e);
            Toast.makeText(mapView.getContext(), context.getString(R.string.export_failed,
                    exportReason(e)), Toast.LENGTH_LONG).show();
        }
    }

    private String exportReason(Exception error) {
        if (!(error instanceof ExportException)) {
            return error.getMessage();
        }
        ExportException failure = (ExportException) error;
        int key;
        switch (failure.reason()) {
            case TEXT_WRITE:
                key = R.string.export_reason_text_write;
                break;
            case DIR_CREATE:
                key = R.string.export_reason_dir_create;
                break;
            case DIR_OPEN:
                key = R.string.export_reason_dir_open;
                break;
            case PDF_WRITE:
                key = R.string.export_reason_pdf_write;
                break;
            default:
                throw new IllegalArgumentException(failure.reason().name());
        }
        return context.getString(key, failure.detail());
    }
}
