package jp.jtak.plugin;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import jp.jtak.core.TrainingNotice;
import jp.jtak.core.lrf.AzimuthReference;
import jp.jtak.core.lrf.InputSource;
import jp.jtak.core.lrf.RangeReading;
import jp.jtak.plugin.cas.CasBriefPanel;
import jp.jtak.plugin.cas.CheckInPanel;
import jp.jtak.plugin.cas.GamePlanPanel;
import jp.jtak.plugin.cas.RoutingPanel;
import jp.jtak.plugin.lrf.LrfAutoMarkingSwitch;
import jp.jtak.plugin.lrf.LrfMarkingController;
import jp.jtak.plugin.weapons.WeaponPanel;

public final class JtakDropDownReceiver extends DropDownReceiver implements OnStateListener {
    public static final String SHOW_PLUGIN = "jp.jtak.plugin.SHOW_PLUGIN";
    private final View content;
    private final CasBriefPanel casPanel;
    private final RoutingPanel routingPanel;
    private final CheckInPanel checkInPanel;
    private final GamePlanPanel gamePlanPanel;
    private final WeaponPanel weaponPanel;

    public JtakDropDownReceiver(MapView mapView, Context context, LrfMarkingController controller,
            LrfAutoMarkingSwitch autoSwitch) {
        super(mapView);
        content = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
        ((TextView) content.findViewById(R.id.training_header)).setText(TrainingNotice.text());

        final View lrfSection = content.findViewById(R.id.lrf_section);
        final View casSection = content.findViewById(R.id.cas_section);
        final View routingSection = content.findViewById(R.id.routing_section);
        final View checkInSection = content.findViewById(R.id.checkin_section);
        final View gamePlanSection = content.findViewById(R.id.gameplan_section);
        final View weaponsSection = content.findViewById(R.id.weapons_section);
        final View[] sections = {lrfSection, casSection, routingSection, checkInSection, gamePlanSection,
                weaponsSection};
        casPanel = new CasBriefPanel(mapView, context, content);
        routingPanel = new RoutingPanel(mapView, context, content);
        checkInPanel = new CheckInPanel(mapView, context, content);
        gamePlanPanel = new GamePlanPanel(mapView, context, content);
        weaponPanel = new WeaponPanel(mapView, context, content);
        content.findViewById(R.id.mode_lrf).setOnClickListener(v -> show(sections, lrfSection));
        content.findViewById(R.id.mode_cas).setOnClickListener(v -> {
            show(sections, casSection);
            casPanel.onShown();
        });
        content.findViewById(R.id.mode_routing).setOnClickListener(v -> {
            show(sections, routingSection);
            routingPanel.onShown();
        });
        content.findViewById(R.id.mode_checkin).setOnClickListener(v -> show(sections, checkInSection));
        content.findViewById(R.id.mode_gameplan).setOnClickListener(v -> show(sections, gamePlanSection));
        content.findViewById(R.id.mode_weapons).setOnClickListener(v -> {
            show(sections, weaponsSection);
            weaponPanel.onShown();
        });

        Switch auto = content.findViewById(R.id.lrf_auto_switch);
        auto.setChecked(autoSwitch.isEnabled());
        auto.setOnCheckedChangeListener((CompoundButton b, boolean on) -> autoSwitch.setEnabled(on));

        final EditText distance = content.findViewById(R.id.lrf_distance);
        final EditText azimuth = content.findViewById(R.id.lrf_azimuth);
        final EditText elevation = content.findViewById(R.id.lrf_elevation);
        final RadioButton trueRef = content.findViewById(R.id.lrf_ref_true);
        content.findViewById(R.id.lrf_place).setOnClickListener(v -> {
            try {
                double d = Double.parseDouble(distance.getText().toString().trim());
                double a = Double.parseDouble(azimuth.getText().toString().trim());
                double e = Double.parseDouble(elevation.getText().toString().trim());
                AzimuthReference ref = trueRef.isChecked() ? AzimuthReference.TRUE : AzimuthReference.MAGNETIC;
                controller.handle(new RangeReading(d, a, ref, e, System.currentTimeMillis(),
                        InputSource.MANUAL, null));
            } catch (NumberFormatException ex) {
                controller.toast(TrainingNotice.text() + " — " + context.getString(R.string.lrf_input_error));
            }
        });
    }

    /**
     * 表の節を 1 つだけ出す。
     * <p>
     * Displays just one section of the table.
     */
    private static void show(View[] sections, View shown) {
        for (View section : sections) {
            section.setVisibility(section == shown ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SHOW_PLUGIN.equals(intent.getAction())) {
            showDropDown(content, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false, this);
        }
    }

    @Override
    protected void disposeImpl() {
        casPanel.close();
        routingPanel.close();
        checkInPanel.close();
        gamePlanPanel.close();
        weaponPanel.removeCircles();
    }
    @Override
    public void onDropDownSelectionRemoved() {}
    @Override
    public void onDropDownVisible(boolean visible) {}
    @Override
    public void onDropDownSizeChanged(double width, double height) {}
    @Override
    public void onDropDownClose() {}
}
