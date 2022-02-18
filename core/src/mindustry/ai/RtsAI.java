package mindustry.ai;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

public class RtsAI{
    static final Seq<Building> targets = new Seq<>();
    static final Seq<Unit> squad = new Seq<>(false);
    static final IntSet used = new IntSet();
    static final IntSet assignedTargets = new IntSet();
    static final float squadRadius = 120f;
    static final int timeUpdate = 0;
    static final float minWeight = 0.9f;

    //in order of priority??
    static final BlockFlag[] flags = {BlockFlag.generator, BlockFlag.factory, BlockFlag.core, BlockFlag.battery};
    static final ObjectFloatMap<Building> weights = new ObjectFloatMap<>();
    static final int minSquadSize = 4;
    //TODO max squad size
    static final boolean debug = true;

    final Interval timer = new Interval(10);
    final TeamData data;
    final ObjectSet<Building> damagedSet = new ObjectSet<>();
    final Seq<Building> damaged = new Seq<>(false);

    //must be static, as this class can get instantiated many times; event listeners are hard to clean up
    static{
        Events.on(BuildDamageEvent.class, e -> {
            if(e.build.team.rules().rtsAi){
                var ai = e.build.team.data().rtsAi;
                if(ai != null){
                    ai.damagedSet.add(e.build);
                }
            }
        });
    }

    public RtsAI(TeamData data){
        this.data = data;
        timer.reset(0, Mathf.random(60f * 2f));

        //TODO remove: debugging!

        if(debug){
            Events.run(Trigger.draw, () -> {

                Draw.draw(Layer.overlayUI, () -> {

                    float s = Fonts.outline.getScaleX();
                    Fonts.outline.getData().setScale(0.5f);
                    for(var target : weights){
                        Fonts.outline.draw("[sky]" + Strings.fixed(target.value, 2), target.key.x, target.key.y, Align.center);
                    }
                    Fonts.outline.getData().setScale(s);
                });

            });
        }
    }

    public void update(){
        if(timer.get(timeUpdate, 60f * 2f)){
            assignSquads();
        }
    }

    void assignSquads(){
        assignedTargets.clear();
        used.clear();
        damaged.addAll(damagedSet);
        damagedSet.clear();

        boolean didDefend = false;

        for(var unit : data.units){
            if(unit.isCommandable() && !unit.command().hasCommand() && used.add(unit.id)){
                squad.clear();
                data.tree().intersect(unit.x - squadRadius/2f, unit.y - squadRadius/2f, squadRadius, squadRadius, squad);
                //remove overlapping squads
                squad.removeAll(u -> (u != unit && used.contains(u.id)) || !u.isCommandable() || u.command().hasCommand());
                //mark used so other squads can't steal them
                for(var item : squad){
                    used.add(item.id);
                }

                //TODO flawed, squads
                if(handleSquad(squad, !didDefend)){
                    didDefend = true;
                }
            }
        }

        damaged.clear();
    }

    boolean handleSquad(Seq<Unit> units, boolean noDefenders){
        float health = 0f, dps = 0f;
        float ax = 0f, ay = 0f;

        for(var unit : units){
            ax += unit.x;
            ay += unit.y;
            health += unit.health;
            dps += unit.type.dpsEstimate;
        }
        ax /= units.size;
        ay /= units.size;

        if(debug){
            Vars.ui.showLabel("Squad: " + units.size, 2f, ax, ay);
        }

        Building defend = null;

        //there is something to defend, see if it's worth the time
        if(damaged.size > 0){
            //TODO do the weights matter at all?
            //for(var build : damaged){
                //float w = estimateStats(ax, ay, dps, health);
                //weights.put(build, w);
            //}

            //screw you java
            float aax = ax, aay = ay;

            Building best = damaged.min(b -> {
                //rush to core IMMEDIATELY
                if(b instanceof CoreBuild){
                    return -999999f;
                }

                return b.dst(aax, aay);
            });

            //defend when close, or this is the only squad defending
            if(best instanceof CoreBuild || (units.size >= minSquadSize && (noDefenders || best.within(ax, ay, 400f)))){
                defend = best;

                if(debug){
                    Vars.ui.showLabel("Defend, dst = " + (int)(best.dst(ax, ay)), 8f, best.x, best.y);
                }
            }
        }

        //find aggressor, or else, the thing being attacked
        Vec2 defendPos = null;
        Teamc defendTarget = null;
        if(defend != null){
            //TODO could be made faster by storing bullet shooter
            Unit aggressor = Units.closestEnemy(data.team, defend.x, defend.y, 250f, u -> true);
            if(aggressor != null){
                defendTarget = aggressor;
            }else if(false){ //TODO currently ignored, no use defending against nothing
                //should it even go there if there's no aggressor found?
                Tile closest = defend.findClosestEdge(units.first(), Tile::solid);
                if(closest != null){
                    defendPos = new Vec2(closest.worldx(), closest.worldy());
                }
            }
        }

        boolean anyDefend = defendPos != null || defendTarget != null;

        var build = anyDefend ? null : findTarget(ax, ay, units.size, dps, health);

        if(build != null || anyDefend){
            for(var unit : units){
                if(unit.isCommandable() && !unit.command().hasCommand()){
                    if(defendPos != null){
                        unit.command().commandPosition(defendPos);
                    }else{
                        unit.command().commandTarget(defendTarget == null ? build : defendTarget);
                    }
                }
            }
        }

        return anyDefend;
    }

    @Nullable Building findTarget(float x, float y, int total, float dps, float health){
        if(total < minSquadSize) return null;

        //flag priority?
        //1. generator
        //2. factory
        //3. core
        targets.clear();
        for(var flag : flags){
            targets.addAll(Vars.indexer.getEnemy(data.team, flag));
        }
        targets.removeAll(b -> assignedTargets.contains(b.id));

        if(targets.size == 0) return null;

        weights.clear();

        for(var target : targets){
            weights.put(target, estimateStats(target.x, target.y, dps, health));
        }

        var result = targets.min(
            Structs.comps(
                //weight is most important?
                Structs.comparingFloat(b -> (1f - weights.get(b, 0f)) + b.dst(x, y)/10000f),
                //then distance TODO why weight above
                Structs.comparingFloat(b -> b.dst2(x, y))
            )
        );

        float weight = weights.get(result, 0f);
        if(weight < minWeight && total < Units.getCap(data.team)){
            return null;
        }

        assignedTargets.add(result.id);
        return result;
    }

    float estimateStats(float x, float y, float selfDps, float selfHealth){
        float[] health = {0f}, dps = {0f};
        float extraRadius = 15f;

        //TODO this does not take into account the path to this object
        for(var turret : Vars.indexer.getEnemy(data.team, BlockFlag.turret)){
            if(turret.within(x, y, ((TurretBuild)turret).range() + extraRadius)){
                health[0] += turret.health;
                dps[0] += ((TurretBuild)turret).estimateDps();
            }
        }

        //add on extra radius, assume unit range is below that...?
        Units.nearbyEnemies(data.team, x, y, extraRadius + 140f, other -> {
            if(other.within(x, y, other.range() + extraRadius)){
                health[0] += other.health;
                dps[0] += other.type.dpsEstimate;
            }
        });

        float hp = health[0], dp = dps[0];

        float timeDestroyOther = Mathf.zero(selfDps, 0.001f) ? Float.POSITIVE_INFINITY : hp / selfDps;
        float timeDestroySelf = Mathf.zero(dp) ? Float.POSITIVE_INFINITY : selfHealth / dp;

        //other can never be destroyed | other destroys self instantly
        if(Float.isInfinite(timeDestroyOther) | Mathf.zero(timeDestroySelf)) return 0f;
        //self can never be destroyed | self destroys other instantly
        if(Float.isInfinite(timeDestroySelf) | Mathf.zero(timeDestroyOther)) return 1f;

        //examples:
        // self 10 sec / other 10 sec -> can destroy target with 100 % losses -> returns 1
        // self 5 sec / other 10 sec -> can destroy about half of other -> returns 0.5 (needs to be 2x stronger to defeat)
        return timeDestroySelf / timeDestroyOther;
    }
}