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
                if (gc.consumes.has(ConsumeType.item)) {
                    for (ItemStack stack : gc.consumes.getItem().items) {
                       Item item = stack.item;
                       items.put(item, items.get(item, 0) - stack.amount * 60f / gc.craftTime);
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
                if (s.consumes.has(ConsumeType.item)) {
                    for (ItemStack stack : s.consumes.getItem().items) {
                        Item item = stack.item;
                        items.put(item, items.get(item, 0) - stack.amount * 60f / s.craftTime);
                    }
                }
            }
            else if (t.block instanceof Fracker f) {
                for (ItemStack stack : f.consumes.getItem().items) {
                    Item item = stack.item;
                    items.put(item, items.get(item, 0) - stack.amount * f.itemUseTime / 60f);
                }
            }
            else if (t.block instanceof PowerGenerator) {
                if (t.block instanceof ItemLiquidGenerator ilg) {
                    if (t.block instanceof SingleTypeGenerator stg) {
                        ItemStack itemStack = stg.consumes.getItem().items[0];
                        items.put(itemStack.item, items.get(itemStack.item, 0) - itemStack.amount / stg.itemDuration * 60f);
                    }
                    else {
                        ConsumeItemFilter consumeItemFilter = ilg.consumes.get(ConsumeType.item);
                        for (Item item : content.items()) {
                            if (consumeItemFilter.filter.get(item)) {
                                items.put(item, items.get(item, 0) - 60f / ilg.itemDuration);
                                break;
                            }
                        }
                    }
                }
                else if (t.block instanceof NuclearReactor nr) {
                    ItemStack itemStack = nr.consumes.getItem().items[0];
                    items.put(itemStack.item, items.get(itemStack.item, 0) - itemStack.amount / nr.itemDuration * 60f);
                }
                else if (t.block instanceof ImpactReactor ir) {
                    ItemStack itemStack = ir.consumes.getItem().items[0];
                    items.put(itemStack.item, items.get(itemStack.item, 0) - itemStack.amount / ir.itemDuration * 60f);
                }
            }
        });
        return items;
    }

    public ObjectFloatMap<Liquid> liquidIO() {
        ObjectFloatMap<Liquid> liquids = new ObjectFloatMap<>(content.liquids().size << 1);
        tiles.each(t -> {
            if (t.block instanceof GenericCrafter gc) {
                if (gc.consumes.has(ConsumeType.liquid)) {
                    ConsumeLiquid consumeLiquid = gc.consumes.getLiquid();
                    Liquid liquid = consumeLiquid.liquid;
                    liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                }

                if (gc.outputLiquid != null) {
                    LiquidStack stack = gc.outputLiquid;
                    if (gc instanceof LiquidConverter) liquids.put(stack.liquid, liquids.get(stack.liquid, 0) + stack.amount * 60f);
                    else liquids.put(stack.liquid, liquids.get(stack.liquid, 0) + stack.amount * 60f / gc.craftTime);
                }
            }
            else if (t.block instanceof Separator s) {
                ConsumeLiquid consumeLiquid = s.consumes.getLiquid();
                Liquid liquid = consumeLiquid.liquid;
                liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
            }
            else if (t.block instanceof SolidPump sp) {
                if (sp.consumes.has(ConsumeType.liquid)) {
                    ConsumeLiquid consumeLiquid = sp.consumes.getLiquid();
                    Liquid liquid = consumeLiquid.liquid;
                    liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                }
                liquids.put(sp.result, liquids.get(sp.result, 0) + sp.pumpAmount * 60f);
            }
            else if (t.block instanceof PowerGenerator) {
                if (t.block instanceof ItemLiquidGenerator ilg && ilg.hasLiquids) {
                    ConsumeLiquid consumeLiquid = ilg.consumes.getLiquid();
                    Liquid liquid = consumeLiquid.liquid;
                    liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                }
                else if (t.block instanceof NuclearReactor nr) {
                    ConsumeLiquid consumeLiquid = nr.consumes.getLiquid();
                    Liquid liquid = consumeLiquid.liquid;
                    liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
                }
                else if (t.block instanceof ImpactReactor ir) {
                    ConsumeLiquid consumeLiquid = ir.consumes.getLiquid();
                    Liquid liquid = consumeLiquid.liquid;
                    liquids.put(liquid, liquids.get(liquid, 0) - consumeLiquid.amount * 60f);
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
