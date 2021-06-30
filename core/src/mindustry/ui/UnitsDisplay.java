package mindustry.ui;

import arc.scene.ui.layout.*;
import mindustry.game.Teams;
import mindustry.type.*;

import static mindustry.Vars.*;

public class UnitsDisplay extends Table {

    private Teams.TeamData data;

    public UnitsDisplay() {
        build();
    }

    public void resetUsed(){
        background(null);
    }

    void build() {
        clear();

        update(() -> {
            data = player.team().data();
            build();
        });

        if (data == null) return;

        int count = 0;
        for (UnitType unitType : content.units()) {
            int num = data.countType(unitType);
            if (num > 0) {
                image(unitType.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(unitType.localizedName).style(Styles.outlineLabel));
                label(() -> String.valueOf(num)).padRight(3).minWidth(25f).left();
                if (++count % 5 == 0) row();
            }
        }
        if(count > 0){
            background(Styles.black6);
            margin(4);
        }
    }
}
