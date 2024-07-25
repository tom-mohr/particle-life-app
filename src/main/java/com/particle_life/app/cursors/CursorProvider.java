package com.particle_life.app.cursors;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;

import java.util.ArrayList;
import java.util.List;

public class CursorProvider implements InfoWrapperProvider<CursorShape> {
    @Override
    public List<InfoWrapper<CursorShape>> create() throws Exception {
        List<InfoWrapper<CursorShape>> list = new ArrayList<>();

        list.add(new InfoWrapper<>("Circle", "Cursor with round shape.", new CircleCursorShape()));
        list.add(new InfoWrapper<>("Square", "Cursor with square shape.", new SquareCursorShape()));
        list.add(new InfoWrapper<>("Infinity", "Cursor that always selects all particles.", new InfinityCursorShape()));

        return list;
    }
}
