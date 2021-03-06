package com.particle_life.app.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

    /**
     * Returns whether there exists an item whose name is equal to the given string.
     */
    public boolean hasName(String name) {
        return getIndexByName(name) != -1;
    }

    /**
     * Returns the index of the first element whose name is equal to the given string.
     * If no such element can be found, -1 is returned.
     * @param name the name of the item
     * @return the index of the first element with that name, or -1
     */
    public int getIndexByName(String name) {
        int i = 0;
        for (InfoWrapper<T> item : items) {
            if (name.equals(item.name)) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
