package jp.jtak.plugin.cas;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
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
import jp.jtak.core.cas.CasProductResult;
import jp.jtak.core.cas.CheckIn;
import jp.jtak.core.cas.CheckInFormatter;
import jp.jtak.core.cas.CheckInMode;
import jp.jtak.core.export.ExportKind;
import jp.jtak.core.export.ExportText;
import jp.jtak.core.importer.CheckInImport;
import jp.jtak.core.importer.ScenarioImportResult;
import jp.jtak.core.importer.ScenarioKind;
import jp.jtak.plugin.R;
import jp.jtak.plugin.export.ExportShare;
import jp.jtak.plugin.export.ExportException;
import jp.jtak.plugin.export.ExportWriter;
import jp.jtak.plugin.export.PdfExporter;

/**
 * チェックインの画面（03 3 章）。画面とファイルに出し、自動で送信する API は呼ばない（U8）。
 * <p>
 * Check-in screen (03 §3). Outputs to the screen and files, without calling automatic transmission APIs (U8).
 */
public final class CheckInPanel {
    private static final CheckInMode[] MODES = {CheckInMode.FULL, CheckInMode.AS_FRAGGED, CheckInMode.WITH_EXCEPTION};

    private final Context context;
    private final MapView mapView;
    private final TextView missingView;
    private final TextView resultView;
    private final View exportTextButton;
    private final View exportPdfButton;
    private String lastResultText;
    private final EditText jtacCallsign;
    private final EditText aircraftCallsign;
    private final Spinner mode;
    private final EditText missionNumber;
    private final EditText aircraft;
    private final EditText positionAltitude;
    private final EditText ordnance;
    private final EditText playtime;
    private final EditText capabilities;
    private final EditText abortCode;
    private final CheckBox abortCodeNone;
    private final ImportFlow importFlow;

    public CheckInPanel(MapView mapView, Context context, View content) {
        this.context = context;
        this.mapView = mapView;
        PanelKit kit = new PanelKit(context);
        missingView = content.findViewById(R.id.checkin_missing);
        resultView = content.findViewById(R.id.checkin_result);
        exportTextButton = content.findViewById(R.id.checkin_export_text);
        exportPdfButton = content.findViewById(R.id.checkin_export_pdf);
        setExportResult(null);
        exportTextButton.setOnClickListener(v -> export(false));
        exportPdfButton.setOnClickListener(v -> export(true));

        LinearLayout fields = content.findViewById(R.id.checkin_fields);
        jtacCallsign = kit.addField(fields, R.string.checkin_jtac_callsign);
        aircraftCallsign = kit.addField(fields, R.string.checkin_aircraft_callsign);
        mode = kit.addChoice(fields, R.string.checkin_mode, R.string.choice_none,
                R.string.checkin_mode_full, R.string.checkin_mode_as_fragged,
                R.string.checkin_mode_with_exception);
        missionNumber = kit.addField(fields, R.string.checkin_mission_number);
        aircraft = kit.addField(fields, R.string.checkin_aircraft);
        positionAltitude = kit.addField(fields, R.string.checkin_position_altitude);
        ordnance = kit.addField(fields, R.string.checkin_ordnance);
        playtime = kit.addField(fields, R.string.checkin_playtime);
        capabilities = kit.addField(fields, R.string.checkin_capabilities);
        abortCode = kit.addField(fields, R.string.checkin_abort_code);
        abortCodeNone = kit.addCheck(fields, R.string.checkin_abort_code_none);

        importFlow = new ImportFlow(mapView, context, ScenarioKind.CHECK_IN, new ImportScreen());
        kit.addImportButton(fields, importFlow::start);

        content.findViewById(R.id.checkin_make).setOnClickListener(v -> make());
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
     * ファイルから読み込む（設計 06 の 3 章）。モードの行がないファイルは FULL として入る。
     * <p>
     * Imports from a file (design 06 §3). Files without a mode line are imported as FULL.
     */
    private final class ImportScreen implements ImportFlow.Screen {
        @Override
        public boolean hasInput() {
            return PanelKit.anyText(jtacCallsign, aircraftCallsign, missionNumber, aircraft,
                    positionAltitude, ordnance, playtime, capabilities, abortCode)
                    || abortCodeNone.isChecked()
                    || mode.getSelectedItemPosition() != 0;
        }

        @Override
        public void resetInput() {
            PanelKit.clear(jtacCallsign, aircraftCallsign, missionNumber, aircraft,
                    positionAltitude, ordnance, playtime, capabilities, abortCode);
            abortCodeNone.setChecked(false);
            mode.setSelection(0);
        }

        @Override
        public void fill(ScenarioImportResult result) {
            CheckInImport c = result.checkIn();
            if (c == null) {
                return;
            }
            PanelKit.set(jtacCallsign, c.jtacCallsign());
            PanelKit.set(aircraftCallsign, c.aircraftCallsign());
            PanelKit.select(mode, MODES, c.mode());
            PanelKit.set(missionNumber, c.missionNumber());
            PanelKit.set(aircraft, c.aircraft());
            PanelKit.set(positionAltitude, c.positionAltitude());
            PanelKit.set(ordnance, c.ordnance());
            PanelKit.set(playtime, c.playtime());
            PanelKit.set(capabilities, c.capabilities());
            PanelKit.set(abortCode, c.abortCode());
            PanelKit.set(abortCodeNone, c.abortCodeNone());
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

    private void make() {
        CheckIn checkIn;
        try {
            checkIn = new CheckIn.Builder()
                    .jtacCallsign(PanelKit.text(jtacCallsign))
                    .aircraftCallsign(PanelKit.text(aircraftCallsign))
                    .mode(PanelKit.picked(mode, MODES))
                    .missionNumber(PanelKit.text(missionNumber))
                    .aircraft(PanelKit.text(aircraft))
                    .positionAltitude(PanelKit.text(positionAltitude))
                    .ordnance(PanelKit.text(ordnance))
                    .playtime(PanelKit.text(playtime))
                    .capabilities(PanelKit.text(capabilities))
                    .abortCode(PanelKit.text(abortCode))
                    .abortCodeNone(abortCodeNone.isChecked())
                    .build();
        } catch (IllegalArgumentException e) {
            missingView.setText(context.getString(R.string.checkin_conflict));
            resultView.setText("");
            setExportResult(null);
            return;
        }
        CasProductResult result = CheckInFormatter.format(checkIn);
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
                    ? new PdfExporter().writePdf(ExportKind.CHECK_IN, lastResultText, time)
                    : new ExportWriter().writeText(ExportKind.CHECK_IN, lastResultText, time);
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
