package mindustry.world.blocks.distribution;

import arc.Core;
import arc.math.*;
import arc.math.geom.*;
import arc.graphics.g2d.*;
import arc.util.Time;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BufferedItemBridge extends ExtendingItemBridge{
    public final int timerAccept = timers++;

    public float speed = 40f;
    public int bufferCapacity = 50;

    public BufferedItemBridge(String name){
        super(name);
        hasPower = false;
        hasItems = true;
        canOverdrive = true;
    }

    public class BufferedItemBridgeBuild extends ExtendingItemBridgeBuild{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity);

        @Override
        public void updateTransport(Building other){
            if(buffer.accepts() && items.total() > 0){
                buffer.accept(items.take());
            }

            Item item = buffer.poll(speed / timeScale);
            if(timer(timerAccept, 4 / timeScale) && item != null && other.acceptItem(this, item)){
                cycleSpeed = Mathf.lerpDelta(cycleSpeed, 4f, 0.05f);
                other.handleItem(this, item);
                buffer.remove();
            }else{
                cycleSpeed = Mathf.lerpDelta(cycleSpeed, 0f, 0.008f);
            }
        }

        @Override
        public void draw() {
            super.draw();
            if (!Core.settings.getBool("showiteminjb")) return;
            Tile other = world.tile(link);
            if(!linkValid(tile, other)) return;

            int direction = tile.absoluteRelativeTo(other.x, other.y);
            float distance = Math.max(Math.abs(other.build.x - x), Math.abs(other.build.y - y)) - region.width / 3f / tilesize;
            float timeSpent = speed / timeScale, iconSize = 4;
            for (int i = 0; i < buffer.count(); i++) {
                float time = buffer.getTime(i);
                float ratio = Math.min(1 - i / (float)bufferCapacity, Time.time < time ? 1 : Math.min((Time.time - time) / timeSpent, 1));
                Draw.rect(buffer.getItem(i).uiIcon, x + Geometry.d4(direction).x * distance * ratio, y + Geometry.d4(direction).y * distance * ratio, iconSize, iconSize);
            }
        }

        @Override
        public void doDump(){
            dump();
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
