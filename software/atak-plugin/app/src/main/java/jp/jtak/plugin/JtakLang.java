package jp.jtak.plugin;

import android.content.Context;
import jp.jtak.core.Lang;

public final class JtakLang {
    private JtakLang() {}

    public static Lang of(Context pluginContext) {
        return "ja".equals(pluginContext.getString(R.string.jtak_lang)) ? Lang.JA : Lang.EN;
    }
}
