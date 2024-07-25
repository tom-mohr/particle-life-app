package com.particle_life.app.selection;

import com.particle_life.app.utils.MathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SelectionManager<T> {

    private final List<InfoWrapper<T>> items = new ArrayList<>();
    private int activeIndex = 0;

    public SelectionManager(InfoWrapperProvider<T> provider) throws Exception {
        List<InfoWrapper<T>> newItems = provider.create();
        if (newItems.isEmpty()) {
            throw new Exception("SelectionManager %s was initialized with zero items."
                    .formatted(this.getClass().getName()));
        }
        addAll(newItems);
    }

    public int size() {
        return items.size();
    }

    public InfoWrapper<T> get(int i) {
        return items.get(i);
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public InfoWrapper<T> getActiveInfoWrapper() {
        return items.get(activeIndex);
    }

    public T getActive() {
        return getActiveInfoWrapper().object;
    }

    public String getActiveName() {
        return getActiveInfoWrapper().name;
    }

    public String getActiveDescription() {
        return getActiveInfoWrapper().description;
    }

    public void setActive(int i) {

        if (i < 0 || i >= items.size()) {
            throw new IllegalArgumentException(String.format("selection index %d out of bounds (size is %d)", i, items.size()));
        }

        if (i != activeIndex) {
            activeIndex = i;
            onActiveChanged();
        }
    }

    public void setActive(String name) {
        int i = getIndexByName(name);
        if (i == -1) {
            throw new IllegalArgumentException("No item with name '%s' found in selection.".formatted(name));
        }
        setActive(i);
    }

    /**
     * Change the active element by going the given number of steps forward.
     * This is done in a circular manner, meaning that stepping over
     * the end of the selection results in coming out at the beginning,
     * and vice-versa.
     *
     * @param step how many steps to move from the current active element
     *             (positive: forward, negative: backward)
     */
    public void step(int step) {
        setActive(MathUtils.modulo(getActiveIndex() + step, items.size()));
    }

    public void stepForward() {
        step(1);
    }

    public void stepBackward() {
        step(-1);
    }

    /**
     * You can override this method.
     */
    protected void onActiveChanged() {
    }

    public void add(InfoWrapper<T> item) {
        boolean wasEmpty = size() == 0;
        items.add(item);

        if (wasEmpty) {
            onActiveChanged();
        }
    }

    public void addAll(Collection<? extends InfoWrapper<T>> items) {
        boolean wasEmpty = size() == 0;
        this.items.addAll(items);

        if (wasEmpty) {
            onActiveChanged();
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
     *
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
