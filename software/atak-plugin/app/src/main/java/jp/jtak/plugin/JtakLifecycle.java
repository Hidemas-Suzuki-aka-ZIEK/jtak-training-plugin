package jp.jtak.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import gov.tak.api.plugin.IServiceController;

/**
 * PluginTemplateLegacy と同じライフサイクルで読み込む。
 * <p>
 * Loads using the same lifecycle as PluginTemplateLegacy.
 */
public final class JtakLifecycle extends AbstractPlugin {
    public JtakLifecycle(IServiceController controller) {
        super(controller,
                new JtakTool(controller.getService(PluginContextProvider.class).getPluginContext()),
                new JtakMapComponent());
    }
}
