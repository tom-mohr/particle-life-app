package com.particle_life.app.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SelectionManager<T> {

    private final List<InfoWrapper<T>> items = new ArrayList<>();
    private int activeIndex = 0;

    public int size() {
        return items.size();
    }

    public InfoWrapper<T> get(int i) {
        return items.get(i);
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActive(int i) {

        if (i < 0 || i >= items.size()) {
            throw new IllegalArgumentException(String.format("selection index %d out of bounds (size is %d)", i, items.size()));
        }

        if (i != activeIndex) {
            activeIndex = i;
            activeChanged();
        }
    }

    /**
     * You can override this method.
     */
    protected void activeChanged() {
    }

    public InfoWrapper<T> getActive() {
        return items.get(activeIndex);
    }

    public void add(InfoWrapper<T> item) {
        boolean wasEmpty = size() == 0;
        items.add(item);

        if (wasEmpty) {
            activeChanged();
        }
    }

    public void addAll(Collection<? extends InfoWrapper<T>> items) {
        boolean wasEmpty = size() == 0;
        this.items.addAll(items);

        if (wasEmpty) {
            activeChanged();
        }
    }

    public boolean contains(InfoWrapper<T> item) {
        return items.contains(item);
    }
}
