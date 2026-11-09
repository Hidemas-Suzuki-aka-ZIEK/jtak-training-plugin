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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.atakmap.android.maps.MapView;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jp.jtak.core.cas.CasProductResult;
import jp.jtak.core.cas.Routing;
import jp.jtak.core.cas.RoutingClosing;
import jp.jtak.core.cas.RoutingFormatter;
import jp.jtak.core.cas.RoutingInstruction;
import jp.jtak.core.export.ExportKind;
import jp.jtak.core.export.ExportText;
import jp.jtak.core.importer.RoutingImport;
import jp.jtak.core.importer.ScenarioImportResult;
import jp.jtak.core.importer.ScenarioKind;
import jp.jtak.plugin.R;
import jp.jtak.plugin.export.ExportShare;
import jp.jtak.plugin.export.ExportException;
import jp.jtak.plugin.export.ExportWriter;
import jp.jtak.plugin.export.PdfExporter;

/**
 * ルーティングの画面（03 3 章）。画面とファイルに出し、ATAK の送信やネットワークの API は呼ばない（U8）。
 * 場所は手入力のほか、地図の点の一覧（{@link MapPointLister}）から選んだ点の名前を入れられる。
 * <p>
 * Routing screen (03 §3). Outputs to the screen and files, without calling ATAK transmission or network APIs (U8).
 * In addition to manual entry, location can be filled with the name of a point selected from the map point list
 * ({@link MapPointLister}).
 */
public final class RoutingPanel {
    private static final RoutingInstruction[] INSTRUCTIONS = RoutingInstruction.values();
    private static final RoutingClosing[] CLOSINGS = {RoutingClosing.SEND, RoutingClosing.HOLD};

    private final MapView mapView;
    private final Context context;
    private final PanelKit kit;
    private final Spinner pointSpinner;
    private final TextView fillStatus;
    private final TextView missingView;
    private final TextView resultView;
    private final View exportTextButton;
    private final View exportPdfButton;
    private String lastResultText;
    private final EditText callsign;
    private final Spinner instruction;
    private final EditText place;
    private final EditText otherInstructions;
    private final CheckBox onlyAircraft;
    private final EditText otherAos;
    private final EditText threats;
    private final EditText weatherTerrain;
    private final EditText statusCalls;
    private final Spinner closing;
    private final EditText holdReason;
    private final ImportFlow importFlow;
    private List<MapPoint> points = Collections.emptyList();

    public RoutingPanel(MapView mapView, Context context, View content) {
        this.mapView = mapView;
        this.context = context;
        kit = new PanelKit(context);
        pointSpinner = content.findViewById(R.id.routing_spinner_place);
        fillStatus = content.findViewById(R.id.routing_fill_status);
        missingView = content.findViewById(R.id.routing_missing);
        resultView = content.findViewById(R.id.routing_result);
        exportTextButton = content.findViewById(R.id.routing_export_text);
        exportPdfButton = content.findViewById(R.id.routing_export_pdf);
        setExportResult(null);
        exportTextButton.setOnClickListener(v -> export(false));
        exportPdfButton.setOnClickListener(v -> export(true));

        LinearLayout fields = content.findViewById(R.id.routing_fields);
        callsign = kit.addField(fields, R.string.routing_callsign);
        instruction = kit.addChoice(fields, R.string.routing_instruction, R.string.choice_none,
                R.string.routing_instr_proceed, R.string.routing_instr_maintain,
                R.string.routing_instr_report_passing);
        place = kit.addField(fields, R.string.routing_place);
        otherInstructions = kit.addField(fields, R.string.routing_other_instructions);
        onlyAircraft = kit.addCheck(fields, R.string.routing_only_aircraft);
        otherAos = kit.addField(fields, R.string.routing_other_aos);
        threats = kit.addField(fields, R.string.routing_threats);
        weatherTerrain = kit.addField(fields, R.string.routing_weather_terrain);
        statusCalls = kit.addField(fields, R.string.routing_status_calls);
        closing = kit.addChoice(fields, R.string.routing_closing, R.string.choice_none,
                R.string.routing_closing_send, R.string.routing_closing_hold);
        holdReason = kit.addField(fields, R.string.routing_hold_reason);

        importFlow = new ImportFlow(mapView, context, ScenarioKind.ROUTING, new ImportScreen());
        kit.addImportButton(fields, importFlow::start);

        content.findViewById(R.id.routing_refresh).setOnClickListener(v -> refreshPoints());
        content.findViewById(R.id.routing_fill).setOnClickListener(v -> fillPlace());
        content.findViewById(R.id.routing_make).setOnClickListener(v -> make());
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
     * ファイルから読み込む（設計 06 の 3 章）。地図の点の選ぶ欄は戻さず、場所は文字の欄だけに入れる。
     * <p>
     * Imports from a file (design 06 §3). Does not reset the map point selector; fills location only in the text
     * field.
     */
    private final class ImportScreen implements ImportFlow.Screen {
        @Override
        public boolean hasInput() {
            return PanelKit.anyText(callsign, place, otherInstructions, otherAos, threats,
                    weatherTerrain, statusCalls, holdReason)
                    || onlyAircraft.isChecked()
                    || instruction.getSelectedItemPosition() != 0
                    || closing.getSelectedItemPosition() != 0;
        }

        @Override
        public void resetInput() {
            PanelKit.clear(callsign, place, otherInstructions, otherAos, threats,
                    weatherTerrain, statusCalls, holdReason);
            onlyAircraft.setChecked(false);
            instruction.setSelection(0);
            closing.setSelection(0);
        }

        @Override
        public void fill(ScenarioImportResult result) {
            RoutingImport r = result.routing();
            if (r == null) {
                return;
            }
            PanelKit.set(callsign, r.callsign());
            PanelKit.select(instruction, INSTRUCTIONS, r.instruction());
            PanelKit.set(place, r.place());
            PanelKit.set(otherInstructions, r.otherInstructions());
            PanelKit.set(onlyAircraft, r.onlyAircraftOnStation());
            PanelKit.set(otherAos, r.otherAos());
            PanelKit.set(threats, r.threats());
            PanelKit.set(weatherTerrain, r.weatherTerrain());
            PanelKit.set(statusCalls, r.statusCalls());
            PanelKit.select(closing, CLOSINGS, r.closing());
            PanelKit.set(holdReason, r.holdReason());
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

    private void refreshPoints() {
        points = MapPointLister.list(mapView);
        Object before = pointSpinner.getSelectedItem();
        String keep = before instanceof MapPoint ? ((MapPoint) before).uid() : null;
        List<Object> items = new ArrayList<>();
        items.add(context.getString(R.string.choice_none));
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
        pointSpinner.setAdapter(adapter);
        if (keep != null) {
            for (int i = 0; i < points.size(); i++) {
                if (keep.equals(points.get(i).uid())) {
                    pointSpinner.setSelection(i + 1);
                    break;
                }
            }
        }
        fillStatus.setText(context.getString(R.string.cas_points_count, points.size()));
    }

    /**
     * 「地図の点から入れる」: 選んだ点の名前（callsign）を場所の欄に入れる。
     * <p>
     * “Fill from map points”: fills the location field with the selected point's name (callsign).
     */
    private void fillPlace() {
        Object o = pointSpinner.getSelectedItem();
        if (o instanceof MapPoint) {
            place.setText(((MapPoint) o).name());
            fillStatus.setText(R.string.routing_fill_done);
        } else {
            fillStatus.setText(R.string.routing_fill_none);
        }
    }

    private void make() {
        Routing routing;
        try {
            routing = new Routing.Builder()
                    .callsign(PanelKit.text(callsign))
                    .instruction(PanelKit.picked(instruction, INSTRUCTIONS))
                    .place(PanelKit.text(place))
                    .otherInstructions(PanelKit.text(otherInstructions))
                    .onlyAircraftOnStation(onlyAircraft.isChecked())
                    .otherAos(PanelKit.text(otherAos))
                    .threats(PanelKit.text(threats))
                    .weatherTerrain(PanelKit.text(weatherTerrain))
                    .statusCalls(PanelKit.text(statusCalls))
                    .closing(PanelKit.picked(closing, CLOSINGS))
                    .holdReason(PanelKit.text(holdReason))
                    .build();
        } catch (IllegalArgumentException e) {
            missingView.setText(context.getString(R.string.routing_conflict));
            resultView.setText("");
            setExportResult(null);
            return;
        }
        CasProductResult result = RoutingFormatter.format(routing);
        missingView.setText(PanelKit.missingText(context, result.missingItems()));
        resultView.setTypeface(Typeface.MONOSPACE);
        resultView.setText(result.text());
        setExportResult(result.text());
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
                    ? new PdfExporter().writePdf(ExportKind.ROUTING, lastResultText, time)
                    : new ExportWriter().writeText(ExportKind.ROUTING, lastResultText, time);
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
