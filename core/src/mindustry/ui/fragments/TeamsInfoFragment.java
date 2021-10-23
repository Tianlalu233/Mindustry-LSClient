package mindustry.ui.fragments;

import arc.Core;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.Teams.TeamData;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class TeamsInfoFragment extends Fragment {

    private Seq<TeamData> teamData;

    @Override
    public void build(Group parent) {
        parent.fill(table -> {
            table.center().left();
            table.table(t -> {
                t.background(Styles.black6).visible(() -> ui.hudfrag.shown && Core.settings.getBool("showallteamstats"));

                Runnable rebuild = () -> {
                    t.clearChildren();

                    for (Item item : content.items()) {
                        Table rowTable = new Table();
                        rowTable.add(new Image(item.uiIcon)).size(15).pad(3);
                        boolean hasATeam = false;
                        for (TeamData data : teamData) {
                            if (data.team.equals(Team.derelict)) continue;
                            int num;
                            if (data.core() == null ) num = 0;
                            else num = data.core().items.get(item);
                            rowTable.add(UI.formatAmount(num)).color(data.team.color).size(15).pad(3).padRight(35);
                            if (num > 0) {
                                hasATeam = true;
                            }
                        }
                        if (hasATeam) {
                            t.add(rowTable).row();
                        }
                    }

                    for (UnitType unitType : content.units()) {
                        Table rowTable = new Table();
                        rowTable.add(new Image(unitType.uiIcon)).size(15).pad(3);
                        boolean hasATeam = false;
                        for (TeamData data : teamData) {
                            if (data.team.equals(Team.derelict)) continue;
                            int num = data.countType(unitType);
                            rowTable.add(UI.formatAmount(num)).color(data.team.color).size(15).pad(3).padRight(35);
                            if (num > 0) {
                                hasATeam = true;
                            }
                        }
                        if (hasATeam) {
                            t.add(rowTable).row();
                        }

                    }
                };

                t.update(() -> {
                    teamData = state.teams.getActive();
                    rebuild.run();
                });

                if (state.teams != null && state.teams.getActive() != null) {
                    teamData = state.teams.getActive();
                    rebuild.run();
                }

            });

        });
    }



}
