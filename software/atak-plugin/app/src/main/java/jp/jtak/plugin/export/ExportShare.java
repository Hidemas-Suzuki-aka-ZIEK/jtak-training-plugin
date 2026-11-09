package jp.jtak.plugin.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.FileProviderHelper;
import java.io.File;
import jp.jtak.plugin.R;

/**
 * 利用者が書き出しを押したときだけ、Android の共有画面を開く。
 * <p>
 * Opens Android's share screen only when the user presses export.
 */
public final class ExportShare {
    private final Context pluginContext;

    public ExportShare(Context pluginContext) {
        this.pluginContext = pluginContext;
    }

    public void share(MapView mapView, File file, String mimeType, String fallbackText) {
        try {
            Context atakContext = mapView.getContext();
            Intent intent = new Intent(Intent.ACTION_SEND).setType(mimeType);
            Uri uri = FileProviderHelper.fromFile(atakContext, file);
            if (uri != null) {
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                FileProviderHelper.setReadAccess(intent);
                intent.setClipData(android.content.ClipData.newRawUri(file.getName(), uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else if ("text/plain".equals(mimeType)) {
                intent.putExtra(Intent.EXTRA_TEXT, fallbackText);
            } else {
                savedWithoutShare(atakContext, file);
                return;
            }
            Intent chooser = Intent.createChooser(intent, pluginContext.getString(R.string.export_share_title));
            if (uri != null) {
                chooser.setClipData(intent.getClipData());
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            atakContext.startActivity(chooser);
        } catch (RuntimeException e) {
            Log.e("JtakExport", "共有の画面を開けなかった", e);
            savedWithoutShare(mapView.getContext(), file);
        }
    }

    private void savedWithoutShare(Context atakContext, File file) {
        Toast.makeText(atakContext, pluginContext.getString(R.string.export_share_failed,
                file.getAbsolutePath()), Toast.LENGTH_LONG).show();
    }
}
