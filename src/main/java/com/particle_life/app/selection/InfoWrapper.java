package com.particle_life.app.selection;

public class InfoWrapper<T> {
    public String name;
    public String description;
    public T object;

    public InfoWrapper(String name, String description, T object) {
        this.name = name;
        this.description = description;
        this.object = object;
    }

    public InfoWrapper(String name, T object) {
        this(name, "", object);
    }
}
