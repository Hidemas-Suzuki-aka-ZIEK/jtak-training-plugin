package jp.jtak.plugin.lrf;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.atakmap.android.lrf.LocalRangeFinderInput;
import com.atakmap.android.lrf.RangeFinderAction2;
import com.atakmap.android.maps.MapView;

/**
 * 自動マーキングの入切と、{@code registerAction} の登録・解除。
 * 既知の限界: ATAK には登録中の動作を読む API がない。解除は標準の動作（null）に戻す。
 * <p>
 * Switches automatic marking on/off and registers/unregisters {@code registerAction}. Known limitation: ATAK has
 * no API to read the currently registered action. Unregistering restores the standard action (null).
 */
public final class LrfAutoMarkingSwitch {
    private static final String TAG = "JtakLrf";
    public static final String PREF_KEY = "jtak.lrf.auto_marking";
    private final MapView mapView;
    private final JtakLrfAction action;
    private boolean registered;

    public LrfAutoMarkingSwitch(MapView mapView, JtakLrfAction action) {
        this.mapView = mapView;
        this.action = action;
    }

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
    }

    public boolean isEnabled() {
        return prefs().getBoolean(PREF_KEY, true);
    }

    public void setEnabled(boolean enabled) {
        prefs().edit().putBoolean(PREF_KEY, enabled).apply();
        apply(enabled);
    }

    /**
     * 設定どおりに登録する（起動時）。
     * <p>
     * Registers according to settings (at startup).
     */
    public void applyStored() {
        apply(isEnabled());
    }

    private void apply(boolean enabled) {
        if (!enabled) {
            action.setActive(false);
        }
        LocalRangeFinderInput input = LocalRangeFinderInput.getInstance();
        if (input == null) {
            Log.w(TAG, "LocalRangeFinderInput が取れない。LRF の自動マーキングは登録されない");
            return;
        }
        if (enabled) {
            input.registerAction(action);
            registered = true;
            action.setActive(true);
        } else if (registered) {
            input.registerAction((RangeFinderAction2) null);
            registered = false;
        }
    }

    /**
     * 止めるとき。JTAK が登録していたなら標準の動作に戻す。
     * <p>
     * When stopping, restores the standard action if JTAK had registered its action.
     */
    public void release() {
        action.setActive(false);
        if (registered) {
            LocalRangeFinderInput input = LocalRangeFinderInput.getInstance();
            if (input != null) {
                input.registerAction((RangeFinderAction2) null);
            }
            registered = false;
        }
    }
}
