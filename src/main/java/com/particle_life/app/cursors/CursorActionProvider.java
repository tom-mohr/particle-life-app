package com.particle_life.app.cursors;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;

import java.util.ArrayList;
import java.util.List;

public class CursorActionProvider implements InfoWrapperProvider<CursorAction> {
    @Override
    public List<InfoWrapper<CursorAction>> create() {
        List<InfoWrapper<CursorAction>> list = new ArrayList<>();

        list.add(new InfoWrapper<>("Move", "Drag particles inside the cursor.", CursorAction.MOVE));
        list.add(new InfoWrapper<>("Brush", "Create new random particles inside the cursor.", CursorAction.BRUSH));
        list.add(new InfoWrapper<>("Delete", "Delete everything inside the cursor.", CursorAction.DELETE));

        return list;
    }
}
