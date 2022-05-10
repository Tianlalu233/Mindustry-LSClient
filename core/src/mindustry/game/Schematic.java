package mindustry.game;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

public class Schematic implements Publishable, Comparable<Schematic>{
    public final Seq<Stile> tiles;
    /** These are used for the schematic tag UI. */
    public Seq<String> labels = new Seq<>();
    /** Internal meta tags. */
    public StringMap tags;
    public int width, height;
    public @Nullable Fi file;
    /** Associated mod. If null, no mod is associated with this schematic. */
    public @Nullable LoadedMod mod;

    public Schematic(Seq<Stile> tiles, StringMap tags, int width, int height){
        this.tiles = tiles;
        this.tags = tags;
        this.width = width;
        this.height = height;
    }

    public float powerProduction(){
        return tiles.sumf(s -> s.block instanceof PowerGenerator p ? p.powerProduction : 0f);
    }

    public float powerConsumption(){
        return tiles.sumf(s -> s.block.consPower != null ? s.block.consPower.usage : 0f);
    }

    public ItemSeq requirements(){
        ItemSeq requirements = new ItemSeq();

        tiles.each(t -> {
            for(ItemStack stack : t.block.requirements){
                requirements.add(stack.item, stack.amount);
            }
        });

        return requirements;
    }

    public ObjectFloatMap<Item> itemIO() {
        ObjectFloatMap<Item> items = new ObjectFloatMap<>(content.items().size << 1);
        tiles.each(t -> {
            if (t.block instanceof GenericCrafter gc) {
                for (Consume consume : gc.consumers) {
                    if (consume instanceof ConsumeItems ci) {
                        for (ItemStack stack : ci.items) {
                           Item item = stack.item;
                           items.put(item, items.get(item, 0) - stack.amount * 60f / gc.craftTime);
                        }
                    }
                }
                if (gc.outputItems != null) {
                    for (ItemStack stack : gc.outputItems) {
                        Item item = stack.item;
                        items.put(item, items.get(item, 0) + stack.amount * 60f / gc.craftTime);
                    }
                }
            }
            else if (t.block instanceof Separator s) {
                for (Consume consume : s.consumers) {
                    if (consume instanceof ConsumeItems ci) {
                        for (ItemStack stack : ci.items) {
                            Item item = stack.item;
                            items.put(item, items.get(item, 0) - stack.amount * 60f / s.craftTime);
                        }
                    }
                }
                // TODO output
            }
            else if (t.block instanceof Fracker f) {
                for (Consume consume : f.consumers) {
                    if (consume instanceof ConsumeItems ci) {
                        for (ItemStack stack : ci.items) {
                            Item item = stack.item;
                            items.put(item, items.get(item, 0) - stack.amount * f.itemUseTime / 60f);
                        }
                    }
                }
            }
            else if (t.block instanceof PowerGenerator) {
                if (t.block instanceof ConsumeGenerator cg) {
                    for (Consume consume : cg.consumers) {
                        if (consume instanceof ConsumeItems ci) {
                            for (ItemStack stack : ci.items) {
                                Item item = stack.item;
                                items.put(item, items.get(item, 0) - stack.amount / cg.itemDuration * 60f);
                            }
                        }
                        else if (consume instanceof ConsumeItemFilter cif) {
                            for (Item item : content.items()) {
                                if (cif.filter.get(item)) {
                                    items.put(item, items.get(item, 0) - 60f / cg.itemDuration);
                                    break;
                                }
                            }
                        }
                    }
                }
                else if (t.block instanceof NuclearReactor nr) {
                    for (Consume consume : nr.consumers) {
                        if (consume instanceof ConsumeItems ci) {
                            ItemStack itemStack = ci.items[0];
                            items.put(itemStack.item, items.get(itemStack.item, 0) - itemStack.amount / nr.itemDuration * 60f);
                        }
                    }
                }
                else if (t.block instanceof ImpactReactor ir) {
                    for (Consume consume : ir.consumers) {
                        if (consume instanceof ConsumeItems ci) {
                            ItemStack itemStack = ci.items[0];
                            items.put(itemStack.item, items.get(itemStack.item, 0) - itemStack.amount / ir.itemDuration * 60f);
                        }
                    }
                }
            }
        });
        return items;
    }

    public ObjectFloatMap<Liquid> liquidIO() {
        ObjectFloatMap<Liquid> liquids = new ObjectFloatMap<>(content.liquids().size << 1);
        tiles.each(t -> {
            if (t.block instanceof GenericCrafter gc) {
                for (Consume consume : gc.consumers) {
                    if (consume instanceof ConsumeLiquid cl) { // TODO may replace with ConsumeLiquids
                        Liquid liquid = cl.liquid;
                        liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                    }
                }

                if (gc.outputLiquid != null) {
                    for (LiquidStack stack : gc.outputLiquids) {
                        liquids.put(stack.liquid, liquids.get(stack.liquid, 0) + stack.amount * 60f);
                    }
                }
            }
            else if (t.block instanceof Separator s) {
                for (Consume consume : s.consumers) {
                    if (consume instanceof ConsumeLiquid cl) {
                        Liquid liquid = cl.liquid;
                        liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                    }
                }
            }
            else if (t.block instanceof Pump p) {
                for (Consume consume : p.consumers) {
                    if (consume instanceof ConsumeLiquid cl) {
                        Liquid liquid = cl.liquid;
                        liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                    }
                }
                if (p instanceof SolidPump sp) {
                    liquids.put(sp.result, liquids.get(sp.result, 0) + sp.pumpAmount * 60f);
                }
            }
            else if (t.block instanceof PowerGenerator pg) {
                if (pg instanceof ConsumeGenerator cg) {
                    for (Consume consume : cg.consumers) {
                        if (consume instanceof ConsumeLiquid cl) {
                            Liquid liquid = cl.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                        }
                        else if (consume instanceof ConsumeLiquids cls) {
                            for (LiquidStack liquidStack : cls.liquids) {
                                liquids.put(liquidStack.liquid, liquids.get(liquidStack.liquid, 0) - liquidStack.amount * 60f);
                            }
                        }
                    }
                }
                else if (pg instanceof ThermalGenerator tg) {
                    for (Consume consume : tg.consumers) {
                        if (consume instanceof ConsumeLiquid cl) {
                            Liquid liquid = cl.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                        }
                    }
                    LiquidStack liquidStack = tg.outputLiquid;
                    liquids.put(liquidStack.liquid, liquids.get(liquidStack.liquid, 0) + liquidStack.amount * 60f);
                }
                else if (pg instanceof NuclearReactor nr) {
                    for (Consume consume : nr.consumers) {
                        if (consume instanceof ConsumeLiquid cl) {
                            Liquid liquid = cl.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                        }
                    }
                }
                else if (pg instanceof ImpactReactor ir) {
                    for (Consume consume : ir.consumers) {
                        if (consume instanceof ConsumeLiquid cl) {
                            Liquid liquid = cl.liquid;
                            liquids.put(liquid, liquids.get(liquid, 0) - cl.amount * 60f);
                        }
                    }
                }
            }
        });
        return liquids;
    }

    public boolean hasCore(){
        return tiles.contains(s -> s.block instanceof CoreBlock);
    }

    public CoreBlock findCore(){
        Stile tile = tiles.find(s -> s.block instanceof CoreBlock);
        if(tile == null) throw new IllegalArgumentException("Schematic is missing a core!");
        return (CoreBlock)tile.block;
    }

    public String name(){
        return tags.get("name", "unknown");
    }

    public String description(){
        return tags.get("description", "");
    }

    public void save(){
        schematics.saveChanges(this);
    }

    @Override
    public String getSteamID(){
        return tags.get("steamid");
    }

    @Override
    public void addSteamID(String id){
        tags.put("steamid", id);
        save();
    }

    @Override
    public void removeSteamID(){
        tags.remove("steamid");
        save();
    }

    @Override
    public String steamTitle(){
        return name();
    }

    @Override
    public String steamDescription(){
        return description();
    }

    @Override
    public String steamTag(){
        return "schematic";
    }

    @Override
    public Fi createSteamFolder(String id){
        Fi directory = tmpDirectory.child("schematic_" + id).child("schematic." + schematicExtension);
        file.copyTo(directory);
        return directory;
    }

    @Override
    public Fi createSteamPreview(String id){
        Fi preview = tmpDirectory.child("schematic_preview_" + id + ".png");
        schematics.savePreview(this, preview);
        return preview;
    }

    @Override
    public int compareTo(Schematic schematic){
        return name().compareTo(schematic.name());
    }

    public static class Stile{
        public Block block;
        public short x, y;
        public Object config;
        public byte rotation;

        public Stile(Block block, int x, int y, Object config, byte rotation){
            this.block = block;
            this.x = (short)x;
            this.y = (short)y;
            this.config = config;
            this.rotation = rotation;
        }

        //pooling only
        public Stile(){
            block = Blocks.air;
        }

        public Stile set(Stile other){
            block = other.block;
            x = other.x;
            y = other.y;
            config = other.config;
            rotation = other.rotation;
            return this;
        }

        public Stile copy(){
            return new Stile(block, x, y, config, rotation);
        }
    }
}
