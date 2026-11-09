package jp.jtak.plugin.cas;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import jp.jtak.plugin.R;

/**
 * CAS 系の画面で使う、入力の欄の部品（ラベル・文字の欄・チェック・選ぶ欄）の作り方。
 * <p>
 * Creates input components (labels, text fields, checkboxes, and selectors) for CAS screens.
 */
public final class PanelKit {
    private final Context context;

    public PanelKit(Context context) {
        this.context = context;
    }

    public int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    public TextView addLabel(LinearLayout parent, int textRes) {
        TextView t = new TextView(context);
        t.setTextSize(13);
        t.setText(textRes);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, dp(4), 0, 0);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    /**
     * 補足の説明（灰色の小さめの字）。
     * <p>
     * Supplementary explanation (smaller gray text).
     */
    public TextView addNote(LinearLayout parent, int textRes) {
        TextView t = new TextView(context);
        t.setText(textRes);
        t.setTextColor(Color.parseColor("#9AA0A6"));
        t.setTextSize(11);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    public EditText addField(LinearLayout parent, int labelRes) {
        addLabel(parent, labelRes);
        EditText e = new EditText(context);
        e.setTextSize(13);
        e.setTextColor(Color.WHITE);
        e.setSingleLine(false);
        parent.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    public CheckBox addCheck(LinearLayout parent, int labelRes) {
        CheckBox c = new CheckBox(context);
        c.setTextSize(13);
        c.setText(labelRes);
        c.setTextColor(Color.WHITE);
        parent.addView(c, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    /**
     * 選ぶ欄。先頭は「選ばない」（既定）で、そのあとに {@code optionRes} の順に並べる。
     * 選んだ番号から 1 を引いたものが {@code optionRes} の番号（0 なら選ばない）。
     * <p>
     * Selector. Starts with “no selection” (default), followed by items in {@code optionRes} order. The selected
     * index minus one is the {@code optionRes} index (0 means no selection).
     */
    public Spinner addChoice(LinearLayout parent, int labelRes, int noneRes, int... optionRes) {
        addLabel(parent, labelRes);
        List<String> items = new ArrayList<>();
        items.add(context.getString(noneRes));
        for (int r : optionRes) {
            items.add(context.getString(r));
        }
        Spinner s = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup p) {
                View v = super.getView(position, convertView, p);
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        parent.addView(s, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    /**
     * 選ぶ欄の選択を、値の配列の要素にする（「選ばない」なら null）。
     * <p>
     * Converts a selector's selection to an element of the values array (null for “no selection”).
     */
    static <T> T picked(Spinner spinner, T[] values) {
        int i = spinner.getSelectedItemPosition() - 1;
        return i < 0 || i >= values.length ? null : values[i];
    }

    public static String text(EditText e) {
        return e.getText().toString();
    }

    /**
     * 「ファイルから読み込む」のボタンを、入力欄の並びの先頭に置く（設計 06 の 3.1）。
     * <p>
     * Places the “Import from file” button before the input fields (design 06 §3.1).
     */
    public Button addImportButton(LinearLayout parent, Runnable onClick) {
        Button b = new Button(context);
        b.setTextSize(13);
        b.setText(R.string.import_button);
        b.setOnClickListener(v -> onClick.run());
        parent.addView(b, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    /**
     * 文字の欄のどれかに値があるか。
     * <p>
     * Whether any text field has a value.
     */
    static boolean anyText(EditText... fields) {
        for (EditText e : fields) {
            if (e.getText().length() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 文字の欄をすべて空にする。
     * <p>
     * Clears all text fields.
     */
    static void clear(EditText... fields) {
        for (EditText e : fields) {
            e.setText("");
        }
    }

    /**
     * 値があれば（null でなければ）文字の欄に入れる。
     * <p>
     * Fills the text field if the value is non-null.
     */
    static void set(EditText field, String value) {
        if (value != null) {
            field.setText(value);
        }
    }

    /**
     * 値があれば（null でなければ）チェックに入れる。
     * <p>
     * Sets the checkbox if the value is non-null.
     */
    static void set(CheckBox box, Boolean value) {
        if (value != null) {
            box.setChecked(value);
        }
    }

    /**
     * 値があれば（null でなければ）選ぶ欄で選ぶ。 {@link #picked} の逆。
     * <p>
     * Selects the value in the selector if non-null. The inverse of {@link #picked}.
     */
    static <T> void select(Spinner spinner, T[] values, T value) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                spinner.setSelection(i + 1);
                return;
            }
        }
    }

    static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p);
        }
        return sb.toString();
    }

    /**
     * 未入力の項目と入力の食い違いを、画面に出す文にする。
     * <p>
     * Converts missing items and inconsistent input into text for the screen.
     */
    static String missingText(Context context, List<String> missing) {
        return missing.isEmpty() ? "" : context.getString(R.string.missing_items, join(missing));
    }
}
