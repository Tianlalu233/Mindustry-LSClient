package mindustry.ui.fragments;

import arc.Core;
import arc.scene.Group;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

public class QuickSettingFragment {

    public void build(Group parent) {
        parent.fill(table -> {
            table.center().right();
            table.table(t -> {
                t.background(Styles.none).visible(() -> ui.hudfrag.shown && Core.settings.getBool("showquicksetting"));
                t.button(Icon.settingsSmall, () -> {
                    ui.settings.showAdavance();
                }).size(40);
                t.button(Icon.terrainSmall, ui.mapInfofrag::show).size(40);
            });

        });
    }
}
