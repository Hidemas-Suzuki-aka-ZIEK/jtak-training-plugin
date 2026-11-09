package jp.jtak.plugin.cas;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
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
import jp.jtak.core.cas.GamePlan;
import jp.jtak.core.cas.GamePlanFormatter;
import jp.jtak.core.cas.MethodOfAttack;
import jp.jtak.core.cas.TypeOfControl;
import jp.jtak.core.export.ExportKind;
import jp.jtak.core.export.ExportText;
import jp.jtak.core.importer.GamePlanImport;
import jp.jtak.core.importer.ScenarioImportResult;
import jp.jtak.core.importer.ScenarioKind;
import jp.jtak.plugin.R;
import jp.jtak.plugin.export.ExportShare;
import jp.jtak.plugin.export.ExportException;
import jp.jtak.plugin.export.ExportWriter;
import jp.jtak.plugin.export.PdfExporter;

/**
 * ゲームプランの画面（03 3 章）。Type と BOT/BOC の欄には、図 III-7 の要点を説明として添える
 * （読み上げの文には入れない）。画面とファイルに出し、自動で送信する API は呼ばない（U8）。
 * <p>
 * Game plan screen (03 §3). Adds explanatory notes summarizing Figure III-7 to the Type and BOT/BOC fields
 * (excluded from the read-aloud text). Outputs to the screen and files, without calling automatic transmission
 * APIs (U8).
 */
public final class GamePlanPanel {
    private static final TypeOfControl[] TYPES = TypeOfControl.values();
    private static final MethodOfAttack[] METHODS = MethodOfAttack.values();

    private final Context context;
    private final MapView mapView;
    private final TextView missingView;
    private final TextView resultView;
    private final View exportTextButton;
    private final View exportPdfButton;
    private String lastResultText;
    private final EditText callsign;
    private final Spinner type;
    private final Spinner method;
    private final EditText intent;
    private final EditText collateral;
    private final EditText ordnance;
    private final EditText effects;
    private final EditText fuzing;
    private final EditText interval;
    private final EditText line6Digits;
    private final ImportFlow importFlow;

    public GamePlanPanel(MapView mapView, Context context, View content) {
        this.context = context;
        this.mapView = mapView;
        PanelKit kit = new PanelKit(context);
        missingView = content.findViewById(R.id.gameplan_missing);
        resultView = content.findViewById(R.id.gameplan_result);
        exportTextButton = content.findViewById(R.id.gameplan_export_text);
        exportPdfButton = content.findViewById(R.id.gameplan_export_pdf);
        setExportResult(null);
        exportTextButton.setOnClickListener(v -> export(false));
        exportPdfButton.setOnClickListener(v -> export(true));

        LinearLayout fields = content.findViewById(R.id.gameplan_fields);
        callsign = kit.addField(fields, R.string.gameplan_callsign);
        kit.addNote(fields, R.string.gameplan_note);
        type = kit.addChoice(fields, R.string.gameplan_type, R.string.choice_none,
                R.string.gameplan_type_1, R.string.gameplan_type_2, R.string.gameplan_type_3);
        kit.addNote(fields, R.string.gameplan_type_1_desc);
        kit.addNote(fields, R.string.gameplan_type_2_desc);
        kit.addNote(fields, R.string.gameplan_type_3_desc);
        method = kit.addChoice(fields, R.string.gameplan_method, R.string.choice_none,
                R.string.gameplan_method_bot, R.string.gameplan_method_boc);
        kit.addNote(fields, R.string.gameplan_bot_desc);
        kit.addNote(fields, R.string.gameplan_boc_desc);
        intent = kit.addField(fields, R.string.gameplan_intent);
        collateral = kit.addField(fields, R.string.gameplan_collateral);
        ordnance = kit.addField(fields, R.string.gameplan_ordnance);
        effects = kit.addField(fields, R.string.gameplan_effects);
        fuzing = kit.addField(fields, R.string.gameplan_fuzing);
        interval = kit.addField(fields, R.string.gameplan_interval);
        line6Digits = kit.addField(fields, R.string.gameplan_line6_digits);

        importFlow = new ImportFlow(mapView, context, ScenarioKind.GAME_PLAN, new ImportScreen());
        kit.addImportButton(fields, importFlow::start);

        content.findViewById(R.id.gameplan_make).setOnClickListener(v -> make());
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
     * ファイルから読み込む（設計 06 の 3 章）。
     * <p>
     * Imports from a file (design 06 §3).
     */
    private final class ImportScreen implements ImportFlow.Screen {
        @Override
        public boolean hasInput() {
            return PanelKit.anyText(callsign, intent, collateral, ordnance, effects, fuzing,
                    interval, line6Digits)
                    || type.getSelectedItemPosition() != 0
                    || method.getSelectedItemPosition() != 0;
        }

        @Override
        public void resetInput() {
            PanelKit.clear(callsign, intent, collateral, ordnance, effects, fuzing,
                    interval, line6Digits);
            type.setSelection(0);
            method.setSelection(0);
        }

        @Override
        public void fill(ScenarioImportResult result) {
            GamePlanImport g = result.gamePlan();
            if (g == null) {
                return;
            }
            PanelKit.set(callsign, g.callsign());
            PanelKit.select(type, TYPES, g.typeOfControl());
            PanelKit.select(method, METHODS, g.methodOfAttack());
            PanelKit.set(intent, g.intent());
            PanelKit.set(collateral, g.collateralConcerns());
            PanelKit.set(ordnance, g.ordnance());
            PanelKit.set(effects, g.desiredEffects());
            PanelKit.set(fuzing, g.fuzing());
            PanelKit.set(interval, g.interval());
            PanelKit.set(line6Digits, g.line6Digits());
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
        GamePlan plan = new GamePlan.Builder()
                .callsign(PanelKit.text(callsign))
                .typeOfControl(PanelKit.picked(type, TYPES))
                .methodOfAttack(PanelKit.picked(method, METHODS))
                .intent(PanelKit.text(intent))
                .collateralConcerns(PanelKit.text(collateral))
                .ordnance(PanelKit.text(ordnance))
                .desiredEffects(PanelKit.text(effects))
                .fuzing(PanelKit.text(fuzing))
                .interval(PanelKit.text(interval))
                .line6Digits(PanelKit.text(line6Digits))
                .build();
        CasProductResult result = GamePlanFormatter.format(plan);
        missingView.setText(PanelKit.missingText(missingView.getContext(), result.missingItems()));
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
                    ? new PdfExporter().writePdf(ExportKind.GAME_PLAN, lastResultText, time)
                    : new ExportWriter().writeText(ExportKind.GAME_PLAN, lastResultText, time);
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
