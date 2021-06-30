package mindustry.ui;

import arc.Core;
import arc.scene.ui.layout.*;

import static mindustry.Vars.*;

public class TopStatsDisplay extends Table {

    private CoreItemsDisplay coreItems = new CoreItemsDisplay();
    private UnitsDisplay units = new UnitsDisplay();
    private boolean prevCore;
    private boolean prevUnit;
    private boolean shown;

    public TopStatsDisplay() {
        shown = true;
        build();
    }

    public void resetUsed() {
        coreItems.resetUsed();
        coreItems.clear();
        units.resetUsed();
        units.clear();
    }

    void build() {
        clear();

        update(() -> {
           if (prevCore != Core.settings.getBool("coreitems") || prevUnit != Core.settings.getBool("unitstat")) {
               build();
           }
        });

        if (prevCore = Core.settings.getBool("coreitems")) {
            collapser(coreItems, () -> !mobile && shown).fillX().top();
        }
        if (prevUnit = Core.settings.getBool("unitstat")) {
            collapser(units, () -> !mobile && shown).fillX().top();
        }

    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }
}
