package mindustry.ui.fragments;

import arc.Core;
import arc.scene.Group;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import java.text.NumberFormat;

public class MemoryFragment extends Fragment{

    public Table content = new Table().marginRight(40f).marginLeft(40f);
    private boolean visible;
    private double[] memory = null;

    @Override
    public void build(Group parent) {
        content.name = "memory";

        parent.fill(cont -> {
            cont.name = "memorylist";
            cont.visible(() -> visible);
            cont.center();
            cont.table(Tex.buttonTrans, pane -> {
                pane.add(Core.bundle.format("memory.memoryspace")).top().center();
                pane.button(Icon.cancel, this::toggle).size(40).top().right().row();
                pane.pane(content).grow().center().get().setScrollingDisabled(true, false);
            }).width(800f);
        });
    }

    public void rebuild() {
        content.clear();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        for (int i = 0; i < memory.length; i++) {
            content.add(String.valueOf(i)).color(Pal.accent).left();
            final int index = i;
            content.label(() -> nf.format(memory[index])).wrap().width(110).pad(5);
            if (i % 4 == 3) content.row();
        }
    }

    public void setMemory(double[] m) {
        memory = m;
        rebuild();
    }

    public void toggle() {
        visible = !visible;
    }
}
