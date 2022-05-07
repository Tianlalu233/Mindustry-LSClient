package mindustry.ui;

import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.core.*;
import mindustry.type.*;
import mindustry.world.modules.ItemModule;

public class ItemImage extends Stack{

    public ItemImage(TextureRegion region, ItemStack stack, ItemModule im){

        add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(32f);
        }));

        add(new Table(t -> {
            t.left().bottom();
            int req = stack.amount;
            String reqStr = req > 1000 ? UI.formatAmount(req) : String.valueOf(req);
            t.add(reqStr).fontScale(0.75f);
            t.pack();
        }));

        add(new Table(t -> {
            t.left().top();
            t.label(() -> {
                int curr;
                if (im == null) curr = 0;
                else curr = im.get(stack.item);
                return curr > 1000 ? UI.formatAmount(curr) : String.valueOf(curr);
            }).fontScale(0.75f);
            t.pack();
        }));
    }

    public ItemImage(TextureRegion region, int amount){

        add(new Table(o -> {
            o.left();
            o.add(new Image(region)).size(32f);
        }));

        if(amount != 0){
            add(new Table(t -> {
                t.left().bottom();
                t.add(amount >= 1000 ? UI.formatAmount(amount) : amount + "").style(Styles.outlineLabel);
                t.pack();
            }));
        }
    }

    public ItemImage(ItemStack stack){
        this(stack.item.uiIcon, stack.amount);
    }

    public ItemImage(PayloadStack stack){
        this(stack.item.uiIcon, stack.amount);
    }
}
