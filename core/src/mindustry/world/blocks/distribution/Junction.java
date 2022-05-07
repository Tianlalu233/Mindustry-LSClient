package mindustry.world.blocks.distribution;

import arc.Core;
import arc.math.geom.*;
import arc.graphics.g2d.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Junction extends Block{
    public float speed = 26; //frames taken to go through this junction
    public int capacity = 6;

    public Junction(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        group = BlockGroup.transportation;
        unloadable = false;
        noUpdateDisabled = true;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class JunctionBuild extends Building{
        public DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity);

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public void updateTile(){

            for(int i = 0; i < 4; i++){
                if(buffer.indexes[i] > 0){
                    if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                    long l = buffer.buffers[i][0];
                    float time = BufferItem.time(l);

                    if(Time.time >= time + speed / timeScale || Time.time < time){

                        Item item = content.item(BufferItem.item(l));
                        Building dest = nearby(i);

                        //skip blocks that don't want the item, keep waiting until they do
                        if(item == null || dest == null || !dest.acceptItem(this, item) || dest.team != team){
                            continue;
                        }

                        dest.handleItem(this, item);
                        System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                        buffer.indexes[i] --;
                    }
                }
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            int relative = source.relativeTo(tile);
            buffer.accept(relative, item);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int relative = source.relativeTo(tile);

            if(relative == -1 || !buffer.accepts(relative)) return false;
            Building to = nearby(relative);
            return to != null && to.team == team;
        }

        @Override
        public void draw(){
            super.draw();
            if (!Core.settings.getBool("showiteminjb")) return;
            float width = region.width / (float)tilesize, height = region.height / (float)tilesize, iconSize = 3f;;
            float widthOffset = width / 1.5f, heightOffset = height / 1.5f;
            float timeSpent = speed / timeScale;
            for(int i = 0; i < 4; i++){
                if(buffer.indexes[i] > 0){
                    float offsetX = x + widthOffset * Geometry.d8edge(i).x, offsetY = y + heightOffset * Geometry.d8edge(i).y;
                    for (int j = 0; j < buffer.indexes[i]; j ++) {
                        long l = buffer.buffers[i][j];
                        float time = BufferItem.time(l);
                        float ratio = Math.max(j / (float)capacity, Time.time < time ? 0 : 1 - Math.min(1, (Time.time - time) / timeSpent));
                        Item item = content.item(BufferItem.item(l));
                        Draw.rect(item.uiIcon, offsetX + Geometry.d4(i+2).x * width * ratio, offsetY + Geometry.d4(i+2).y * height * ratio, iconSize, iconSize);
                    }
                }
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            buffer.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buffer.read(read);
        }
    }
}
