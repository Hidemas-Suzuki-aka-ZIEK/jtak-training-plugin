package jp.jtak.plugin.weapons;

import android.content.Context;
import android.util.Log;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jp.jtak.core.TrainingNotice;
import jp.jtak.core.weapons.WeaponEntry;
import jp.jtak.core.weapons.WeaponRing;
import jp.jtak.plugin.R;
import jp.jtak.plugin.UidPrefixes;

/**
 * 兵器の範囲の円を地図に描く・消す（04 3.4）。
 * 輪 1 つにつき {@link DrawingCircle} 1 つ。保存（persist）・送信はしない（U8）。
 * <p>
 * Draws and removes weapon effect circles on the map (04 §3.4). One {@link DrawingCircle} per ring. Does not
 * persist or transmit them (U8).
 */
final class WeaponCircles {
    private static final String TAG = "JtakWeapons";

    /**
     * ATAK の描画の道具が使う標準のグループ名。
     * <p>
     * Standard group name used by ATAK's drawing tools.
     */
    private static final String ATAK_DRAWING_GROUP = "Drawing Objects";

    /**
     * 輪の線の色（内側から。ARGB）。赤・橙・黄。
     * <p>
     * Ring outline colors, from innermost outward (ARGB): red, orange, yellow.
     */
    private static final int[] STROKE_COLORS = {0xFFFF0000, 0xFFFF8C00, 0xFFFFFF00};

    private static final int NO_FILL = 0x00000000;

    private final MapView mapView;
    private final Context context;
    private final List<DrawingCircle> circles = new ArrayList<>();

    WeaponCircles(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;
    }

    /**
     * 目標の位置に、{@code entry} の輪を描く。描いた輪の数を返す。前に描いた円は残す。
     * 途中で例外が出たら、この回に足した円だけをグループから取り除いてから、その例外をそのまま投げる
     * （前の回の円は残す。04 3.4 の「失敗したとき」）。
     * <p>
     * Draws the rings of {@code entry} at the target position. Returns the number of rings drawn. Keeps previously
     * drawn circles. If an exception occurs partway through, removes only circles added in this call from the
     * group, then rethrows that same exception (keeping circles from previous calls; “On failure” in 04 §3.4).
     */
    int draw(WeaponEntry entry, GeoPoint center, String targetName) {
        List<DrawingCircle> added = new ArrayList<>();
        DrawingCircle inFlight = null;
        try {
            MapGroup group = targetGroup();
            String remarks = remarks(entry, targetName);
            List<WeaponRing> rings = entry.rings();
            for (int i = 0; i < rings.size(); i++) {
                WeaponRing ring = rings.get(i);
                DrawingCircle circle = new DrawingCircle(mapView, UidPrefixes.WEAPON_CIRCLE + UUID.randomUUID(),
                        DrawingCircle.COT_TYPE);
                inFlight = circle;
                circle.setCenterPoint(GeoPointMetaData.wrap(center));
                circle.setRadius(ring.radiusM());
                circle.setStrokeColor(STROKE_COLORS[Math.min(i, STROKE_COLORS.length - 1)]);
                circle.setFillColor(NO_FILL);
                circle.setTitle(title(entry, ring));
                circle.setMetaString("remarks", remarks);
                circle.setMetaBoolean("archive", false);
                group.addItem(circle);
                added.add(circle);
                inFlight = null;
            }
        } catch (RuntimeException e) {
            if (inFlight != null) {
                added.add(inFlight);
            }
            rollback(added);
            throw e;
        }
        circles.addAll(added);
        return added.size();
    }

    /**
     * この回に足した円を、グループに入っているものだけ取り除く。取り除けなくてもログに残して先へ進む。
     * 取り除けなかった円（{@code removeItem} が例外を投げた、または取り除いたあともグループに入っている）は
     * {@link #circles} に入れておき、次の {@link #clear()} でもう一度取り除く（04 3.4）。取り除けた円は入れない。
     * <p>
     * Removes circles added in this call only if they are in the group. Logs removal failures and continues. Keeps
     * circles that could not be removed ({@code removeItem} threw or the circle remained in the group after
     * removal) in {@link #circles} for another removal attempt in the next {@link #clear()} (04 §3.4). Does not
     * keep successfully removed circles.
     */
    private void rollback(List<DrawingCircle> added) {
        for (DrawingCircle circle : added) {
            boolean left = false;
            try {
                MapGroup group = circle.getGroup();
                if (group != null) {
                    group.removeItem(circle);
                    left = circle.getGroup() != null;
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "描き損ねた円を取り除けなかった", e);
                left = true;
            }
            if (left) {
                circles.add(circle);
            }
        }
    }

    /**
     * 描いた円を全部消す。利用者が ATAK の側で先に消した円は飛ばす。消した数を返す。
     * 円ごとに取り除く。{@code removeItem} が例外を投げた円や、取り除いたあともグループに入っている円は、
     * 覚えたまま（次の {@link #clear()} でもう一度取り除く）にして、残りの円の取り除きを続ける。
     * 最後まで回ったあと、例外が出ていたら最初の例外を投げ直す。取り除けなかった円は消した数に入れない（04 3.4）。
     * <p>
     * Removes all drawn circles. Skips circles the user already removed through ATAK. Returns the number removed.
     * Removes circles individually. Keeps circles for which {@code removeItem} throws or which remain in the group
     * afterward (to retry in the next {@link #clear()}), and continues removing the rest. After processing all
     * circles, rethrows the first exception if any occurred. Circles that could not be removed are not counted as
     * removed (04 §3.4).
     */
    int clear() {
        int removed = 0;
        RuntimeException first = null;
        List<DrawingCircle> left = new ArrayList<>();
        for (DrawingCircle circle : circles) {
            try {
                MapGroup group = circle.getGroup();
                if (group == null) {
                    continue;
                }
                group.removeItem(circle);
                if (circle.getGroup() != null) {
                    left.add(circle);
                } else {
                    removed++;
                }
            } catch (RuntimeException e) {
                left.add(circle);
                if (first == null) {
                    first = e;
                }
            }
        }
        circles.clear();
        circles.addAll(left);
        if (first != null) {
            throw first;
        }
        return removed;
    }

    private MapGroup targetGroup() {
        MapGroup root = mapView.getRootGroup();
        MapGroup drawing = root.findMapGroup(ATAK_DRAWING_GROUP);
        if (drawing != null) {
            return drawing;
        }
        String ownName = context.getString(R.string.weapons_group_name);
        MapGroup own = root.findMapGroup(ownName);
        return own != null ? own : root.addGroup(ownName);
    }

    /**
     * 円の題名。文言は strings.xml（04 3.6）。
     * <p>
     * Circle title. Text is in strings.xml (04 §3.6).
     */
    private String title(WeaponEntry entry, WeaponRing ring) {
        return context.getString(R.string.weapons_circle_title, entry.label(), ring.name(), ring.radiusM());
    }

    /**
     * 円の説明（remarks）。ファイルの {@code notice} は入れない。
     * <p>
     * Circle description (remarks). Does not include {@code notice} from the file.
     */
    private String remarks(WeaponEntry entry, String targetName) {
        StringBuilder sb = new StringBuilder();
        sb.append(TrainingNotice.text()).append('\n');
        sb.append(TrainingNotice.weaponSampleText()).append('\n');
        sb.append(context.getString(R.string.weapons_circle_target, targetName));
        String note = entry.note();
        if (note != null && !note.trim().isEmpty()) {
            sb.append('\n').append(note);
        }
        return sb.toString();
    }
}
