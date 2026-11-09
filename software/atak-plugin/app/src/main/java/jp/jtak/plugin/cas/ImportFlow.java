package jp.jtak.plugin.cas;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.atakmap.android.gui.ImportFileBrowserDialog;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import jp.jtak.core.importer.ScenarioImportError;
import jp.jtak.core.importer.ScenarioImportResult;
import jp.jtak.core.importer.ScenarioImporter;
import jp.jtak.core.importer.ScenarioKind;
import jp.jtak.plugin.JtakLang;
import jp.jtak.plugin.R;

/**
 * 「ファイルから読み込む」の流れ（設計 06 の 3 章）。ファイルを選ぶ → 別のスレッドで読む →
 * 誤りなら文を出して終わる → 入力中なら確かめる → 欄を戻して値を入れる → 出力を作り直す → 飛ばした項目を出す。
 * ファイルの中身はログに出さない（出すのは誤りの code だけ）。ネットワークは使わない。
 * <p>
 * “Import from file” flow (design 06 §3): select a file → read on another thread → display a message and stop on
 * error → confirm if input exists → reset fields and fill values → regenerate output → display skipped items.
 * Never logs file contents (only error codes). Does not use the network.
 */
final class ImportFlow {
    private static final String TAG = "JtakImport";

    /**
     * 読み込みを受ける画面。
     * <p>
     * Screen receiving the import.
     */
    interface Screen {
        /**
         * 入力欄に 1 つでも値があるか（地図の点の選ぶ欄は数えない）。
         * <p>
         * Whether any input field has a value (excluding map point selectors).
         */
        boolean hasInput();

        /**
         * 入力欄をすべて、画面を開いたときの状態に戻す（地図の点の選ぶ欄は戻さない）。
         * <p>
         * Resets all input fields to their initial screen state (without resetting map point selectors).
         */
        void resetInput();

        /**
         * 読めた値（null でないもの）を入力欄に入れる。
         * <p>
         * Fills input fields with successfully read, non-null values.
         */
        void fill(ScenarioImportResult result);

        /**
         * 今までどおり formatter で出力を作り直す。
         * <p>
         * Regenerates output with the formatter as before.
         */
        void regenerate();

        /**
         * 「未入力の行」を出す欄。飛ばした項目も同じ場所に出す。
         * <p>
         * Area for “missing lines.” Skipped items appear in the same area.
         */
        TextView missingView();
    }

    private final MapView mapView;
    private final Context pluginContext;
    private final ScenarioKind kind;
    private final Screen screen;
    /**
     * 画面が閉じられたら true。読み終わったあとに、閉じた画面へ結果を入れない。
     * <p>
     * True when the screen is closed. Does not apply results to a closed screen after reading finishes.
     */
    private volatile boolean closed;
    private final ExecutorService reader = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "jtak-import");
        t.setDaemon(true);
        return t;
    });

    ImportFlow(MapView mapView, Context pluginContext, ScenarioKind kind, Screen screen) {
        this.mapView = mapView;
        this.pluginContext = pluginContext;
        this.kind = kind;
        this.screen = screen;
    }

    /**
     * 画面を閉じるとき。読み込みの途中のスレッドを止める。
     * <p>
     * When closing the screen, stops the thread performing an import.
     */
    void close() {
        closed = true;
        reader.shutdownNow();
    }

    /**
     * ファイルを選ぶ画面を出す（3.1）。
     * <p>
     * Opens the file picker (§3.1).
     */
    void start() {
        File startDir;
        try {
            startDir = FileSystemUtils.getItem(FileSystemUtils.EXPORT_DIRECTORY + "/jtak");
            if (!startDir.isDirectory()) {
                startDir = FileSystemUtils.getRoot();
            }
        } catch (RuntimeException e) {
            startDir = FileSystemUtils.getRoot();
        }
        new ImportFileBrowserDialog(mapView.getContext())
                .setTitle(pluginContext.getString(R.string.import_dialog_title))
                .setExtensionTypes("txt")
                .setStartDirectory(startDir)
                .setOnDismissListener(new ImportFileBrowserDialog.DialogDismissed() {
                    @Override
                    public void onFileSelected(File file) {
                        if (file != null) {
                            read(file);
                        }
                    }

                    @Override
                    public void onDialogClosed() {
                        // 何もしない。
                        // Do nothing.
                    }
                })
                .show();
    }

    private void read(File file) {
        try {
            reader.execute(() -> readOnWorker(file));
        } catch (RejectedExecutionException e) {
            Log.d(TAG, "画面が閉じたので読み込まない");
        }
    }

    /**
     * 別のスレッドで読む。大きさの確かめは core に任せる（app は先に File.length() を見ない）。
     * <p>
     * Reads on another thread. Delegates the size check to core (app does not check File.length() first).
     */
    private void readOnWorker(File file) {
        ScenarioImportResult result = null;
        try (FileInputStream in = new FileInputStream(file)) {
            result = ScenarioImporter.read(in, kind, JtakLang.of(pluginContext));
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "ファイルを読めなかった");
        }
        ScenarioImportResult done = result;
        mapView.post(() -> onRead(done));
    }

    private void onRead(ScenarioImportResult result) {
        if (closed) {
            return;
        }
        if (result == null) {
            Toast.makeText(mapView.getContext(), pluginContext.getString(R.string.import_read_failed),
                    Toast.LENGTH_LONG).show();
            return;
        }
        ScenarioImportError error = result.error();
        if (error != null) {
            Log.w(TAG, "読み込まなかった code=" + error.code());
            screen.missingView().setText(error.message());
            Toast.makeText(mapView.getContext(), error.message(), Toast.LENGTH_LONG).show();
            return;
        }
        if (!screen.hasInput()) {
            apply(result);
            return;
        }
        new AlertDialog.Builder(mapView.getContext())
                .setMessage(pluginContext.getString(R.string.import_confirm_message))
                .setPositiveButton(pluginContext.getString(R.string.import_confirm_ok),
                        (dialog, which) -> apply(result))
                .setNegativeButton(pluginContext.getString(R.string.import_confirm_cancel), null)
                .show();
    }

    private void apply(ScenarioImportResult result) {
        if (closed) {
            return;
        }
        screen.resetInput();
        screen.fill(result);
        screen.regenerate();
        TextView view = screen.missingView();
        if (result.skippedItems().isEmpty()) {
            Toast.makeText(mapView.getContext(), pluginContext.getString(R.string.import_done),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String skipped = pluginContext.getString(R.string.import_skipped_items,
                PanelKit.join(result.skippedItems()));
        CharSequence before = view.getText();
        view.setText(before == null || before.length() == 0 ? skipped : before + "\n" + skipped);
    }
}
