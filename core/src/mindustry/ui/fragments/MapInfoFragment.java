package mindustry.ui.fragments;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.Image;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.UI;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.input.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapInfoFragment extends Fragment{

    private boolean shown;
    private Element background;
    private int wave;
    private GameState currState;

    @Override
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
            t.add(info).growY();
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
        if (state.hasSpawns()) {
            info.add(buildWavesInfoTable());
        }
    }

    private Table buildMapAttrsTable() {
        Table mapAttrs = new Table();
        Table table1 = new Table();
        Table table2 = new Table();
        addMapAttrs(table1, "reactorexplosions", currState.rules.reactorExplosions);
        addMapAttrs(table1, "schematic", currState.rules.schematicsAllowed);
        addMapAttrs(table1, "explosions", currState.rules.damageExplosions);
        addMapAttrs(table1, "fire", currState.rules.fire);
        addMapAttrs(table1, "unitammo", currState.rules.unitAmmo);
        addMapAttrs(table1, "coreincinerates", currState.rules.coreIncinerates);
        addMapAttrs(table2, "unitbuildspeedmultiplier", currState.rules.unitBuildSpeedMultiplier);
        addMapAttrs(table2, "unitdamagemultiplier", currState.rules.unitDamageMultiplier);
        addMapAttrs(table2, "blockhealthmultiplier", currState.rules.blockHealthMultiplier);
        addMapAttrs(table2, "blockdamagemultiplier", currState.rules.blockDamageMultiplier);
        addMapAttrs(table2, "buildcostmultiplier", currState.rules.buildCostMultiplier);
        addMapAttrs(table2, "buildspeedmultiplier", currState.rules.buildSpeedMultiplier);
        addMapAttrs(table2, "deconstructrefundmultiplier", currState.rules.deconstructRefundMultiplier);

        mapAttrs.add(table1).pad(10);
        mapAttrs.add(table2).pad(10);
        return mapAttrs;
    }

    private void addMapAttrs(Table t, String name, boolean open) {
        t.add(Core.bundle.get("rules." + name)).pad(5);
        t.add(new Image(open ? Icon.ok : Icon.cancel)).pad(5).row();
    }

    private void addMapAttrs(Table t, String name, float num) {
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
        labels.add().size(50).pad(5).padLeft(10).padRight(10).row();
        labels.add(Core.bundle.get("number")).pad(5).padLeft(10).padRight(10).row();
        labels.add(Core.bundle.get("shield")).pad(5).padLeft(10).padRight(10).row();
        t.add(labels);
        Table enemyTable = new Table();
        t.pane(Styles.horizontalPane, enemyTable).maxWidth(1000f).get().setScrollingDisabled(false, true);
        for(SpawnGroup group : currState.rules.spawns) {
            if (group.getSpawned(wave - 1) == 0) continue;
            Color color = group.effect == StatusEffects.boss ? Pal.health : Color.white;
            Table enemy = new Table();
            enemy.add(new Image(group.type.uiIcon)).size(50).pad(5).padLeft(10).padRight(10).tooltip(o -> o.background(Styles.black6).margin(4f).add(group.type.localizedName).style(Styles.outlineLabel));
            enemy.row();
            enemy.add(String.valueOf(group.getSpawned(wave - 1) * spawner.countSpawns())).color(color).pad(5).padLeft(10).padRight(10).row();
            enemy.add(String.valueOf(UI.formatFloat(group.getShield(wave)))).color(color).pad(5).padLeft(10).padRight(10).row();
            enemyTable.add(enemy);
        }
    }
}
