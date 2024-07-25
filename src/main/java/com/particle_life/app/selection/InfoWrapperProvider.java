package com.particle_life.app.selection;

import java.util.List;

public interface InfoWrapperProvider<T> {
    List<InfoWrapper<T>> create() throws Exception;
}
