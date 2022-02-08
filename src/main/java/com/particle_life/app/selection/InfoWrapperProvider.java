package com.particle_life.app.selection;

import com.particle_life.app.selection.InfoWrapper;

import java.util.List;

public interface InfoWrapperProvider<T> {
    List<InfoWrapper<T>> create();
}
