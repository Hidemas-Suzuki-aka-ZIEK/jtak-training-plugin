package jp.jtak.plugin;

import android.content.Context;
import android.content.Intent;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import jp.jtak.plugin.lrf.JtakLrfAction;
import jp.jtak.plugin.lrf.LrfAutoMarkingSwitch;
import jp.jtak.plugin.lrf.LrfMarkingController;

public final class JtakMapComponent extends DropDownMapComponent {
    private LrfAutoMarkingSwitch autoSwitch;

    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        context.setTheme(R.style.JtakTheme);
        super.onCreate(context, intent, view);
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(JtakDropDownReceiver.SHOW_PLUGIN);
        LrfMarkingController controller = new LrfMarkingController(view, JtakLang.of(context));
        autoSwitch = new LrfAutoMarkingSwitch(view, new JtakLrfAction(view, controller));
        autoSwitch.applyStored();
        registerDropDownReceiver(new JtakDropDownReceiver(view, context, controller, autoSwitch), filter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        if (autoSwitch != null) {
            autoSwitch.release();
        }
        super.onDestroyImpl(context, view);
    }
}
