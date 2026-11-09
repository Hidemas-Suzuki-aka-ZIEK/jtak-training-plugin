package jp.jtak.plugin;

import android.content.Context;
import com.atak.plugins.impl.AbstractPluginTool;
import gov.tak.api.util.Disposable;

public final class JtakTool extends AbstractPluginTool implements Disposable {
    public JtakTool(Context context) {
        super(context, context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getDrawable(R.drawable.ic_jtak), JtakDropDownReceiver.SHOW_PLUGIN);
    }

    @Override
    public void dispose() {}
}
