package mindustry.ui.fragments;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.input.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapInfoFragment {

    private boolean shown;
    private Element background;
    private int wave;
    private GameState currState;

    public void build(Group parent) {
        Table info = new Table();
        currState = state;
        background = parent.fill((x, y, w, h) -> {
            w = Core.graphics.getWidth();
            h = Core.graphics.getHeight();
            Draw.color(Color.black);
            Fill.crect(0, 0, w, h);
        }).update(() -> {
            background.setFillParent(true);
            background.setBounds(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
            if(Core.input.keyTap(Binding.menu)){
                shown = false;
            }
        }).visible(() -> shown);

        parent.fill(t -> {
            t.setFillParent(true);
            t.visible(() -> shown);
            t.update(() -> {
                t.setBounds(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
                if (!state.equals(currState)) {
                    currState = state;
                    wave = currState.wave;
                    buildInfoTable(info);
                }
            });

            t.add("@mapinfo").style(Styles.outlineLabel).pad(10f);
            t.row();
            t.pane(info).growY();
            t.row();
            t.button("@back", Icon.leftOpen, this::hide).size(220f, 60f).pad(10f);
        });
    }

    public void show() {
        shown = true;
    }

    public void hide() {
        shown = false;
    }

    public boolean isShown() {
        return shown;
    }

    private void buildInfoTable(Table info) {
        info.clear();
        info.add(buildMapAttrsTable()).padBottom(10).row();
        info.add(buildTeamAttrsTable()).padBottom(10).row();
        if (state.hasSpawns()) {
            info.add(buildWavesInfoTable());
        }
    }

    private Table buildMapAttrsTable() {
        Table mapAttrs = new Table();
        mapAttrs.add(buildGlobalAttrsTable()).row();
        return mapAttrs;
    }

    private Table buildGlobalAttrsTable() {
        Table global = new Table();
        Table table1 = new Table();
        Table table2 = new Table();
        addMapAttr(table1, "reactorexplosions", currState.rules.reactorExplosions);
        addMapAttr(table1, "schematic", currState.rules.schematicsAllowed);
        addMapAttr(table1, "explosions", currState.rules.damageExplosions);
        addMapAttr(table1, "fire", currState.rules.fire);
        addMapAttr(table1, "unitammo", currState.rules.unitAmmo);
        addMapAttr(table1, "coreincinerates", currState.rules.coreIncinerates);
        addMapAttr(table2, "unitbuildspeedmultiplier", currState.rules.unitBuildSpeedMultiplier);
        addMapAttr(table2, "unitdamagemultiplier", currState.rules.unitDamageMultiplier);
        addMapAttr(table2, "blockhealthmultiplier", currState.rules.blockHealthMultiplier);
        addMapAttr(table2, "blockdamagemultiplier", currState.rules.blockDamageMultiplier);
        addMapAttr(table2, "buildspeedmultiplier", currState.rules.buildSpeedMultiplier);
        addMapAttr(table2, "buildcostmultiplier", currState.rules.buildCostMultiplier);
        addMapAttr(table2, "deconstructrefundmultiplier", currState.rules.deconstructRefundMultiplier);
        global.add(table1).pad(10);
        global.add(table2).pad(10);
        return global;
    }

    private Table buildTeamAttrsTable() {
        Table teamTable = new Table();

        Table header = new Table();
        header.add(Core.bundle.get("rules." + "infiniteresources")).pad(5).row();
        header.add(Core.bundle.get("rules." + "cheat")).pad(5).row();
        header.add(Core.bundle.get("rules." + "unitbuildspeedmultiplier")).pad(5).row();
        header.add(Core.bundle.get("rules." + "unitdamagemultiplier")).pad(5).row();
        header.add(Core.bundle.get("rules." + "blockhealthmultiplier")).pad(5).row();
        header.add(Core.bundle.get("rules." + "blockdamagemultiplier")).pad(5).row();
        header.add(Core.bundle.get("rules." + "buildspeedmultiplier")).pad(5).row();
        header.marginRight(20);

        teamTable.add(header);
        Rules rules = currState.rules;
        int count = 0;
        for (Team team: Team.baseTeams) {
            Table tt = new Table();
            tt.add(new Image(rules.teams.get(team).infiniteResources ? Icon.ok : Icon.cancel)).pad(5).row();
            tt.add(new Image(rules.teams.get(team).cheat ? Icon.ok : Icon.cancel)).pad(5).row();
            tt.add(String.valueOf(rules.unitBuildSpeed(team))).color(team.color).pad(5).row();
            tt.add(String.valueOf(rules.unitDamage(team))).color(team.color).pad(5).row();
            tt.add(String.valueOf(rules.blockHealth(team))).color(team.color).pad(5).row();
            tt.add(String.valueOf(rules.blockDamage(team))).color(team.color).pad(5).row();
            tt.add(String.valueOf(rules.buildSpeed(team))).color(team.color).pad(5).row();
            tt.marginRight(10);
            teamTable.add(tt);
            count++;
        }

        return count > 0 ? teamTable : new Table();
    }

    private void addMapAttr(Table t, String name, boolean open) {
        t.add(Core.bundle.get("rules." + name)).pad(5);
        t.add(new Image(open ? Icon.ok : Icon.cancel)).pad(5).row();
    }

    private void addMapAttr(Table t, String name, float num) {
        t.add(Core.bundle.get("rules." + name)).pad(5);
        t.add(String.valueOf(num)).pad(5).row();
    }

    private Table buildWavesInfoTable() {
        Table wavesInfo = new Table();

        Table waveNums = new Table();
        waveNumDisplay(waveNums, wave);
        wavesInfo.update(() -> waveNumDisplay(waveNums, wave));

        Table control = new Table();
        Table enemies = new Table();
        control.button("-10", () -> backWave(enemies, 10)).size(60).pad(5);
        control.button("-1", () -> backWave(enemies, 1)).size(60).pad(5);
        control.button(Icon.home, () -> currWave(enemies)).size(60).pad(5);
        control.button("+1", () -> forwardWave(enemies, 1)).size(60).pad(5);
        control.button("+10", () -> forwardWave(enemies, 10)).size(60).pad(5);
        buildEnemiesTable(enemies, wave);

        wavesInfo.add(waveNums).pad(5).row();
        wavesInfo.add(control).pad(5).row();
        wavesInfo.add(enemies).pad(5);
        return wavesInfo;
    }

    private void waveNumDisplay(Table waveNums, int num) {
        waveNums.clear();
        waveNums.add(Core.bundle.format("wave", num)).pad(5).row();
    }

    private void backWave(Table enemies, int step) {
        wave = Math.max(1, wave - step);
        buildEnemiesTable(enemies, wave);
    }

    private void forwardWave(Table enemies, int step) {
        wave += step;
        buildEnemiesTable(enemies, wave);
    }

    private void currWave(Table enemies) {
        wave = currState.wave;
        buildEnemiesTable(enemies, wave);
    }

    private void buildEnemiesTable(Table t, int wave) {
        t.clear();
        Table labels = new Table();
        labels.add().size(50).pad(5, 10, 5, 10).row();
        labels.add(Core.bundle.get("number")).height(20).pad(5, 10, 5, 10).row();
        labels.add(Core.bundle.get("shield")).height(20).pad(5, 10, 5, 10).row();
        labels.add(Core.bundle.get("statuseffect")).height(30).pad(5, 10, 5, 10).row();
        t.add(labels);
        Table enemyTable = new Table();
        t.pane(Styles.horizontalPane, enemyTable).maxWidth(1000f).get().setScrollingDisabled(false, true);
        for(SpawnGroup group : currState.rules.spawns) {
            if (group.getSpawned(wave - 1) == 0) continue;
            Color color = group.effect == StatusEffects.boss ? Pal.health : Color.white;
            Table enemy = new Table();
            enemy.add(new Image(group.type.uiIcon)).size(50).pad(5, 10, 5, 10).tooltip(o -> o.background(Styles.black6).margin(4f).add(group.type.localizedName).style(Styles.outlineLabel)).row();
            enemy.add(String.valueOf(group.getSpawned(wave - 1) * spawner.countSpawns())).color(color).height(20).pad(5, 10, 5, 10).row();
            enemy.add(String.valueOf(UI.formatFloat(group.getShield(wave)))).color(color).height(20).pad(5, 10, 5, 10).row();
            if (group.effect != null && group.effect != StatusEffects.none) {
                enemy.add(new Image(group.effect.uiIcon)).size(30).pad(5, 10, 5, 10).tooltip(o -> o.background(Styles.black6).margin(4f).add(group.effect.localizedName).style(Styles.outlineLabel)).row();
            }
            else {
                enemy.add().size(30).pad(5, 10, 5, 10).row();
            }
            enemyTable.add(enemy);
        }
    }
}
